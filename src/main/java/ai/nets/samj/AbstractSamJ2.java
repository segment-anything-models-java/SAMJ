/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj;

import java.lang.AutoCloseable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.nets.samj.AbstractSamJ.DebugTextPrinter;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import io.bioimage.modelrunner.apposed.appose.Environment;
import io.bioimage.modelrunner.apposed.appose.Service;
import io.bioimage.modelrunner.apposed.appose.Service.Task;
import io.bioimage.modelrunner.apposed.appose.Service.TaskStatus;
import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import io.bioimage.modelrunner.utils.CommonUtils;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Class that enables the use of EfficientSAM from Java.
 * @author Carlos Garcia
 * @author vladimir Ulman
 */
public abstract class AbstractSamJ2 implements AutoCloseable {
	
	protected static int LOWER_REENCODE_THRESH = 50;
	
	protected static int OPTIMAL_BBOX_IM_RATIO = 10;
	
	protected static double UPPER_REENCODE_THRESH = 1.1;
	
	public static long MAX_ENCODED_AREA_RS = 1024;
	
	public static long MAX_ENCODED_SIDE = MAX_ENCODED_AREA_RS * 3;
	
	protected static long ENCODE_MARGIN = 20;

	/** Essentially, a syntactic-shortcut for a String consumer */
	public interface DebugTextPrinter { void printText(String text); }
	/**
	 * Default String consumer that just prints the Strings that are inputed with {@link System#out}
	 */
	protected DebugTextPrinter debugPrinter = System.out::println;
	/**
	 * Whether the SAMJ model instance is verbose or not
	 */
	protected boolean isDebugging = true;
	/**
	 * Instance referencing the Python environment that is going to be used to run EfficientSAM
	 */
	private Environment env;
	/**
	 * Instance of {@link Service} that is in charge of opening a Python process and running the
	 * scripts provided in that Python process in order to be able to use EfficientSAM
	 */
	private Service python;
	/**
	 * The scripts that want to be run in Python
	 */
	private String script = "";
	/**
	 * Shared memory array used to share between Java and Python the image that wants to be processed by EfficientSAM 
	 */
	private SharedMemoryArray shma;
	/**
	 * Target dimensions of the image that is going to be encoded. If a single-channel 2D image is provided, that image is
	 * converted into a 3-channel image that EfficientSAM requires
	 */
	private long[] targetDims;
	/**
	 * Coordinates of the vertex of the crop/zoom of hte image of interest that has been encoded.
	 * It is the closest vertex to the origin.
	 * Usually the vertex is at 0,0 and the encoded image is all the pixels. This feature is useful for when the image 
	 * is big and reeconding needs to happen while the user pans and zooms in the image.
	 */
	private long[] encodeCoords;
	/**
	 * Scale factor of x and y applied to the image that is going to be annotated. 
	 * The image of interest does not need to be encoded normally. However, it is optimal to scale big images
	 * as the resolution of the segmentation depends on the ratio between the size of the image and the size of
	 * the object, thus 
	 */
	private double[] scale;
	/**
	 * Complete image being annotated. Usually this image is encoded completely 
	 * but for larger images, zooms of it might be encoded instead of the whole image
	 */
	private RandomAccessibleInterval<?> img;
	
	/**
	 * Change the image encoded by the EfficientSAM model
	 * @param <T>
	 * 	ImgLib2 data type of the image of interest
	 * @param rai
	 * 	image (n-dimensional array) that is going to be encoded as a {@link RandomAccessibleInterval}
	 * @throws IOException if any of the files to run a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public <T extends RealType<T> & NativeType<T>>
	void updateImage(RandomAccessibleInterval<T> rai) throws IOException, RuntimeException, InterruptedException {
		addImage(rai);
		this.img = rai;
	}
	
	/**
	 * Encode an image (n-dimensional array) with an EfficientSAM model
	 * @param <T>
	 * 	ImgLib2 data type of the image of interest
	 * @param rai
	 * 	image (n-dimensional array) that is going to be encoded as a {@link RandomAccessibleInterval}
	 * @throws IOException if any of the files to run a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	private <T extends RealType<T> & NativeType<T>>
	void addImage(RandomAccessibleInterval<T> rai) 
			throws IOException, RuntimeException, InterruptedException {
		if (rai.dimensionsAsLongArray()[0] * rai.dimensionsAsLongArray()[1] > MAX_ENCODED_AREA_RS * MAX_ENCODED_AREA_RS
				|| rai.dimensionsAsLongArray()[0] > MAX_ENCODED_SIDE || rai.dimensionsAsLongArray()[1] > MAX_ENCODED_SIDE) {
			this.targetDims = new long[] {0, 0, 0};
			this.img = rai;
			return;
		}
		this.script = "";
		sendImgLib2AsNp(rai);
		createEncodeImageScript();
		try {
			printScript(script, "Creation of initial embeddings");
			Task task = python.task(script);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
			this.shma.close();
		} catch (IOException | InterruptedException | RuntimeException e) {
			try {
				this.shma.close();
			} catch (IOException e1) {
				throw new IOException(e.toString() + System.lineSeparator() + e1.toString());
			}
			throw e;
		}
	}
	
	protected abstract void createEncodeImageScript();
	
	private void reencodeCrop() throws IOException, InterruptedException, RuntimeException {
		reencodeCrop(null);
	}
	
	private void reencodeCrop(long[] cropSize) throws IOException, InterruptedException, RuntimeException {
		this.script = "";
		sendCropAsNp(cropSize);
		createEncodeImageScript();
		try {
			printScript(script, "Creation of the cropped embeddings");
			Task task = python.task(script);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
			this.shma.close();
		} catch (IOException | InterruptedException | RuntimeException e) {
			try {
				this.shma.close();
			} catch (IOException e1) {
				throw new IOException(e.toString() + System.lineSeparator() + e1.toString());
			}
			throw e;
		}
		
	}
	
	private List<Polygon> processAndRetrieveContours(HashMap<String, Object> inputs) 
			throws IOException, RuntimeException, InterruptedException {
		Map<String, Object> results = null;
		try {
			Task task = python.task(script, inputs);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
			else if (task.status != TaskStatus.COMPLETE)
				throw new RuntimeException();
			else if (task.outputs.get("contours_x") == null)
				throw new RuntimeException();
			else if (task.outputs.get("contours_y") == null)
				throw new RuntimeException();
			results = task.outputs;
		} catch (IOException | InterruptedException | RuntimeException e) {
			try {
				this.shma.close();
			} catch (IOException e1) {
				throw new IOException(e.toString() + System.lineSeparator() + e1.toString());
			}
			throw e;
		}

		final List<List<Number>> contours_x_container = (List<List<Number>>)results.get("contours_x");
		final Iterator<List<Number>> contours_x = contours_x_container.iterator();
		final Iterator<List<Number>> contours_y = ((List<List<Number>>)results.get("contours_y")).iterator();
		final List<Polygon> polys = new ArrayList<>(contours_x_container.size());
		while (contours_x.hasNext()) {
			int[] xArr = contours_x.next().stream().mapToInt(Number::intValue).toArray();
			int[] yArr = contours_y.next().stream().mapToInt(Number::intValue).toArray();
			polys.add( new Polygon(xArr, yArr, xArr.length) );
		}
		return polys;
	}
	
	/**
	 * Method used that runs EfficientSAM using a mask as the prompt. The mask should be a 2D single-channel
	 * image {@link RandomAccessibleInterval} of the same x and y sizes as the image of interest, the image 
	 * where the model is finding the segmentations.
	 * Note that the quality of this prompting method is not good, it is still experimental as it barely works.
	 * It returns a list of polygons that corresponds to the contours of the masks found by EfficientSAM.
	 * 
	 * @param <T>
	 * 	ImgLib2 datatype of the mask
	 * @param img
	 * 	mask used as the prompt
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public <T extends RealType<T> & NativeType<T>>
	List<Polygon> processMask(RandomAccessibleInterval<T> img) 
				throws IOException, RuntimeException, InterruptedException {
		return processMask(img, true);
	}
	
	/**
	 * Method used that runs EfficientSAM using a mask as the prompt. The mask should be a 2D single-channel
	 * image {@link RandomAccessibleInterval} of the same x and y sizes as the image of interest, the image 
	 * where the model is finding the segmentations.
	 * Note that the quality of this prompting method is not good, it is still experimental as it barely works
	 * 
	 * @param <T>
	 * 	ImgLib2 datatype of the mask
	 * @param img
	 * 	mask used as the prompt
	 * @param returnAll
	 * 	whether to return all the polygons created by EfficientSAM of only the biggest
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public <T extends RealType<T> & NativeType<T>>
	List<Polygon> processMask(RandomAccessibleInterval<T> img, boolean returnAll) 
				throws IOException, RuntimeException, InterruptedException {
		long[] dims = img.dimensionsAsLongArray();
		if (dims.length == 2 && dims[1] == this.shma.getOriginalShape()[1] && dims[0] == this.shma.getOriginalShape()[0]) {
			img = Views.permute(img, 0, 1);
		} else if (dims.length != 2 && dims[0] != this.shma.getOriginalShape()[1] && dims[1] != this.shma.getOriginalShape()[0]) {
			throw new IllegalArgumentException("The provided mask should be a 2d image with just one channel of width "
					+ this.shma.getOriginalShape()[1] + " and height " + this.shma.getOriginalShape()[0]);
		}
		SharedMemoryArray maskShma = SharedMemoryArray.buildSHMA(img);
		try {
			return processMask(maskShma, returnAll);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			maskShma.close();
			throw ex;
		}
	}
	
	private List<Polygon> processMask(SharedMemoryArray shmArr, boolean returnAll) 
				throws IOException, RuntimeException, InterruptedException {
		this.script = "";
		processMasksWithSam(shmArr, returnAll);
		printScript(script, "Pre-computed mask inference");
		List<Polygon> polys = processAndRetrieveContours(null);
		debugPrinter.printText("processMask() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	abstract protected void processMasksWithSam(SharedMemoryArray shmArr, boolean returnAll);
	
	/**
	 * Method used that runs EfficientSAM using a list of points as the prompt. This method runs
	 * the prompt encoder and the EfficientSAM decoder only, the image encoder was run when the model
	 * was initialized with the image, thus it is quite fast.
	 * It returns a list of polygons that corresponds to the contours of the masks found by EfficientSAM
	 * @param pointsList
	 * 	the list of points that serve as a prompt for EfficientSAM. Each point is an int array
	 * 	of length 2, first position is x-axis, second y-axis
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> processPoints(List<int[]> pointsList)
			throws IOException, RuntimeException, InterruptedException{
		return processPoints(pointsList, true);
	}
	
	/**
	 * Method used that runs EfficientSAM using a list of points as the prompt. This method runs
	 * the prompt encoder and the EfficientSAM decoder only, the image encoder was run when the model
	 * was initialized with the image, thus it is quite fast.
	 * It returns a list of polygons that corresponds to the contours of the masks found by EfficientSAM
	 * @param pointsList
	 * 	the list of points that serve as a prompt for EfficientSAM. Each point is an int array
	 * 	of length 2, first position is x-axis, second y-axis
	 * @param returnAll
	 * 	whether to return all the polygons created by EfficientSAM of only the biggest
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> processPoints(List<int[]> pointsList, boolean returnAll)
			throws IOException, RuntimeException, InterruptedException{
		Rectangle rect = new Rectangle();
		rect.x = -1;
		rect.y = -1;
		rect.height = -1;
		rect.width = -1;
		return processPoints(pointsList, rect, returnAll);
	}

	public List<Polygon> processPoints(List<int[]> pointsList, Rectangle encodingArea, boolean returnAll)
			throws IOException, RuntimeException, InterruptedException {
		Objects.requireNonNull(encodingArea, "Second argument cannot be null. Use the method "
				+ "'processPoints(List<int[]> pointsList, Rectangle zoomedArea, boolean returnAll)'"
				+ " instead");
		return processPoints(pointsList, new ArrayList<int[]>(), encodingArea, returnAll);
	}
	
	/**
	 * Method used that runs EfficientSAM using a list of points as the prompt. This method also accepts another
	 * list of points as the negative prompt, the points that represent the background class wrt the object of interest. This method runs
	 * the prompt encoder and the EfficientSAM decoder only, the image encoder was run when the model
	 * was initialized with the image, thus it is quite fast.
	 * It returns a list of polygons that corresponds to the contours of the masks found by EfficientSAM
	 * @param pointsList
	 * 	the list of points that serve as a prompt for EfficientSAM. Each point is an int array
	 * 	of length 2, first position is x-axis, second y-axis
	 * @param pointsNegList
	 * 	the list of points that does not point to the instance of interest, but the background
	 * @param returnAll
	 * 	whether to return all the polygons created by EfficientSAM of only the biggest
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> processPoints(List<int[]> pointsList, List<int[]> pointsNegList, 
			Rectangle encodingArea, boolean returnAll)
			throws IOException, RuntimeException, InterruptedException {
		Objects.requireNonNull(encodingArea, "Third argument cannot be null. Use the method "
				+ "'processPoints(List<int[]> pointsList, List<int[]> pointsNegList, Rectangle zoomedArea, boolean returnAll)'"
				+ " instead");

		if (encodingArea.x == -1) {
			encodingArea = getCurrentlyEncodedArea();
		} else {
			ArrayList<int[]> outsideP = getPointsNotInRect(pointsList, pointsNegList, encodingArea);
			if (outsideP.size() != 0)
				throw new IllegalArgumentException("The Rectangle containing the area to be encoded should "
					+ "contain all the points. Point {x=" + outsideP.get(0)[0] + ", y=" + outsideP.get(0)[1] + "} is out of the region.");
		}
		evaluateReencodingNeeded(pointsList, pointsNegList, encodingArea);
		this.script = "";
		processPointsWithSAM(pointsList.size(), pointsNegList.size(), returnAll);
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_points", pointsList);
		inputs.put("input_neg_points", pointsNegList);
		printScript(script, "Points and negative points inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		recalculatePolys(polys, encodeCoords);
		debugPrinter.printText("processPoints() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	private ArrayList<int[]> getPointsNotInRect(List<int[]> pointsList, List<int[]> pointsNegList, Rectangle encodingArea) {
		ArrayList<int[]> points = new ArrayList<int[]>();
		ArrayList<int[]> not = new ArrayList<int[]>();
		points.addAll(pointsNegList);
		points.addAll(pointsList);
		for (int[] pp : points) {
			if (!encodingArea.contains(pp[0], pp[1]))
				not.add(pp);
		}
		return not;
	}
	
	public Rectangle getCurrentlyEncodedArea() {
		int xMargin = (int) (targetDims[1] * 0.1);
		int yMargin = (int) (targetDims[0] * 0.1);
		Rectangle alreadyEncoded;
		if (encodeCoords[0] != 0 || encodeCoords[1] != 0 || targetDims[1] != this.img.dimensionsAsLongArray()[1]
				 || targetDims[0] != this.img.dimensionsAsLongArray()[0]) {
			alreadyEncoded = new Rectangle((int) encodeCoords[0] + xMargin / 2, (int) encodeCoords[1] + yMargin / 2, 
					(int) targetDims[1] - xMargin, (int) targetDims[0] - yMargin);
		} else {
			alreadyEncoded = new Rectangle((int) encodeCoords[0], (int) encodeCoords[1], 
					(int) targetDims[1], (int) targetDims[0]);
		}
		return alreadyEncoded;
	}
	
	/**
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * @param pointsList
	 * @param pointsNegList
	 * @param rect
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws RuntimeException
	 */
	private void evaluateReencodingNeeded(List<int[]> pointsList, List<int[]> pointsNegList, Rectangle rect) 
			throws IOException, InterruptedException, RuntimeException {
		Rectangle alreadyEncoded = getCurrentlyEncodedArea();
		Rectangle neededArea = getApproximateAreaNeeded(pointsList, pointsNegList, rect);
		ArrayList<int[]> notInRect = getPointsNotInRect(pointsList, pointsNegList, rect);
		if (alreadyEncoded.x <= rect.x && alreadyEncoded.y <= rect.y 
				&& alreadyEncoded.width + alreadyEncoded.x >= rect.width + rect.x 
				&& alreadyEncoded.height + alreadyEncoded.y >= rect.width + rect.y
				&& alreadyEncoded.width * 0.9 < rect.width && alreadyEncoded.height * 0.9 < rect.height
				&& notInRect.size() == 0) {
			return;
		} else if (notInRect.size() != 0) {
			this.encodeCoords = new long[] {rect.x, rect.y};
			this.reencodeCrop(new long[] {rect.width, rect.height});
		} else if (alreadyEncoded.x <= rect.x && alreadyEncoded.y <= rect.y 
				&& alreadyEncoded.width + alreadyEncoded.x >= rect.width + rect.x 
				&& alreadyEncoded.height + alreadyEncoded.y >= rect.width + rect.y
				&& (alreadyEncoded.width * 0.9 > rect.width || alreadyEncoded.height * 0.9 > rect.height)) {
			this.encodeCoords = new long[] {rect.x, rect.y};
			this.reencodeCrop(new long[] {rect.width, rect.height});
		} else if (alreadyEncoded.x <= neededArea.x && alreadyEncoded.y <= neededArea.y 
				&& alreadyEncoded.width + alreadyEncoded.x >= neededArea.width + neededArea.x 
				&& alreadyEncoded.height + alreadyEncoded.y >= neededArea.width + neededArea.y
				&& alreadyEncoded.width * 0.9 < neededArea.width && alreadyEncoded.height * 0.9 < neededArea.height
				&& notInRect.size() == 0) {
			return;
		} else {
			this.encodeCoords = new long[] {rect.x, rect.y};
			this.reencodeCrop(new long[] {rect.width, rect.height});
		}
	}
	
	private Rectangle getApproximateAreaNeeded(List<int[]> pointsList, List<int[]> pointsNegList) {
		ArrayList<int[]> points = new ArrayList<int[]>();
		points.addAll(pointsNegList);
		points.addAll(pointsList);
		int minY = Integer.MAX_VALUE;
		int minX = Integer.MAX_VALUE;
		int maxY = 0;
		int maxX = 0;
		for (int[] pp : points) {
			if (pp[0] < minX)
				minX = pp[0];
			if (pp[0] > maxX)
				maxX = pp[0];
			if (pp[1] < minY)
				minY = pp[1];
			if (pp[1] > maxY)
				maxY = pp[1];
		}
		minX = (int) Math.max(0,  minX - Math.max((maxX - minX) * 0.1, ENCODE_MARGIN));
		minY = (int) Math.max(0,  minY - Math.max((maxY - minY) * 0.1, ENCODE_MARGIN));
		Rectangle rect = new Rectangle();
		rect.x = minX;
		rect.y = minY;
		rect.width = maxX - minY;
		rect.height = maxY - minY;
		return rect;
	}
	
	private Rectangle getApproximateAreaNeeded(List<int[]> pointsList, List<int[]> pointsNegList, Rectangle focusedArea) {
		ArrayList<int[]> points = new ArrayList<int[]>();
		points.addAll(pointsNegList);
		points.addAll(pointsList);
		int minY = Integer.MAX_VALUE;
		int minX = Integer.MAX_VALUE;
		int maxY = 0;
		int maxX = 0;
		for (int[] pp : points) {
			if (pp[0] < minX)
				minX = pp[0];
			if (pp[0] > maxX)
				maxX = pp[0];
			if (pp[1] < minY)
				minY = pp[1];
			if (pp[1] > maxY)
				maxY = pp[1];
		}
		minX = (int) Math.max(0,  minX - Math.max(focusedArea.width * 0.1, ENCODE_MARGIN));
		minY = (int) Math.max(0,  minY - Math.max(focusedArea.height * 0.1, ENCODE_MARGIN));
		maxX = (int) Math.min(img.dimensionsAsLongArray()[1],  maxX + Math.max(focusedArea.width * 0.1, ENCODE_MARGIN));
		maxY = (int) Math.min(img.dimensionsAsLongArray()[0],  maxY + Math.max(focusedArea.height * 0.1, ENCODE_MARGIN));
		Rectangle rect = new Rectangle();
		rect.x = minX;
		rect.y = minY;
		rect.width = maxX - minY;
		rect.height = maxY - minY;
		return rect;
	}
	
	/**
	 * Method used that runs EfficientSAM using a list of points as the prompt. This method also accepts another
	 * list of points as the negative prompt, the points that represent the background class wrt the object of interest. This method runs
	 * the prompt encoder and the EfficientSAM decoder only, the image encoder was run when the model
	 * was initialized with the image, thus it is quite fast.
	 * It returns a list of polygons that corresponds to the contours of the masks found by EfficientSAM
	 * @param pointsList
	 * 	the list of points that serve as a prompt for EfficientSAM. Each point is an int array
	 * 	of length 2, first position is x-axis, second y-axis
	 * @param pointsNegList
	 * 	the list of points that does not point to the instance of interest, but the background
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> processPoints(List<int[]> pointsList, List<int[]> pointsNegList)
			throws IOException, RuntimeException, InterruptedException {
		Rectangle rect = new Rectangle();
		rect.x = (int) this.encodeCoords[0];
		rect.y = (int) this.encodeCoords[1];
		rect.height = (int) this.targetDims[0];
		rect.width = (int) this.targetDims[1];
		return processPoints(pointsList, pointsNegList, rect, true);
	}
	
	public List<Polygon> processPoints(List<int[]> pointsList, List<int[]> pointsNegList, 
			Rectangle zoomedArea)
			throws IOException, RuntimeException, InterruptedException {
		return processPoints(pointsList, pointsNegList, zoomedArea, true);
	}
	
	/**
	 * Method used that runs EfficientSAM using a list of points as the prompt. This method also accepts another
	 * list of points as the negative prompt, the points that represent the background class wrt the object of interest. This method runs
	 * the prompt encoder and the EfficientSAM decoder only, the image encoder was run when the model
	 * was initialized with the image, thus it is quite fast.
	 * It returns a list of polygons that corresponds to the contours of the masks found by EfficientSAM
	 * @param pointsList
	 * 	the list of points that serve as a prompt for EfficientSAM. Each point is an int array
	 * 	of length 2, first position is x-axis, second y-axis
	 * @param pointsNegList
	 * 	the list of points that does not point to the instance of interest, but the background
	 * @param returnAll
	 * 	whether to return all the polygons created by EfficientSAM of only the biggest
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> processPoints(List<int[]> pointsList, List<int[]> pointsNegList, boolean returnAll)
			throws IOException, RuntimeException, InterruptedException {
		Rectangle rect = new Rectangle();
		rect.x = (int) this.encodeCoords[0];
		rect.y = (int) this.encodeCoords[1];
		rect.height = (int) this.targetDims[0];
		rect.width = (int) this.targetDims[1];
		return processPoints(pointsList, pointsNegList, rect, returnAll);
	}
	
	/**
	 * Method used that runs EfficientSAM using a bounding box as the prompt. The bounding box should
	 * be a int array of length 4 of the form [x0, y0, x1, y1].
	 * This method runs the prompt encoder and the EfficientSAM decoder only, the image encoder was run when the model
	 * was initialized with the image, thus it is quite fast.
	 * 
	 * Returns a list of all the polygons found by EfficientSAM
	 * 
	 * @param boundingBox
	 * 	the bounding box that serves as the prompt for EfficientSAM
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> processBox(int[] boundingBox) 
			throws IOException, RuntimeException, InterruptedException {
		return processBox(boundingBox, true);
	}
	
	/**
	 * Method used that runs EfficientSAM using a bounding box as the prompt. The bounding box should
	 * be a int array of length 4 of the form [x0, y0, x1, y1].
	 * This method runs the prompt encoder and the EfficientSAM decoder only, the image encoder was run when the model
	 * was initialized with the image, thus it is quite fast.
	 * 
	 * @param boundingBox
	 * 	the bounding box that serves as the prompt for EfficientSAM
	 * @param returnAll
	 * 	whether to return all the polygons created by EfficientSAM of only the biggest
	 * @return a list of polygons where each polygon is the contour of a mask that has been found by EfficientSAM
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> processBox(int[] boundingBox, boolean returnAll)
			throws IOException, RuntimeException, InterruptedException {
		if (needsMoreResolution(boundingBox)) {
			this.encodeCoords = calculateEncodingNewCoords(boundingBox, this.img.dimensionsAsLongArray());
			reencodeCrop();
		} else if (!isAreaEncoded(boundingBox)) {
			this.encodeCoords = calculateEncodingNewCoords(boundingBox, this.img.dimensionsAsLongArray());
			reencodeCrop();
		}
		int[] adaptedBoundingBox = new int[] {(int) (boundingBox[0] - encodeCoords[0]), (int) (boundingBox[1] - encodeCoords[1]),
				(int) (boundingBox[2] - encodeCoords[0]), (int) (boundingBox[3] - encodeCoords[1])};;
		this.script = "";
		processBoxWithSAM(returnAll);
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_box", adaptedBoundingBox);
		printScript(script, "Rectangle inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		recalculatePolys(polys, encodeCoords);
		debugPrinter.printText("processBox() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	/**
	 * Check whether the bounding box is inside the area that is encoded or not
	 * @param boundingBox
	 * 	the vertices of the bounding box
	 * @return whether the bounding box is within the encoded area or not
	 */
	public boolean isAreaEncoded(int[] boundingBox) {
		boolean upperLeftVertex = (boundingBox[0] > this.encodeCoords[0]) && (boundingBox[0] < this.encodeCoords[2]);
		boolean upperRightVertex = (boundingBox[2] > this.encodeCoords[0]) && (boundingBox[2] < this.encodeCoords[2]);
		boolean downLeftVertex = (boundingBox[1] > this.encodeCoords[1]) && (boundingBox[1] < this.encodeCoords[3]);
		boolean downRightVertex = (boundingBox[3] > this.encodeCoords[1]) && (boundingBox[3] < this.encodeCoords[3]);
		
		if (upperLeftVertex && upperRightVertex && downLeftVertex && downRightVertex)
			return true;
		return false;
	}

	/**
	 * For bounding box masks, check whether the its size is too small compared to the size
	 * of the encoded image.
	 * 
	 * Approximately, if the original image encoded is about 20 times bigger than the bounding box size,
	 * the resolution of the SAM-based model encodings will not be enough to identify the object of interest,
	 * thus re-encoding of a zoomed part of the image will be necessary.
	 * 
	 * @param boundingBox
	 * 	bounding box of interest
	 * @return whether the bounding box of interest is big enough to produce good results or not
	 */
	public boolean needsMoreResolution(int[] boundingBox) {
		long xSize = boundingBox[2] - boundingBox[0];
		long ySize = boundingBox[3] - boundingBox[1];
		long encodedX = targetDims[1];
		long encodedY = targetDims[0];
		if (xSize * LOWER_REENCODE_THRESH < encodedX && ySize * LOWER_REENCODE_THRESH < encodedY)
			return true;
		return false;
	}
	
	/**
	 * TODO what to do, is there a bounding box that is too big with respect to the encoded crop?
	 * @param boundingBox
	 * @return
	 */
	public boolean boundingBoxTooBig(int[] boundingBox) {
		long xSize = boundingBox[2] - boundingBox[0];
		long ySize = boundingBox[3] - boundingBox[1];
		long encodedX = targetDims[1];
		long encodedY = targetDims[0];
		if (xSize * UPPER_REENCODE_THRESH > encodedX && ySize * UPPER_REENCODE_THRESH > encodedY)
			return true;
		return false;
	}

	@Override
	/**
	 * {@inheritDoc}
	 * Close the Python process and clean the memory
	 */
	public void close() {
		if (python != null) python.close();
	}
	
	private <T extends RealType<T> & NativeType<T>> 
	void sendImgLib2AsNp(RandomAccessibleInterval<T> targetImg) {
		shma = createEfficientSAMInputSHM(reescaleIfNeeded(targetImg));
		adaptImageToModel(targetImg, shma.getSharedRAI());
		String code = "";
		// This line wants to recreate the original numpy array. Should look like:
		// input0_appose_shm = shared_memory.SharedMemory(name=input0)
		// input0 = np.ndarray(size, dtype="float64", buffer=input0_appose_shm.buf).reshape([64, 64])
		code += "im_shm = shared_memory.SharedMemory(name='"
							+ shma.getNameForPython() + "', size=" + shma.getSize() 
							+ ")" + System.lineSeparator();
		int size = 1;
		for (long l : targetDims) {size *= l;}
		code += "im = np.ndarray(" + size + ", dtype='float32', buffer=im_shm.buf).reshape([";
		for (long ll : targetDims)
			code += ll + ", ";
		code = code.substring(0, code.length() - 2);
		code += "])" + System.lineSeparator();
		code += "input_h = im.shape[0]" + System.lineSeparator();
		code += "input_w = im.shape[1]" + System.lineSeparator();
		code += "globals()['input_h'] = input_h" + System.lineSeparator();
		code += "globals()['input_w'] = input_w" + System.lineSeparator();
		code += "im = torch.from_numpy(np.transpose(im.astype('float32'), (2, 0, 1)))" + System.lineSeparator();
		code += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	private <T extends RealType<T> & NativeType<T>> 
	void sendCropAsNp() {
		sendCropAsNp(null);
	}
		
	private <T extends RealType<T> & NativeType<T>> void sendCropAsNp(long[] cropSize) {
		if (cropSize == null)
			cropSize = new long[] {encodeCoords[3] - encodeCoords[1], encodeCoords[2] - encodeCoords[0], 3};
		//RandomAccessibleInterval<T> crop = 
			//	Views.interval( Cast.unchecked(img), new long[] {encodeCoords[1], encodeCoords[0], 0}, interValSize );
		
		//RandomAccessibleInterval<T> crop = Views.offsetInterval(crop, new long[] {encodeCoords[1], encodeCoords[0], 0}, interValSize);
		RandomAccessibleInterval<T> crop = Views.offsetInterval(Cast.unchecked(img), new long[] {encodeCoords[1], encodeCoords[0], 0}, cropSize);
		targetDims = crop.dimensionsAsLongArray();
		shma = SharedMemoryArray.buildMemorySegmentForImage(new long[] {targetDims[0], targetDims[1], targetDims[2]}, 
															Util.getTypeFromInterval(crop));
		RealTypeConverters.copyFromTo(crop, shma.getSharedRAI());
		String code = "";
		// This line wants to recreate the original numpy array. Should look like:
		// input0_appose_shm = shared_memory.SharedMemory(name=input0)
		// input0 = np.ndarray(size, dtype="float64", buffer=input0_appose_shm.buf).reshape([64, 64])
		code += "im_shm = shared_memory.SharedMemory(name='"
							+ shma.getNameForPython() + "', size=" + shma.getSize() 
							+ ")" + System.lineSeparator();
		int size = 1;
		for (long l : targetDims) {size *= l;}
		code += "im = np.ndarray(" + size + ", dtype='" + CommonUtils.getDataType(Util.getTypeFromInterval(crop)) + "', buffer=im_shm.buf).reshape([";
		for (long ll : targetDims)
			code += ll + ", ";
		code = code.substring(0, code.length() - 2);
		code += "])" + System.lineSeparator();
		code += "input_h = im.shape[0]" + System.lineSeparator();
		code += "input_w = im.shape[1]" + System.lineSeparator();
		//code += "np.save('/home/carlos/git/cropped.npy', im)" + System.lineSeparator();
		code += "globals()['input_h'] = input_h" + System.lineSeparator();
		code += "globals()['input_w'] = input_w" + System.lineSeparator();
		code += "im = torch.from_numpy(np.transpose(im.astype('float32'), (2, 0, 1)))" + System.lineSeparator();
		code += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	protected abstract void processPointsWithSAM(int nPoints, int nNegPoints, boolean returnAll);
	
	protected abstract void processBoxWithSAM(boolean returnAll);
	
	private static <T extends RealType<T> & NativeType<T>>
	SharedMemoryArray  createEfficientSAMInputSHM(final RandomAccessibleInterval<T> inImg) {
		long[] dims = inImg.dimensionsAsLongArray();
		if ((dims.length != 3 && dims.length != 2) || (dims.length == 3 && dims[2] != 3 && dims[2] != 1)){
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'yxc', first dimension height, second width and third channels.");
		}
		return SharedMemoryArray.buildMemorySegmentForImage(new long[] {dims[0], dims[1], 3}, new FloatType());
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void adaptImageToModel(final RandomAccessibleInterval<T> ogImg, RandomAccessibleInterval<FloatType> targetImg) {
		if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int i = 0; i < 3; i ++) 
				RealTypeConverters.copyFromTo( normalizedView(Views.hyperSlice(ogImg, 2, i)), Views.hyperSlice(targetImg, 2, i) );
		} else if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 1) {
			debugPrinter.printText("CONVERTED 1 CHANNEL IMAGE INTO 3 TO BE FEEDED TO SAMJ");
			IntervalView<FloatType> resIm = Views.interval( Views.expandMirrorDouble(normalizedView(ogImg), new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, ogImg.dimensionsAsLongArray()[0], ogImg.dimensionsAsLongArray()[1], 2}) );
			RealTypeConverters.copyFromTo( resIm, targetImg );
		} else if (ogImg.numDimensions() == 2) {
			adaptImageToModel(Views.addDimension(ogImg, 0, 0), targetImg);
		} else {
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'yxc', first dimension height, second width and third channels.");
		}
		this.img = targetImg;
		this.targetDims = targetImg.dimensionsAsLongArray();
	}

	/**
	 * Set an empty consumer as {@link DebugTextPrinter} to avoid the SAMJ model instance
	 * to communicate its process
	 */
	public void disableDebugPrinting() {
		debugPrinter = (text) -> {};
	}
	
	/**
	 * Set the {@link DebugTextPrinter} that wants to be used in the model
	 * @param lineComsumer
	 * 	the {@link DebugTextPrinter} (which is basically a String consumer used to communicate the
	 *  SAMJ model instance process) that wants to be used
	 */
	public void setDebugPrinter(final DebugTextPrinter lineComsumer) {
		if (lineComsumer != null) debugPrinter = lineComsumer;
	}
	
	/**
	 * Set whether the SAMJ model instance has to be more verbose or not
	 * @param newState
	 * 	whether the new model is verbose or not
	 */
	public void setDebugging(boolean newState) {
		isDebugging = newState;
	}
	
	/**
	 * 
	 * @return true if the SAMJ model instance is verbose or not
	 */
	public boolean isDebugging() {
		return isDebugging;
	}

	/**
	 * Method that prints the String in the script parameter to the {@link DebugTextPrinter}
	 * 
	 * @param script
	 * 	text that wants to be printed, usually a Python script
	 * @param designationOfTheScript
	 * 	the name (or some string to design) of the text that is going to be printed
	 */
	public void printScript(final String script, final String designationOfTheScript) {
		if (!isDebugging) return;
		debugPrinter.printText("START: =========== "+designationOfTheScript+" ===========");
		debugPrinter.printText(LocalDateTime.now().toString());
		debugPrinter.printText(script);
		debugPrinter.printText("END:   =========== "+designationOfTheScript+" ===========");
	}

	/**
	 * Get the maximum and minimum pixel values of an {@link IterableInterval}
	 * @param <T>
	 * 	the ImgLib2 data types that the {@link IterableInterval} can have
	 * @param inImg
	 * 	the {@link IterableInterval} from which the max and min values are going to be found
	 * @param outMinMax
	 * 	double array where the max and min values of the {@link IterableInterval} will be written
	 */
	public static <T extends RealType<T> & NativeType<T>>
	void getMinMaxPixelValue(final IterableInterval<T> inImg, final double[] outMinMax) {
		double min = inImg.firstElement().getRealDouble();
		double max = min;

		for (T px : inImg) {
			double val = px.getRealDouble();
			min = Math.min(min,val);
			max = Math.max(max,val);
		}

		if (outMinMax.length > 1) {
			outMinMax[0] = min;
			outMinMax[1] = max;
		}
	}

	/**
	 * Whether the values in the length 2 array are between 0 and 1
	 * @param inMinMax
	 * 	the interval to be evaluated
	 * @return true if the values are between 0 and 1 and false otherwise
	 */
	public static boolean isNormalizedInterval(final double[] inMinMax) {
		return (inMinMax[0] >= 0 && inMinMax[0] <= 1
			&& inMinMax[1] >= 0 && inMinMax[1] <= 1);
	}

	/**
	 * Normalize the {@link RandomAccessibleInterval} with the position 0 of the inMimMax array as the min
	 * and the position 1 as the max
	 * @param <T>
	 * 	the ImgLib2 data types that the {@link RandomAccessibleInterval} can have
	 * @param inImg
	 *  {@link RandomAccessibleInterval} to be normalized
	 * @param inMinMax
	 * 	 the values to which the {@link RandomAccessibleInterval} will be normalized. Should be a double array of length
	 *   2 with the smaller value at position 0
	 * @return the normalized {@link RandomAccessibleInterval}
	 */
	private static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<FloatType> normalizedView(final RandomAccessibleInterval<T> inImg, final double[] inMinMax) {
		final double min = inMinMax[0];
		final double range = inMinMax[1] - min;
		return Converters.convert(inImg, (i, o) -> o.setReal((i.getRealFloat() - min) / (range + 1e-9)), new FloatType());
	}

	/**
	 * Checks the input RAI if its min and max pixel values are between [0,1].
	 * If they are not, the RAI will be subject to {@link Converters#convert(RandomAccessibleInterval, Converter, Type)}
	 * with here-created Converter that knows how to bring the pixel values into the interval [0,1].
	 *
	 * @param <T>
	 * 	the ImgLib2 data types that the {@link RandomAccessibleInterval} can have
	 * @param inImg
	 *  RAI to be potentially normalized.
	 * @return The input image itself or a View of it with {@link FloatType} data type
	 */
	public <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<FloatType> normalizedView(final RandomAccessibleInterval<T> inImg) {
		final double[] minMax = new double[2];
		getMinMaxPixelValue(Views.iterable(inImg), minMax);
		///debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ NORMALIZED, returning Converted view");
		//return normalizedView(inImg, minMax);
		if (isNormalizedInterval(minMax) && Util.getTypeFromInterval(inImg) instanceof FloatType) {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS NORMALIZED, returning directly itself");
			return Cast.unchecked(inImg);
		} else if (isNormalizedInterval(minMax)) {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS NORMALIZED, returning directly itself");
			return  Converters.convert(inImg, (i, o) -> o.setReal(i.getRealFloat()), new FloatType());
		} else {
			debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ NORMALIZED, returning Converted view");
			return normalizedView(inImg, minMax);
		}
	}

	private static <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<UnsignedByteType> convertViewToRGB(final RandomAccessibleInterval<T> inImg, final double[] inMinMax) {
		final double min = inMinMax[0];
		final double range = inMinMax[1] - min;
		return Converters.convert(inImg, (i, o) -> o.setReal(255 * (i.getRealDouble() - min) / range), new UnsignedByteType());
	}

	/**
	 * Checks the input RAI if its min and max pixel values are between [0,255] and if it is of {@link UnsignedByteType} type.
	 * If they are not, the RAI will be subject to {@link Converters#convert(RandomAccessibleInterval, Converter, Type)}
	 * with here-created Converter that knows how to bring the pixel values into the interval [0,255].
	 *
	 * @param inImg
	 *  RAI to be potentially converted to RGB.
	 * @return The input image itself or a View of it in {@link UnsignedByteType} data type
	 */
	public <T extends RealType<T> & NativeType<T>>
	RandomAccessibleInterval<UnsignedByteType> convertViewToRGB(final RandomAccessibleInterval<T> inImg) {
		if (Util.getTypeFromInterval(inImg) instanceof UnsignedByteType) {
			debugPrinter.printText("IMAGE IS RGB, returning directly itself");
			return Cast.unchecked(inImg);
		}
		final double[] minMax = new double[2];
		debugPrinter.printText("MIN VALUE="+minMax[0]+", MAX VALUE="+minMax[1]+", IMAGE IS _NOT_ RGB, returning Converted view");
		getMinMaxPixelValue(Views.iterable(inImg), minMax);
		return convertViewToRGB(inImg, minMax);
	}
	
	protected static <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> 
	reescaleIfNeeded(RandomAccessibleInterval<T> rai) {
		if ((rai.dimensionsAsLongArray()[0] > rai.dimensionsAsLongArray()[1])
				&& (rai.dimensionsAsLongArray()[0] > MAX_ENCODED_AREA_RS)) {
			// TODO reescale
			return rai;
		} else if ((rai.dimensionsAsLongArray()[0] < rai.dimensionsAsLongArray()[1])
				&& (rai.dimensionsAsLongArray()[1] > MAX_ENCODED_SIDE)) {
			// TODO reescale
			return rai;
		} else {
			return rai;
		}
	}
	
	/**
	 * Calculate the coordinates of the encoded image with respect to the coordinates
	 * of the bounding box that contain the area that we need to crop and encode for SAM to have
	 * the needed resolution to sement properly the object of interest.
	 * 
	 * This will only be used on big images.
	 * 
	 * @param boundingBox
	 * 	coordinates of the bounding box
	 * @param imageSize
	 * 	total size of the image that wants to be annotated
	 * @return the distance in each direction with respect to the coordinates of the bounding box that
	 * 	indicate the crop that needs to be encoded
	 */
	protected long[] calculateEncodingNewCoords(int[] boundingBox, long[] imageSize) {
		long xSize = boundingBox[2] - boundingBox[0]; 
		long ySize = boundingBox[3] - boundingBox[1];
		long smallerSize = ySize < xSize ? ySize * OPTIMAL_BBOX_IM_RATIO : xSize * OPTIMAL_BBOX_IM_RATIO;
		long biggerSize = smallerSize * 3;
		if ((ySize < xSize) && (ySize * 3 > xSize)) {
			biggerSize = xSize * OPTIMAL_BBOX_IM_RATIO;
		} else if ((ySize > xSize) && (xSize * 3 > ySize)) {
			biggerSize = ySize * OPTIMAL_BBOX_IM_RATIO;
		}
		long[] newSize = new long[] {biggerSize, smallerSize};
		if (ySize > xSize) newSize = new long[] {smallerSize, biggerSize};
		long[] posWrtBbox = new long[4];
		posWrtBbox[0] = (long) Math.max(0, Math.ceil((boundingBox[0] + xSize / 2) - newSize[0] / 2));
		posWrtBbox[1] = (long) Math.max(0, Math.ceil((boundingBox[1] + ySize / 2) - newSize[1] / 2));
		posWrtBbox[2] = (long) Math.min(imageSize[1], Math.floor((boundingBox[2] + xSize / 2) + newSize[0] / 2));
		posWrtBbox[3] = (long) Math.min(imageSize[0], Math.floor((boundingBox[3] + ySize / 2) + newSize[1] / 2));
		return posWrtBbox;
	}
	
	/**
	 * Method that recalculates the coordinates of the polygons outputed by SAMJ.
	 * 
	 * This method is usually for big images. In order to create encoding with enough resolution
	 * to detect small objects compared to the size of the whole image, SAMJ might encode crops of
	 * the total image, thus the coordinates of the polygons obtained need to be shifted in order
	 * to match the original image.
	 * @param polys
	 * 	polys obtained by SAMJ on the encoded crop
	 * @param encodeCoords
	 * 	position of the crop in the total image
	 */
	protected void recalculatePolys(List<Polygon> polys, long[] encodeCoords) {
		polys.stream().forEach(pp -> {
			pp.xpoints = Arrays.stream(pp.xpoints).map(x -> x + (int) encodeCoords[0]).toArray();
			pp.ypoints = Arrays.stream(pp.ypoints).map(y -> y + (int) encodeCoords[1]).toArray();
		});
	}
	
	/**
	 * MEthod used during development to test features
	 * @param args
	 * 	nothing
	 * @throws IOException nothing
	 * @throws RuntimeException nothing
	 * @throws InterruptedException nothing
	 */
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		img = Views.addDimension(img, 1, 2);
		try (AbstractSamJ2 sam = initializeSam(SamEnvManager.create(), img)) {
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}
}
