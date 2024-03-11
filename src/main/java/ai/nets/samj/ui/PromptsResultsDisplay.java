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
package ai.nets.samj.ui;

import net.imglib2.RandomAccessibleInterval;

import java.util.List;

import ai.nets.samj.communication.model.SAMModel;

import java.awt.Polygon;
import java.io.File;

/**
 * Interface to be implemented by in the consumer software that contains all the methods 
 * necessary to communicate with and run SAMJ models and retrieve segmentation/annotations.
 * @author Carlos Garcia
 * @author Vladimir Ulman
 */
public interface PromptsResultsDisplay {
	
	/**
	 * Enum that is used to identify from where the exception is being thrown by SAMJ.
	 * It can be thrown whne encoding the image of interest {@link #ENCODING}, when producing an annotation after
	 * getting a prompt {@link #DECODING} or in other situation {@link #OTHER}
	 */
	public enum SAMJException {
	    ENCODING,
	    DECODING, 
	    OTHER; 
	}

	/**
	 * Get the image on which the wanted model will act.
	 * @param selectedModel
	 * 	the wanted model to be used to process the image
	 * @return the image that wants to be processed as a {@link RandomAccessibleInterval} in the format required by the
	 * 	SAMJ model
	 */
	RandomAccessibleInterval<?> giveProcessedSubImage(SAMModel selectedModel);

	/**
	 * Change the model that is going to be sued to process the image to another model.
	 * @param promptsToNetAdapter
	 * 	the {@link SAMModel} that wants to be used on the image
	 */
	void switchToThisNet(final SAMModel promptsToNetAdapter);
	
	/**
	 * Close the SAMJ network and the Python process where it was running
	 */
	void notifyNetToClose();

	/**
	 * 
	 * @return all the polygons created by SAMJ models for a give image
	 */
	List<Polygon> getPolygonsFromRoiManager();
	
	/**
	 * Use a mask as a prompt for SAMJ models
	 * @param mask
	 * 	the file where the mask is located. MAsks should be single channel 2D images of the same
	 * 	dimensions as the image that is being processed
	 */
	void improveExistingMask(File mask);

	/**
	 * Whether to add the ROIs being created to the ROI manager of the consumer software
	 * @param shouldBeAdding
	 * 	true to add the rois or false otherwise
	 */
	void enableAddingToRoiManager(boolean shouldBeAdding);
	
	/**
	 * Check if the ROIs are being added to the ROI manager or not
	 * @return true if rois are being added to the consumer software ROI manager or false otherwise
	 */
	boolean isAddingToRoiManager();

	/**
	 * Select bounding boxes as the way to send prompts to SAMJ models
	 */
	void switchToUsingRectangles();

	/**
	 * Select a the brush tool to create free lines that can be used as the prompts for 
	 * SAMJ models. The way to use free lines as prompts is to either convert thme into a list of points
	 * or to use the freeline as a mask prompt.
	 */
	void switchToUsingBrush();

	/**
	 * Select multiple points as the prompts used to annotate objects in SAMJ
	 */
	void switchToUsingPoints();
	
	/**
	 * Stop sneding prompts to SAMJ and thus stop finding ROIs
	 */
	void switchToNone();
	
	/**
	 * Return the image that is being processed/annotated
	 * @return
	 */
	Object getFocusedImage();
	
	/**
	 * Uses the created ROIs to create an instance segmentation mask
	 */
	void createInstanceSegmentationMask();
	
	/**
	 * This method uses the exception launched by SAMJ to display an understandable error message in the consumer software
	 * @param type
	 * 	which kind of exception is being throw from the possibilities of {@link SAMJException}
	 * @param ex
	 * 	the exception thrown by SAMJ
	 */
	void notifyException(SAMJException type, Exception ex);
}
