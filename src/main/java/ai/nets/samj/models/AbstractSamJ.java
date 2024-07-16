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
package ai.nets.samj.models;

import java.lang.AutoCloseable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.IOException;

import io.bioimage.modelrunner.apposed.appose.Environment;
import io.bioimage.modelrunner.apposed.appose.Service;
import io.bioimage.modelrunner.apposed.appose.Service.Task;
import io.bioimage.modelrunner.apposed.appose.Service.TaskStatus;
import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.view.Views;

/**
 * Class that enables the use of EfficientSAM from Java.
 * @author Carlos Garcia
 * @author vladimir Ulman
 */
public abstract class AbstractSamJ implements AutoCloseable {
	
	protected static int LOWER_REENCODE_THRESH = 50;
	
	protected static int OPTIMAL_BBOX_IM_RATIO = 10;
	
	protected static double UPPER_REENCODE_THRESH = 1.1;
	/**
	 * TODO rethink maximum size
	 */
	public static long MAX_ENCODED_AREA_RS = 512;
	
	public static long MIN_ENCODED_AREA_SIDE = 128;
	
	public static long MAX_ENCODED_SIDE = MAX_ENCODED_AREA_RS * 3;
	
	protected static long ENCODE_MARGIN = 64;

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
	protected Environment env;
	/**
	 * Instance of {@link Service} that is in charge of opening a Python process and running the
	 * scripts provided in that Python process in order to be able to use EfficientSAM
	 */
	protected Service python;
	/**
	 * The scripts that want to be run in Python
	 */
	protected String script = "";
	/**
	 * Shared memory array used to share between Java and Python the image that wants to be processed by EfficientSAM 
	 */
	protected SharedMemoryArray shma;
	/**
	 * Target dimensions of the image that is going to be encoded. If a single-channel 2D image is provided, that image is
	 * converted into a 3-channel image that EfficientSAM requires.
	 * The axes are "xyc"
	 */
	protected long[] targetDims;
	/**
	 * Coordinates of the vertex of the crop/zoom of hte image of interest that has been encoded.
	 * It is the closest vertex to the origin.
	 * Usually the vertex is at 0,0 and the encoded image is all the pixels. This feature is useful for when the image 
	 * is big and reeconding needs to happen while the user pans and zooms in the image.
	 * The axes are "xyc"
	 */
	protected long[] encodeCoords;
	/**
	 * Scale factor of x and y applied to the image that is going to be annotated. 
	 * The image of interest does not need to be encoded normally. However, it is optimal to scale big images
	 * as the resolution of the segmentation depends on the ratio between the size of the image and the size of
	 * the object, thus 
	 * The axes are "xyc"
	 */
	protected double[] scale;
	/**
	 * TODO this should be false by default, but at the moment IJ is the only consumer
	 * IMPORTANT (ONLY FOR IMAGEJ ROI MANAGER)
	 * If the resulting polygons are going to be displayed using 
	 * ImageJ's ROI manager, this needs to be set to true.
	 * 
	 * If this is set to true, the Polygon ROI produced by the model will be increased
	 * by one pixel on the right and down sides. This is because IJ's ROI manager takes
	 * the upper left corner of the pixel to anchor the Polygon ROI, effectively leaving the
	 * the pixels on the right and lower part out of the ROI.
	 */
	protected boolean isIJROIManager = true;
	/**
	 * Complete image being annotated. Usually this image is encoded completely 
	 * but for larger images, zooms of it might be encoded instead of the whole image.
	 * 
	 * This image is stored as "xyc"
	 */
	protected RandomAccessibleInterval<?> img;
	/**
	 * Whether the image is small or not. If the image is small, it is only encoded once, if
	 * it is not, the image is encoded on demand
	 */
	protected boolean imageSmall = true;
	
	/**
	 * List of encodings that are cached to avoid recalculating
	 */
	List<String> savedEncodings = new ArrayList<String>();

	protected abstract String persistEncodingScript(String encodingName);

	protected abstract String selectEncodingScript(String encodingName);

	protected abstract String deleteEncodingScript(String encodingName);
	
	protected abstract void cellSAM(List<int[]> grid, boolean returnAll);
	
	protected abstract void processPointsWithSAM(int nPoints, int nNegPoints, boolean returnAll);
	
	protected abstract void processBoxWithSAM(boolean returnAll);
	
	protected abstract <T extends RealType<T> & NativeType<T>> void setImageOfInterest(RandomAccessibleInterval<T> rai);
	
	protected abstract <T extends RealType<T> & NativeType<T>> void createEncodeImageScript();
	
	abstract protected void processMasksWithSam(SharedMemoryArray shmArr, boolean returnAll);
	
	protected abstract <T extends RealType<T> & NativeType<T>> void createSHMArray(RandomAccessibleInterval<T> imShared);

	@Override
	/**
	 * {@inheritDoc}
	 * Close the Python process and clean the memory
	 */
	public void close() {
		if (python != null) python.close();
	}
	
	/**
	 * 
	 * @return true if the SAMJ model instance is verbose or not
	 */
	public boolean isDebugging() {
		return isDebugging;
	}
	
	/**
	 * IMPORTANT (only for ImageJ)
	 * If the resulting polygons are going to be displayed using 
	 * ImageJ's ROI manager, this method method needs to be called with true.
	 * 
	 * If this is set to true, the Polygon ROI produced by the model will be increased
	 * by one pixel on the right and down sides. This is because IJ's ROI manager takes
	 * the upper left corner of the pixel to anchor the Polygon ROI, effectively leaving the
	 * the pixels on the right and lower part out of the ROI.
	 * @param isUsingIJRoiManager
	 * 	whether we are going to give the Polygon ROI to ImageJ ROI manager
	 */
	public void setUsinIJRoiManager(boolean isUsingIJRoiManager) {
		this.isIJROIManager = isUsingIJRoiManager;
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
	 * Encode an image (n-dimensional array) with an SAM model
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
		setImageOfInterest(rai);
		if (img.dimensionsAsLongArray()[0] * img.dimensionsAsLongArray()[1] > MAX_ENCODED_AREA_RS * MAX_ENCODED_AREA_RS
				|| img.dimensionsAsLongArray()[0] > MAX_ENCODED_SIDE || img.dimensionsAsLongArray()[1] > MAX_ENCODED_SIDE) {
			this.targetDims = new long[] {0, 0, 0};
			this.imageSmall = false;
			return;
		}
		this.script = "";
		sendImgLib2AsNp();
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
	
	protected <T extends RealType<T> & NativeType<T>> 
	void sendImgLib2AsNp() {
		createSHMArray((RandomAccessibleInterval<T>) this.img);
	}
		
	private <T extends RealType<T> & NativeType<T>> void sendCropAsNp(long[] cropSize) {
		if (cropSize == null)
			cropSize = new long[] {encodeCoords[2] - encodeCoords[0], encodeCoords[3] - encodeCoords[1], 3};
		else if (cropSize.length == 2)
			cropSize = new long[] {cropSize[0], cropSize[1], 3};
		else if (cropSize.length == 3 && cropSize[2] != 3)
			throw new IllegalArgumentException("The size of the area that wants to be encoded needs to be defined as [width, height].");
		else 
			throw new IllegalArgumentException("The size of the area that wants to be encoded needs to be defined as [width, height].");
		RandomAccessibleInterval<T> crop = 
				Views.offsetInterval( Cast.unchecked(img), new long[] {encodeCoords[0], encodeCoords[1], 0}, cropSize );
		
		//RandomAccessibleInterval<T> crop = Views.offsetInterval(crop, new long[] {encodeCoords[1], encodeCoords[0], 0}, interValSize);
		//RandomAccessibleInterval<T> crop = Views.offsetInterval(Cast.unchecked(img), new long[] {encodeCoords[1], encodeCoords[0], 0}, cropSize);
		targetDims = crop.dimensionsAsLongArray();
		createSHMArray(crop);
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
	 * TODO explain what happens with big images
	 * Method that uses SAM-like models to create a pseudo-segmentation of the image of interest.
	 * The objectSize argument is used to create a grid of points that cover the original image 
	 * and that will be used as prompts to the model. 
	 * The object masks with more accuracy will be put together to generate a pseudo-segmentation mask.
	 * 
	 * Imagine that you have a 512x256 image, with objects of an approximate size of 50 pixels. This method
	 * will create a grid of ceil(512 / 30) x ceil(256 / 30) = 18 x 9 equally spaced points, then each of
	 * them will be prompted to the SAM model to see if there is any image in the position of the point.
	 * Finally, all the masks that have been found will be put together creating a pseudo-mask that 
	 * will probably have not detected eery object in the image.
	 * Regard, that the smaller the objectSize argument, the more prompts will be evaluated, the more time
	 * the pseudo-segmentation will take but the more objects will be detected.
	 * 
	 * @param objectSize
	 * 	mean size of the object
	 * @param returnAll
	 * 	whether to return all the ROIS found with one prompt or only the largest one
	 * @return a pseudo-segmentation mask
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Polygon> noPromptAnnotation(int objectSize, boolean returnAll) 
			throws IOException, RuntimeException, InterruptedException {
		// TODO add something
		return null;
	}
	
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
		rect.height = (int) this.targetDims[1];
		rect.width = (int) this.targetDims[0];
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
		rect.height = (int) this.targetDims[1];
		rect.width = (int) this.targetDims[0];
		return processPoints(pointsList, pointsNegList, rect, returnAll);
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
		if (!this.imageSmall || this.encodeCoords[0] != 0 || this.encodeCoords[1] != 0 
				|| targetDims[0] != img.dimensionsAsLongArray()[0] || targetDims[1] != img.dimensionsAsLongArray()[1]) {
			if (encodingArea.x == -1) {
				encodingArea = getCurrentlyEncodedArea();
			} else {
				ArrayList<int[]> outsideP = getPointsNotInRect(pointsList, pointsNegList, encodingArea);
				if (outsideP.size() != 0)
					throw new IllegalArgumentException("The Rectangle containing the area to be encoded should "
						+ "contain all the points. Point {x=" + outsideP.get(0)[0] + ", y=" + outsideP.get(0)[1] + "} is out of the region.");
			}
			evaluateReencodingNeeded(pointsList, pointsNegList, encodingArea);
		}
		pointsList = adaptPointPrompts(pointsList);
		pointsNegList = adaptPointPrompts(pointsNegList);
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
	
	private List<int[]> adaptPointPrompts(List<int[]> pointsList) {
		pointsList = pointsList.stream().map(pp -> {
			int[] newPoint = new int[2];
			newPoint[0] = (int) (pp[0] - this.encodeCoords[0]);
			newPoint[1] = (int) (pp[1] - this.encodeCoords[1]);
			return newPoint;
			}).collect(Collectors.toList());
		return pointsList;
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
		if (!this.imageSmall || this.encodeCoords[0] != 0 || this.encodeCoords[1] != 0 
				|| targetDims[0] != img.dimensionsAsLongArray()[0] || targetDims[1] != img.dimensionsAsLongArray()[1]) {
			if (needsMoreResolution(boundingBox)) {
				this.encodeCoords = calculateEncodingNewCoords(boundingBox, this.img.dimensionsAsLongArray());
				reencodeCrop();
			} else if (!isAreaEncoded(boundingBox)) {
				this.encodeCoords = calculateEncodingNewCoords(boundingBox, this.img.dimensionsAsLongArray());
				reencodeCrop();
			}
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
		SharedMemoryArray maskShma = SharedMemoryArray.createSHMAFromRAI(img, false, false);
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
		// TODO remove int xMargin = (int) (targetDims[0] * 0.1);
		// TODO remove int yMargin = (int) (targetDims[1] * 0.1);
		Rectangle alreadyEncoded;
		alreadyEncoded = new Rectangle((int) encodeCoords[0], (int) encodeCoords[1], 
				(int) targetDims[0], (int) targetDims[1]);
		return alreadyEncoded;
		/** 
		 * TODO
		 * TODO
		 * TODO
		 * TODO
		if (encodeCoords[0] != 0 || encodeCoords[1] != 0 || targetDims[1] != this.img.dimensionsAsLongArray()[1]
				 || targetDims[0] != this.img.dimensionsAsLongArray()[0]) {
			alreadyEncoded = new Rectangle((int) encodeCoords[0] + xMargin / 2, (int) encodeCoords[1] + yMargin / 2, 
					(int) targetDims[0] - xMargin, (int) targetDims[1] - yMargin);
		} else {
			alreadyEncoded = new Rectangle((int) encodeCoords[0], (int) encodeCoords[1], 
					(int) targetDims[0], (int) targetDims[1]);
		}
		return alreadyEncoded;
		*/
	}
	
	/**
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * TODO Explain reencoding logic
	 * TODO Threshold for scale (0.7)
	 * TODO Threshold for scale (0.7)
	 * TODO Threshold for scale (0.7)
	 * TODO Threshold for scale (0.7)
	 * @param pointsList
	 * @param pointsNegList
	 * @param rect
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws RuntimeException
	 */
	private void evaluateReencodingNeeded(List<int[]> pointsList, List<int[]> pointsNegList, Rectangle rect) 
			throws IOException, InterruptedException, RuntimeException {
		Rectangle extendedRect = extendRect(rect, 20);
		
		Rectangle alreadyEncoded = getCurrentlyEncodedArea();
		Rectangle neededArea = getApproximateAreaNeeded(pointsList, pointsNegList, rect);
		if (rect.equals(alreadyEncoded)) neededArea = getApproximateAreaNeeded(pointsList, pointsNegList);
		ArrayList<int[]> notInRect = getPointsNotInRect(pointsList, pointsNegList, rect);
		
		if (rectContainsRect(alreadyEncoded, neededArea)
				&& alreadyEncoded.width * 0.7 < extendedRect.width && alreadyEncoded.height * 0.7 < extendedRect.height) {
			return;
		} else if (extendedRect.contains(neededArea)) {
			this.encodeCoords = new long[] {extendedRect.x, extendedRect.y};
			long width = extendedRect.width;
			long height = extendedRect.height;
			if (alreadyEncoded.x == encodeCoords[0] && alreadyEncoded.y == encodeCoords[1]
					&& alreadyEncoded.width == width && alreadyEncoded.height == height)
				return;
			this.reencodeCrop(new long[] {width, height});
		} else {
			long[] imgDims = this.img.dimensionsAsLongArray();
			long width = neededArea.width;
			long height = neededArea.height;
			this.encodeCoords[0] = Math.min(neededArea.x, imgDims[0] - width);
			this.encodeCoords[1]  = Math.min(neededArea.y, imgDims[1] - height);
			if (alreadyEncoded.x == encodeCoords[0] && alreadyEncoded.y == encodeCoords[1]
					&& alreadyEncoded.width == width && alreadyEncoded.height == height)
				return;
			this.reencodeCrop(new long[] {width, height});
		}
	}
	
	private Rectangle extendRect(Rectangle rect, int percentage) {
		double newX = Math.max(0, Math.min(rect.x - percentage * rect.width / 2 / 100, rect.x - ENCODE_MARGIN));
		double newY = Math.max(0, Math.min(rect.y - percentage * rect.height / 2 / 100, rect.y - ENCODE_MARGIN));
		
		double newW = Math.min(
				Math.max(2 * percentage * rect.width / 100, 2 * ENCODE_MARGIN) + rect.width + newX, img.dimensionsAsLongArray()[0]) 
				- newX;
		double newH = Math.min(
				Math.max(2 * percentage * rect.height / 100, 2 * ENCODE_MARGIN) + rect.height + newY, img.dimensionsAsLongArray()[1]) 
				- newY;
		return new Rectangle((int) newX, (int) newY, (int) newW, (int) newH);
	}
	
	private static boolean rectContainsRect(Rectangle outer, Rectangle inner) {
		if (outer.x <= inner.x && outer.y <= inner.y 
				&& outer.width + outer.x >= inner.width + inner.x 
				&& outer.height + outer.y >= inner.height + inner.y)
			return true;
		return false;
	}

	/**
	 * TODO remove
	 * @param pointsList
	 * @param pointsNegList
	 * @param rect
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws RuntimeException
	 */
	private void evaluateReencodingNeeded2(List<int[]> pointsList, List<int[]> pointsNegList, Rectangle rect) 
			throws IOException, InterruptedException, RuntimeException {
		Rectangle alreadyEncoded = getCurrentlyEncodedArea();
		Rectangle neededArea = getApproximateAreaNeeded(pointsList, pointsNegList, rect);
		if (rect.equals(alreadyEncoded)) neededArea = getApproximateAreaNeeded(pointsList, pointsNegList);
		ArrayList<int[]> notInRect = getPointsNotInRect(pointsList, pointsNegList, rect);
		if (alreadyEncoded.x <= rect.x && alreadyEncoded.y <= rect.y 
				&& alreadyEncoded.width + alreadyEncoded.x >= rect.width + rect.x 
				&& alreadyEncoded.height + alreadyEncoded.y >= rect.height + rect.y
				&& alreadyEncoded.width * 0.7 < rect.width && alreadyEncoded.height * 0.7 < rect.height
				&& notInRect.size() == 0 && alreadyEncoded.contains(neededArea)) {
			return;
		} else if (alreadyEncoded.x <= rect.x && alreadyEncoded.y <= rect.y 
				&& alreadyEncoded.width + alreadyEncoded.x >= rect.width + rect.x 
				&& alreadyEncoded.height + alreadyEncoded.y >= rect.height + rect.y
				&& (alreadyEncoded.width * 0.7 > rect.width || alreadyEncoded.height * 0.7 > rect.height)
				&& rect.contains(neededArea)) {
			this.encodeCoords = new long[] {(long) Math.max(0, rect.x - rect.width * 0.1), 
											(long) Math.max(0, rect.y - rect.height * 0.1)};
			long[] imgDims = this.img.dimensionsAsLongArray();
			long width = (long) Math.min(rect.width * 1.2, imgDims[0] - encodeCoords[0]);
			long height = (long) Math.min(rect.height * 1.2, imgDims[1] - encodeCoords[1]);
			if (alreadyEncoded.x == encodeCoords[0] && alreadyEncoded.y == encodeCoords[1]
					&& alreadyEncoded.width == width && alreadyEncoded.height == height)
				return;
			this.reencodeCrop(new long[] {width, height});
		} else if (alreadyEncoded.x <= neededArea.x && alreadyEncoded.y <= neededArea.y 
				&& alreadyEncoded.width + alreadyEncoded.x >= neededArea.width + neededArea.x 
				&& alreadyEncoded.height + alreadyEncoded.y >= neededArea.height + neededArea.y
				&& alreadyEncoded.width * 0.7 < neededArea.width && alreadyEncoded.height * 0.7 < neededArea.height
				&& notInRect.size() == 0) {
			return;
		} else if (!alreadyEncoded.equals(rect)) {
			this.encodeCoords = new long[] {Math.min(rect.x, neededArea.x), Math.min(rect.y, neededArea.y)};
			long[] imgDims = this.img.dimensionsAsLongArray();
			long width = Math.min(imgDims[0], Math.max(rect.x + rect.width, neededArea.x + neededArea.width) - encodeCoords[0]);
			long height = Math.min(imgDims[1], Math.max(rect.y + rect.height, neededArea.y + neededArea.height) - encodeCoords[1]);
			if (alreadyEncoded.x == encodeCoords[0] && alreadyEncoded.y == encodeCoords[1]
					&& alreadyEncoded.width == width && alreadyEncoded.height == height)
				return;
			this.reencodeCrop(new long[] {width, height});
		} else {
			long[] imgDims = this.img.dimensionsAsLongArray();
			long width = neededArea.width;
			long height = neededArea.height;
			this.encodeCoords[0] = Math.min(neededArea.x, imgDims[0] - width);
			this.encodeCoords[1]  = Math.min(neededArea.y, imgDims[1] - height);
			if (alreadyEncoded.x == encodeCoords[0] && alreadyEncoded.y == encodeCoords[1]
					&& alreadyEncoded.width == width && alreadyEncoded.height == height)
				return;
			this.reencodeCrop(new long[] {width, height});
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
		maxX = (int) (maxX + Math.max((maxX - minX) * 0.1, ENCODE_MARGIN));
		maxY = (int) (maxY + Math.max((maxY - minY) * 0.1, ENCODE_MARGIN));
		Rectangle rect = new Rectangle();
		rect.x = minX;
		rect.y = minY;
		rect.width = (int) Math.max(maxX - minX, MIN_ENCODED_AREA_SIDE);
		rect.height = (int) Math.max(maxY - minY, MIN_ENCODED_AREA_SIDE);
		rect.x -= (Math.max(rect.x + rect.width - img.dimensionsAsLongArray()[0], 0));
		rect.y -= (Math.max(rect.y + rect.height - img.dimensionsAsLongArray()[1], 0));
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
		maxX = (int) (maxX + Math.max(focusedArea.width * 0.1, ENCODE_MARGIN));
		maxY = (int) (maxY + Math.max(focusedArea.height * 0.1, ENCODE_MARGIN));
		Rectangle rect = new Rectangle();
		rect.x = minX;
		rect.y = minY;
		rect.width = (int) Math.max(maxX - minX, MIN_ENCODED_AREA_SIDE);
		rect.height = (int) Math.max(maxY - minY, MIN_ENCODED_AREA_SIDE);
		rect.x -= (Math.max(rect.x + rect.width - img.dimensionsAsLongArray()[0], 0));
		rect.y -= (Math.max(rect.y + rect.height - img.dimensionsAsLongArray()[1], 0));
		return rect;
	}
	
	/**
	 * Check whether the bounding box is inside the area that is encoded or not
	 * @param boundingBox
	 * 	the vertices of the bounding box
	 * @return whether the bounding box is within the encoded area or not
	 */
	public boolean isAreaEncoded(int[] boundingBox) {
		boolean upperLeftVertex = (boundingBox[0] > encodeCoords[0]) && (boundingBox[0] < encodeCoords[0] + targetDims[0]);
		boolean upperRightVertex = (boundingBox[2] > encodeCoords[0]) && (boundingBox[2] < encodeCoords[0] + targetDims[0]);
		boolean downLeftVertex = (boundingBox[1] > encodeCoords[1]) && (boundingBox[1] < encodeCoords[1] + targetDims[1]);
		boolean downRightVertex = (boundingBox[3] > encodeCoords[1]) && (boundingBox[3] < encodeCoords[1] + targetDims[1]);
		
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
		long encodedX = targetDims[0];
		long encodedY = targetDims[1];
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
		long encodedX = targetDims[0];
		long encodedY = targetDims[1];
		if (xSize * UPPER_REENCODE_THRESH > encodedX && ySize * UPPER_REENCODE_THRESH > encodedY)
			return true;
		return false;
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
		newSize[0] = Math.max(MIN_ENCODED_AREA_SIDE, newSize[0]);
		newSize[1] = Math.max(MIN_ENCODED_AREA_SIDE, newSize[1]);
		long[] posWrtBbox = new long[4];
		posWrtBbox[0] = (long) Math.max(0, Math.ceil((boundingBox[0] + xSize / 2) - newSize[0] / 2));
		posWrtBbox[1] = (long) Math.max(0, Math.ceil((boundingBox[1] + ySize / 2) - newSize[1] / 2));
		posWrtBbox[2] = (long) Math.min(imageSize[0], Math.floor((boundingBox[2] + xSize / 2) + newSize[0] / 2));
		posWrtBbox[3] = (long) Math.min(imageSize[1], Math.floor((boundingBox[3] + ySize / 2) + newSize[1] / 2));
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

	public String persistEncoding() throws IOException, InterruptedException {
		String uuid = UUID.randomUUID().toString();
		String saveEncodings = persistEncodingScript(uuid);
		try {
			Task task = python.task(saveEncodings);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
		} catch (IOException | InterruptedException | RuntimeException e) {
			throw e;
		}
		this.savedEncodings.add(uuid);
		return uuid;
	}

	public void selectEncoding(String encodingName) throws IOException, InterruptedException {
		if (!this.savedEncodings.contains(encodingName))
			throw new IllegalArgumentException("No saved encoding found with name: " + encodingName);
		String setEncoding = selectEncodingScript(encodingName);
		try {
			Task task = python.task(setEncoding);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
		} catch (IOException | InterruptedException | RuntimeException e) {
			throw e;
		}
		
	}


	public void deleteEncoding(String encodingName) throws IOException, InterruptedException {
		if (!this.savedEncodings.contains(encodingName))
			return;
		String returnEncoding = deleteEncodingScript(encodingName);
		try {
			Task task = python.task(returnEncoding);
			task.waitFor();
			if (task.status == TaskStatus.CANCELED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.FAILED)
				throw new RuntimeException();
			else if (task.status == TaskStatus.CRASHED)
				throw new RuntimeException();
		} catch (IOException | InterruptedException | RuntimeException e) {
			throw e;
		}
		this.savedEncodings.remove(encodingName);
	}
}
