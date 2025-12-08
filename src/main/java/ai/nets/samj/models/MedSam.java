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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ai.nets.samj.install.MedSamEnvManager;
import ai.nets.samj.install.SamEnvManagerAbstract;
import io.bioimage.modelrunner.apposed.appose.Environment;
import io.bioimage.modelrunner.apposed.appose.Service.Task;
import io.bioimage.modelrunner.apposed.appose.Service.TaskStatus;
import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import io.bioimage.modelrunner.utils.CommonUtils;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Class that provides the needed methods to run MedSAM from Java.
 * 
 * MedSAM is a fine-tuned version of SAM specifically trained on medical imaging data.
 * It uses the vit_b architecture from the original SAM.
 * 
 * @author SAMJ developers
 */
public class MedSam extends AbstractSamJ {
	
	/**
	 * List of encodings that are cached to avoid recalculating
	 */
	List<String> savedEncodings = new ArrayList<String>();
	
	/**
	 * All the Python imports and configurations needed to start using MedSAM.
	 */
	public static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "import numpy as np" + System.lineSeparator()
			+ "from skimage import measure" + System.lineSeparator()
			+ "measure.label(np.ones((10, 10)), connectivity=1)" + System.lineSeparator()
			+ "import torch" + System.lineSeparator()
			+ "device = 'cpu'" + System.lineSeparator()
			+ ((!IS_APPLE_SILICON || true) ? ""
					: "from torch.backends import mps" + System.lineSeparator()
					+ "if mps.is_built() and mps.is_available():" + System.lineSeparator()
					+ "  device = 'mps'" + System.lineSeparator())
			+ "from scipy.ndimage import binary_fill_holes" + System.lineSeparator()
			+ "from scipy.ndimage import label" + System.lineSeparator()
			+ "import sys" + System.lineSeparator()
			+ "import os" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "from segment_anything import sam_model_registry, SamPredictor" + System.lineSeparator()
			+ "model = sam_model_registry['vit_b'](checkpoint=r'%s').to(device)" + System.lineSeparator()
			+ "model.eval()" + System.lineSeparator()
			+ "predictor = SamPredictor(model)" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "encodings_map = {}" + System.lineSeparator()
			+ "globals()['encodings_map'] = encodings_map" + System.lineSeparator()
			+ "globals()['shared_memory'] = shared_memory" + System.lineSeparator()
			+ "globals()['measure'] = measure" + System.lineSeparator()
			+ "globals()['np'] = np" + System.lineSeparator()
			+ "globals()['torch'] = torch" + System.lineSeparator()
			+ "globals()['label'] = label" + System.lineSeparator()
			+ "globals()['binary_fill_holes'] = binary_fill_holes" + System.lineSeparator()
			+ "globals()['predictor'] = predictor" + System.lineSeparator()
			+ "globals()['device'] = device" + System.lineSeparator();
	
	/**
	 * String containing the Python imports code after it has been formatted with the correct 
	 * paths and names
	 */
	private String IMPORTS_FORMATED;

	/**
	 * Create an instance of the class to be able to run MedSAM in Java.
	 * 
	 * @param manager
	 * 	environment manager that contains all the paths to the environments needed, Python executables and model weights
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	private MedSam(SamEnvManagerAbstract manager) throws IOException, RuntimeException, InterruptedException {
		this(manager, (t) -> {}, false);
	}

	/**
	 * Create an instance of the class to be able to run MedSAM in Java.
	 * 
	 * @param manager
	 * 	environment manager that contains all the paths to the environments needed, Python executables and model weights
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and output to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	private MedSam(SamEnvManagerAbstract manager,
	               final DebugTextPrinter debugPrinter,
	               final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {

		this.debugPrinter = debugPrinter;
		this.isDebugging = printPythonCode;

		this.env = new Environment() {
			@Override public String base() { return manager.getModelEnv(); }
		};
		python = env.python();
		python.debug(debugPrinter::printText);
		IMPORTS_FORMATED = String.format(IMPORTS, manager.getModelWeigthPath());
		
		Task task = python.task(IMPORTS_FORMATED + PythonMethods.RLE_METHOD + PythonMethods.TRACE_EDGES);
		task.waitFor();
		if (task.status == TaskStatus.CANCELED)
			throw new RuntimeException("Task canceled");
		else if (task.status == TaskStatus.FAILED)
			throw new RuntimeException(task.error);
		else if (task.status == TaskStatus.CRASHED)
			throw new RuntimeException(task.error);
	}

	/**
	 * Create a MedSam instance that allows to use MedSAM on an image.
	 * 
	 * @param manager
	 * 	environment manager that contains all the paths to the environments needed, Python executables and model weights
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and output to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @return an instance of {@link MedSam} that allows running MedSAM on an image
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static MedSam initializeSam(SamEnvManagerAbstract manager,
	                                    final DebugTextPrinter debugPrinter,
	                                    final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		MedSam sam = null;
		try{
			sam = new MedSam(manager, debugPrinter, printPythonCode);
			sam.encodeCoords = new long[] {0, 0};
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	/**
	 * Create a MedSam instance that allows to use MedSAM on an image.
	 * 
	 * @param manager
	 * 	environment manager that contains all the paths to the environments needed, Python executables and model weights
	 * @return an instance of {@link MedSam} that allows running MedSAM on an image
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static MedSam initializeSam(SamEnvManagerAbstract manager) 
			throws IOException, RuntimeException, InterruptedException {
		MedSam sam = null;
		try{
			sam = new MedSam(manager);
			sam.encodeCoords = new long[] {0, 0};
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	@Override
	protected <T extends RealType<T> & NativeType<T>>
	void setImageOfInterest(RandomAccessibleInterval<T> rai) {
		long[] dims = rai.dimensionsAsLongArray();
		if (dims.length == 2)
			rai = Views.addDimension(rai, 0, 0);
		dims = rai.dimensionsAsLongArray();
		if (dims[2] == 1)
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, dims[0] - 1, dims[1] - 1, 2}) );
		this.img = rai;
		this.targetDims = img.dimensionsAsLongArray();
	}
	
	@Override
	protected <T extends RealType<T> & NativeType<T>> void createSHMArray(RandomAccessibleInterval<T> imShared) {
		RandomAccessibleInterval<T> imageToBeSent = ImgLib2Utils.reescaleIfNeeded(imShared);
		long[] dims = imageToBeSent.dimensionsAsLongArray();
		shma = SharedMemoryArray.create(new long[] {dims[0], dims[1], dims[2]}, new UnsignedByteType(), false, false);
		adaptImageToModel(imageToBeSent, shma.getSharedRAI());
	}

	@Override
	protected <T extends RealType<T> & NativeType<T>> void createEncodeImageScript() {
		script = "";
		script += "im_shm = shared_memory.SharedMemory(name='"
				+ shma.getNameForPython() + "', size=" + shma.getSize() 
				+ ")" + System.lineSeparator();
		int size = (int) targetDims[2];
		for (int i = 0; i < targetDims.length - 1; i ++) {
			size *= Math.ceil(targetDims[i] / (double) scale);
		}
		script += "im = np.ndarray(" + size + ", dtype='" + CommonUtils.getDataTypeFromRAI(Cast.unchecked(shma.getSharedRAI()))
				+ "', buffer=im_shm.buf).reshape([";
		for (int i = 0; i < targetDims.length - 1; i ++)
			script += (int) Math.ceil(targetDims[i] / (double) scale) + ", ";
		script += targetDims[2];
		script += "])" + System.lineSeparator();
		script += "im = np.transpose(im, (1, 0, 2))" + System.lineSeparator();
		script += "im_shm.unlink()" + System.lineSeparator();
		script += "predictor.set_image(im)" + System.lineSeparator();
		script += "task.update('encoded')" + System.lineSeparator();
		script += "task.outputs['encoded'] = True" + System.lineSeparator();
	}

	@Override
	protected void processPromptsBatchWithSAM(SharedMemoryArray shmArr, boolean returnAll) {
		String code = "";
		// Note: shmArr (mask array) is optional and typically null for multi-point batch processing
		// It's only used when providing a mask ROI as additional input
		// For now, MedSAM processes each point prompt individually (simplified version)
		code += "num_prompts = len(point_prompts) + len(rect_prompts)" + System.lineSeparator();
		code += "contours_x = []" + System.lineSeparator();
		code += "contours_y = []" + System.lineSeparator();
		code += "rle_masks = []" + System.lineSeparator();
		code += "for i, point_prompt in enumerate(point_prompts):" + System.lineSeparator();
		code += "    point_coords = np.array([[point_prompt[0], point_prompt[1]]])" + System.lineSeparator();
		code += "    point_labels = np.array([1])" + System.lineSeparator();
		code += "    masks, _, _ = predictor.predict(point_coords, point_labels, multimask_output=False)" + System.lineSeparator();
		code += "    c_x, c_y, r_m = get_polygons_from_binary_mask(masks[0], only_biggest=True)" + System.lineSeparator();
		code += "    contours_x += c_x" + System.lineSeparator();
		code += "    contours_y += c_y" + System.lineSeparator();
		code += "    rle_masks += r_m" + System.lineSeparator();
		code += "for i, rect_prompt in enumerate(rect_prompts):" + System.lineSeparator();
		code += "    rect_coords = np.array([[rect_prompt[0], rect_prompt[1]], [rect_prompt[2], rect_prompt[3]]])" + System.lineSeparator();
		code += "    masks, _, _ = predictor.predict(point_coords=None, point_labels=None, box=rect_coords, multimask_output=False)" + System.lineSeparator();
		code += "    c_x, c_y, r_m = get_polygons_from_binary_mask(masks[0], only_biggest=True)" + System.lineSeparator();
		code += "    contours_x += c_x" + System.lineSeparator();
		code += "    contours_y += c_y" + System.lineSeparator();
		code += "    rle_masks += r_m" + System.lineSeparator();
		code += "task.outputs['n'] = str(len(rle_masks))" + System.lineSeparator();
		code += "task.outputs['contours_x'] = contours_x" + System.lineSeparator();
		code += "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		code += "task.outputs['rle'] = rle_masks" + System.lineSeparator();
		this.script = code;
	}

	@Override
	protected void processBoxWithSAM(boolean returnAll) {
		String code = "" + System.lineSeparator();
		code += "input_box_np = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator();
		code += "masks, _, _ = predictor.predict(" + System.lineSeparator();
		code += "    point_coords=None," + System.lineSeparator();
		code += "    point_labels=None," + System.lineSeparator();
		code += "    box=input_box_np," + System.lineSeparator();
		code += "    multimask_output=" + (returnAll ? "True" : "False") + ")" + System.lineSeparator();
		code += "input_box_np = None" + System.lineSeparator();
		processMasksWithPython(code);
	}

	@Override
	protected void processPointsWithSAM(int nPoints, int nNegPoints, boolean returnAll) {
		String code = "" + System.lineSeparator();
		code += "input_points_list = []" + System.lineSeparator();
		code += "input_neg_points_list = []" + System.lineSeparator();
		for (int n = 0; n < nPoints; n ++)
			code += "input_points_list.append([input_points[" + n + "][0], input_points[" + n + "][1]])" + System.lineSeparator();
		for (int n = 0; n < nNegPoints; n ++)
			code += "input_neg_points_list.append([input_neg_points[" + n + "][0], input_neg_points[" + n + "][1]])" + System.lineSeparator();
		code += "input_points_array = np.concatenate("
				+ "(np.array(input_points_list).reshape(" + nPoints + ", 2), np.array(input_neg_points_list).reshape(" + nNegPoints + ", 2))"
				+ ", axis=0)" + System.lineSeparator();
		code += "input_labels_array = np.array([1] * " + (nPoints + nNegPoints) + ")" + System.lineSeparator();
		code += "input_labels_array[" + nPoints + ":] -= 1" + System.lineSeparator();
		code += "masks, _, _ = predictor.predict(" + System.lineSeparator();
		code += "    point_coords=input_points_array," + System.lineSeparator();
		code += "    point_labels=input_labels_array," + System.lineSeparator();
		code += "    multimask_output=" + (returnAll ? "True" : "False") + ")" + System.lineSeparator();
		code += "input_points_array = None" + System.lineSeparator();
		code += "input_labels_array = None" + System.lineSeparator();
		processMasksWithPython(code);
	}

	@Override
	protected void cellSAM(List<int[]> grid, boolean returnAll) {
		throw new UnsupportedOperationException("CellSAM is not supported for MedSAM");
	}

	@Override
	protected String persistEncodingScript(String encodingName) {
		String code = "encodings_map['" + encodingName + "'] = predictor.features" + System.lineSeparator();
		code += "encodings_map['" + encodingName + "_original_size'] = predictor.original_size" + System.lineSeparator();
		code += "encodings_map['" + encodingName + "_input_size'] = predictor.input_size" + System.lineSeparator();
		code += "task.update('encoded')" + System.lineSeparator();
		code += "task.outputs['encoded'] = True" + System.lineSeparator();
		return code;
	}

	@Override
	protected String selectEncodingScript(String encodingName) {
		String code = "predictor.features = encodings_map['" + encodingName + "']" + System.lineSeparator();
		code += "predictor.is_image_set = True" + System.lineSeparator();
		code += "predictor.original_size = encodings_map['" + encodingName + "_original_size']" + System.lineSeparator();
		code += "predictor.input_size = encodings_map['" + encodingName + "_input_size']" + System.lineSeparator();
		return code;
	}

	@Override
	protected String deleteEncodingScript(String encodingName) {
		String code = "del encodings_map['" + encodingName + "']" + System.lineSeparator();
		code += "del encodings_map['" + encodingName + "_original_size']" + System.lineSeparator();
		code += "del encodings_map['" + encodingName + "_input_size']" + System.lineSeparator();
		return code;
	}
	
	private void processMasksWithPython(String code) {
		code += "contours_x, contours_y, rle_masks = get_polygons_from_binary_mask(masks[0], only_biggest=" 
				+ (this.isIJROIManager ? "False" : "True") + ")" + System.lineSeparator();
		code += "task.outputs['contours_x'] = contours_x" + System.lineSeparator();
		code += "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		code += "task.outputs['rle'] = rle_masks" + System.lineSeparator();
		code += "masks = None" + System.lineSeparator();
		this.script = code;
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void adaptImageToModel(RandomAccessibleInterval<T> ogImg, RandomAccessibleInterval<T> targetImg) {
		if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int i = 0; i < 3; i ++) {
				RealTypeConverters.copyFromTo( ImgLib2Utils.convertViewToRGB(Views.hyperSlice(ogImg, 2, i), this.debugPrinter), 
						Views.hyperSlice(targetImg, 2, i) );
			}
		} else if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 1) {
			debugPrinter.printText("CONVERTED 1 CHANNEL IMAGE INTO 3 TO BE FEEDED TO MEDSAM");
			IntervalView<UnsignedByteType> resIm = 
					Views.interval( Views.expandMirrorDouble(ImgLib2Utils.convertViewToRGB(ogImg, this.debugPrinter), new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, ogImg.dimensionsAsLongArray()[0] - 1, ogImg.dimensionsAsLongArray()[1] - 1, 2}) );
			RealTypeConverters.copyFromTo( resIm, targetImg );
		} else if (ogImg.numDimensions() == 2) {
			adaptImageToModel(Views.addDimension(ogImg, 0, 0), targetImg);
		} else {
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'yxc', first dimension height, second width and third channels.");
		}
	}
}
