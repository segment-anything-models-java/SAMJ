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

import java.awt.Rectangle;
import java.util.List;
import java.util.Objects;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.components.ComboBoxItem;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Interface to be implemented by the imaging software that wants to use the default SAMJ UI.
 * Provides a list of the images open to the SAMJ GUI
 * @author Carlos Garcia
 */
public abstract class ConsumerInterface {
	
	protected SAMModel selectedModel;
	
	protected PromptBridge promptBridge;
	
	public interface ConsumerCallback { 
		
		void validPromptChosen(boolean isValid);
		
		}
	
	protected Runnable guiCallback;
	
	protected ConsumerCallback callback;

	/**
	 * Method to be implemented in the softwar that wants to use the SAMJ default GUI.
	 * This method should return a list of {@link ComboBoxItem} where each instance contains
	 * a reference to an image object in the consumer software (ImagePlus in ImageJ or 
	 * Sequence in Icy) with an unique identifier
	 * 
	 * @return a list of the open images in the consumer software
	 */
	public abstract List<ComboBoxItem> getListOfOpenImages();
	

	/**
	 * Generate and display a labeling image in the consumer software.
	 * Samples within each ROI will have a different integer value,
	 * numbered from 1.
	 */
	public abstract void exportImageLabeling();

	public abstract Object getFocusedImage();

	public abstract String getFocusedImageName();

	public abstract < T extends RealType< T > & NativeType< T > > RandomAccessibleInterval<T> getFocusedImageAsRai();
	
	public abstract List<int[]> getPointRoisOnFocusImage();
	
	public abstract List<Rectangle> getRectRoisOnFocusImage();
	
	public abstract void notifyPolygons(List<Mask> masks);
	
	public abstract void activateListeners();
	
	public abstract void deactivateListeners();

	public abstract void setFocusedImage(Object image);
	
	public abstract void deselectImage();

	public abstract void deletePointRoi(int[] pp);

	public abstract void deleteRectRoi(Rectangle rect);

	public abstract boolean isValidPromptSelected();

	/**
	 * Notify when the user has clicked on the button batchSAMIZe
	 * 
	 * @param modelName
	 * 	the model that is going to be used
	 * @param maskPrompt
	 * 	the name of the image that contained the prompts wanted
	 */
	public abstract void notifyBatchSamize(String modelName, String maskPrompt);
	
	public void setModel(SAMModel model) {
		this.selectedModel = model;
	}
	
	public void setGuiCallback(Runnable guiCallback) {
		this.guiCallback = guiCallback;
	}
	
	public void setCallback(ConsumerCallback callback) {
		this.callback = callback;
	}
	
	public void setPromptBridge(PromptBridge promptBridge) {
		Objects.requireNonNull(promptBridge, "promptBridge");
		this.promptBridge = promptBridge;
	}
}
