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
package ai.nets.samj.communication.model;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.install.SamEnvManagerAbstract;
import ai.nets.samj.models.AbstractSamJ;
import ai.nets.samj.ui.SAMJLogger;
import net.imglib2.Interval;
import net.imglib2.Localizable;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * A common ground for various placeholder classes to inform
 * the system that even this network may be available/installed.
 * It is, however, not creating/instancing any connection to any
 * (Python) network code or whatsoever, that happens only after the
 * {@link SAMModel#setImage(RandomAccessibleInterval, SAMJLogger)} is called, and reference
 * to such connection is returned.
 * @author Vladimir Ulman
 * @author Carlos Javier Garcia Lopez de Haro
 */
public abstract class SAMModel {

	protected String fullName;
	protected AbstractSamJ samj;
	protected double size;
	protected int performanceRank;
	protected int speedRank;
	protected String githubName;
	protected String paperName;
	protected String githubLink;
	protected String paperLink;
	protected boolean isHeavy;
	protected boolean onlyBiggest = false;
	protected SamEnvManagerAbstract manager;
	

	protected SAMJLogger log = new SAMJLogger() {

		@Override
		public void info(String text) {
			System.out.println(text);
		}

		@Override
		public void warn(String text) {
			System.err.println("[WARNING] -- " + text);
		}

		@Override
		public void error(String text) {
			System.err.println(text);
		}
		
	};
	
	private static final String CAUTION_STRING = "<br><p style=\"color: green;\">CAUTION: This model is"
			+ " computationally heavy. It is not recommended to use it on lower-end computers.</p>";
	
	protected static String HTML_MODEL_FORMAT = ""
			+ "<h2>%s</h2>" + System.lineSeparator()
			+ "<table>" + System.lineSeparator()
			+ "  <tr>" + System.lineSeparator()
			+ "    <th>Weights size:</th>" + System.lineSeparator()
			+ "    <td>%s MB</td>" + System.lineSeparator()
			+ "  </tr>" + System.lineSeparator()
			+ "  <tr>" + System.lineSeparator()
			+ "    <th>Speed:</th>" + System.lineSeparator()
			+ "    <td>%sth out of 6</td>" + System.lineSeparator()
			+ "  </tr>" + System.lineSeparator()
			+ "  <tr>" + System.lineSeparator()
			+ "    <th>Performance:</th>" + System.lineSeparator()
			+ "    <td>%sst out of 6</td>" + System.lineSeparator()
			+ "  </tr>" + System.lineSeparator()
			+ "  <tr>" + System.lineSeparator()
			+ "    <th>GitHub Repository:</th>" + System.lineSeparator()
			+ "    <td><a href=\"%s\">%s</a></td>" + System.lineSeparator()
			+ "  </tr>" + System.lineSeparator()
			+ "  <tr>" + System.lineSeparator()
			+ "    <th>Paper:</th>" + System.lineSeparator()
			+ "    <td><a href=\"%s\">%s</a></td>" + System.lineSeparator()
			+ "  </tr>" + System.lineSeparator()
			+ "</table>" + System.lineSeparator()
			+ "";
	
	public static String HTML_NOT_INSTALLED = "<br><p style=\"color: red;\">This model is not installed yet.</p>";
	
	/**
	 * 
	 * @return the name of the model architecture
	 */
	public abstract String getName();
	/**
	 * 
	 * @return the axes order required for the input image to the model
	 */
	public abstract String getInputImageAxes();
	
	/**
	 * 
	 * @return the {@link SamEnvManagerAbstract} used to install this model
	 */
	public SamEnvManagerAbstract getInstallationManger() {
		return this.manager;
	}

	/**
	 * Instantiate a SAM based model. Provide also an image that will be encoded by the model encoder
	 * @param image
	 * 	the image of interest for segmentation or annotation
	 * @param useThisLoggerForIt
	 * 	a logger to provide info about the progress
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public abstract <T extends RealType<T> & NativeType<T>> 
	void setImage(final RandomAccessibleInterval<T> image, final SAMJLogger useThisLoggerForIt) 
			throws IOException, RuntimeException, InterruptedException;

	/**
	 * 
	 * @return a text describing the model.
	 */
	public String getDescription() {
		String description = String.format(HTML_MODEL_FORMAT, fullName, "" + size, 
				"" + speedRank, "" + performanceRank, githubLink, githubName, paperLink, paperName);
		boolean installed = this.isInstalled();
		description = this.isHeavy & installed ? CAUTION_STRING + description: description;
		description = installed ? description : HTML_NOT_INSTALLED + description;
		return description;
	}
	
	/**
	 * 
	 * @return true or false whether all the things needed to run the model are already installed or not.
	 * 	This includes the Python environment and requirements, the model weights or micrommaba
	 */
	public boolean isInstalled() {
		return this.getInstallationManger().checkEverythingInstalled();
	}
	

	/**
	 * Set whether SAMJ will only return the biggest ROI or all of them when several are obtained from the model
	 * @param onlyBiggest
	 * 	whether the ouput of SAMJ will only be the biggest ROI obtained or all of them.
	 */
	public void setReturnOnlyBiggest(boolean onlyBiggest) {
		this.onlyBiggest = onlyBiggest;
	}

	public List<Mask> processBatchOfPoints(List<int[]> points) throws IOException, RuntimeException, InterruptedException {
		return samj.processBatchOfPoints(points, !onlyBiggest);
	}

	public <T extends RealType<T> & NativeType<T>>
	List<Mask> processBatchOfPrompts(List<int[]> points, List<Rectangle> rects, RandomAccessibleInterval<T> rai) 
			throws IOException, RuntimeException, InterruptedException {
		return samj.processBatchOfPrompts(points, rects, rai, !onlyBiggest);
	}

	/**
	 * Get a 2D segmentation/annotation using two lists of points as the prompts. 
	 * @param listOfPoints2D
	 * 	List of points that make reference to the instance of interest
	 * @param listOfNegPoints2D
	 * 	list of points that makes reference to something that is not the instance of interest. This
	 * 	points make reference to the background
	 * @return a list of polygons that represent the edges of each of the masks segmented by the model
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Mask> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			List<int[]> list = listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			List<int[]> negList = listOfNegPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			if (negList.size() == 0) return samj.processPoints(list, !onlyBiggest);
			else return samj.processPoints(list, negList, !onlyBiggest);
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(this.getName()+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}
	

	/**
	 * Get a 2D segmentation/annotation using two lists of points as the prompts. 
	 * @param listOfPoints2D
	 * 	List of points that make reference to the instance of interest
	 * @param listOfNegPoints2D
	 * 	list of points that makes reference to something that is not the instance of interest. This
	 * 	points make reference to the background
	 * @param zoomedRectangle
	 * 	rectangle that specifies the area that is being zoomed in.It will be the area encoded.
	 * @return a list of polygons that represent the edges of each of the masks segmented by the model
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Mask> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D,
			Rectangle zoomedRectangle) throws IOException, RuntimeException, InterruptedException {
		try {
			List<int[]> list = listOfPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			List<int[]> negList = listOfNegPoints2D.stream()
					.map(i -> new int[] {(int) i.positionAsDoubleArray()[0], (int) i.positionAsDoubleArray()[1]}).collect(Collectors.toList());
			if (negList.size() == 0) return samj.processPoints(list, zoomedRectangle, !onlyBiggest);
			else return samj.processPoints(list, negList, zoomedRectangle, !onlyBiggest);
		} catch (IOException | RuntimeException | InterruptedException e) {
			log.error(getName()+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	/**
	 * Get a 2D segmentation/annotation using a bounding box as the prompt. 
	 * @param boundingBox2D
	 * 	a bounding box around the instance of interest
	 * @return a list of polygons that represent the edges of each of the masks segmented by the model
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public List<Mask> fetch2dSegmentation(Interval boundingBox2D) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			//order to processBox() should be: x0,y0, x1,y1
			final int bbox[] = {
				(int)boundingBox2D.min(0),
				(int)boundingBox2D.min(1),
				(int)boundingBox2D.max(0),
				(int)boundingBox2D.max(1)
			};
			return samj.processBox(bbox, !onlyBiggest);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(getName()+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	/**
	 * Get a 2D segmentation/annotation using an existing mask as the prompt. 
	 * @param <T>
	 * 	the ImgLib2 data types allowed for the input mask
	 * @param rai
	 * 	the mask as a {@link RandomAccessibleInterval} 
	 * @return a list of polygons that represent the edges of each of the masks segmented by the model
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	public <T extends RealType<T> & NativeType<T>> List<Mask> fetch2dSegmentationFromMask(RandomAccessibleInterval<T> rai) 
			throws IOException, InterruptedException, RuntimeException {
		try {
			return samj.processMask(rai, !onlyBiggest);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(getName()+", providing empty result because of some trouble: "+e.getMessage());
			throw e;
		}
	}

	/**
	 * Notify the User Interface that the model has been closed
	 */
	public void notifyUiHasBeenClosed() {
		if (log != null)
			log.info(getName()+": OKAY, I'm closing myself...");
		closeProcess();
	}
	
	/**
	 * Close the Python process where the model is being executed
	 */
	public void closeProcess() {
	if (samj != null)
			samj.close();
		samj = null;
	}

	public String persistEncoding() throws IOException, InterruptedException {
		try {
			return samj.persistEncoding();
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(getName()+", unable to persist the encoding: "+e.getMessage());
			throw e;
		}
	}

	public void selectEncoding(String encodingName) throws IOException, InterruptedException {
		try {
			samj.selectEncoding(encodingName);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(getName()+", unable to persist the encoding named '" + encodingName + "': "+e.getMessage());
			throw e;
		}
	}

	public void deleteEncoding(String encodingName) throws IOException, InterruptedException {
		try {
			samj.deleteEncoding(encodingName);
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(getName()+", unable to delete the encoding named '" + encodingName + "': "+e.getMessage());
			throw e;
		}
	}
	
}
