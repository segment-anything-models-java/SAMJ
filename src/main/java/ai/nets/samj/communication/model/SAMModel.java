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

import java.awt.Polygon;
import java.io.IOException;
import java.util.List;

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
 * {@link SAMModel#instantiate(RandomAccessibleInterval, SAMJLogger)} is called, and reference
 * to such connection is returned.
 * @author Vladimir Ulman
 * @author Carlos Javier Garcia Lopez de Haro
 */
public interface SAMModel {
	
	public static String HTML_NOT_INSTALLED = "<br><p style=\"color: red;\">This model is not installed yet.</p>";
	
	/**
	 * 
	 * @return the name of the model architecture
	 */
	String getName();
	/**
	 * 
	 * @return the axes order required for the input image to the model
	 */
	String getInputImageAxes();
	/**
	 * 
	 * @return a text describing the model.
	 */
	String getDescription();
	/**
	 * 
	 * @return true or false whether all the things needed to run the model are already installed or not.
	 * 	This includes the Python environment and requirements, the model weights or micrommaba
	 */
	boolean isInstalled();
	/**
	 * Set whether the model is installed or
	 * @param installed
	 */
	void setInstalled(boolean installed);

	/**
	 * Instantiate a SAM based model. Provide also an image that will be encoded by the model encoder
	 * @param image
	 * 	the image of interest for segmentation or annotation
	 * @param useThisLoggerForIt
	 * 	a logger to provide info about the progress
	 * @return an instance of a SAM-based model
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	SAMModel instantiate(final RandomAccessibleInterval<?> image, final SAMJLogger useThisLoggerForIt) throws IOException, RuntimeException, InterruptedException;

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
	List<Polygon> fetch2dSegmentation(List<Localizable> listOfPoints2D, List<Localizable> listOfNegPoints2D) throws IOException, RuntimeException, InterruptedException;

	/**
	 * Get a 2D segmentation/annotation using a bounding box as the prompt. 
	 * @param boundingBox2D
	 * 	a bounding box around the instance of interest
	 * @return a list of polygons that represent the edges of each of the masks segmented by the model
	 * @throws IOException if any of the files needed to run the Python script is missing 
	 * @throws RuntimeException if there is any error running the Python process
	 * @throws InterruptedException if the process in interrupted
	 */
	List<Polygon> fetch2dSegmentation(Interval boundingBox2D) throws IOException, RuntimeException, InterruptedException;
	
	/**
	 * Set whether SAMJ will only return the biggest ROI or all of them when several are obtained from the model
	 * @param onlyBiggest
	 * 	whether the ouput of SAMJ will only be the biggest ROI obtained or all of them.
	 */
	void setReturnOnlyBiggest(boolean onlyBiggest);

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
	public <T extends RealType<T> & NativeType<T>> List<Polygon> fetch2dSegmentationFromMask(RandomAccessibleInterval<T> rai) throws IOException, RuntimeException, InterruptedException;

	/**
	 * Close the Python process where the model is being executed
	 */
	void closeProcess();

	/**
	 * Notify the User Interface that the model has been closed
	 */
	void notifyUiHasBeenClosed();
}