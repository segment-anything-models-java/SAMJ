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

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.install.SamEnvManager;

import java.io.File;
import java.io.IOException;

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
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Cast;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Class that provides the needed methods to run EfficientViTSAM models from Java
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class EfficientViTSamJ extends AbstractSamJ {
	/**
	 * Map that associates the key for each of the existing EfficientViTSAM models to its complete name
	 */
	private static final HashMap<String, String> MODELS_DICT = new HashMap<String, String>();
	static {
		MODELS_DICT.put("l0", "efficientvit_sam_l0");
		MODELS_DICT.put("l1", "efficientvit_sam_l1");
		MODELS_DICT.put("l2", "efficientvit_sam_l2");
		MODELS_DICT.put("xl0", "efficientvit_sam_xl0");
		MODELS_DICT.put("xl1", "efficientvit_sam_xl1");
	}
	/**
	 * All the Python imports and configurations needed to start using EfficientViTSAM.
	 */
	public static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "import numpy as np" + System.lineSeparator()
			+ "from skimage import measure" + System.lineSeparator()
			+ "measure.label(np.ones((10, 10)), connectivity=1)" + System.lineSeparator()
			+ "import torch" + System.lineSeparator()
			+ "import sys" + System.lineSeparator()
			+ "import os" + System.lineSeparator()
			+ "os.chdir(r'%s')" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "task.update('import sam')" + System.lineSeparator()
			+ "from efficientvit.models.efficientvit import EfficientViTSam, %s" + System.lineSeparator()
			+ "from efficientvit.models.efficientvit.sam import EfficientViTSamPredictor" + System.lineSeparator()
			+ "task.update('imported')" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "model = %s().cpu().eval()" + System.lineSeparator()
			+ "eps = 1e-6" + System.lineSeparator()
			+ "for m in model.modules():" + System.lineSeparator()
			+ "  if isinstance(m, (torch.nn.GroupNorm, torch.nn.LayerNorm, torch.nn.modules.batchnorm._BatchNorm)):" + System.lineSeparator()
			+ "    if eps is not None:" + System.lineSeparator()
			+ "      m.eps = eps" + System.lineSeparator()
			+ "f_name = os.path.realpath(os.path.expanduser(r'%s'))" + System.lineSeparator()
			+ "weight = torch.load(f_name, map_location='cpu')" + System.lineSeparator()
			+ "if \"state_dict\" in weight:" + System.lineSeparator()
			+ "  weight = weight[\"state_dict\"]" + System.lineSeparator()
			+ "model.load_state_dict(weight)" + System.lineSeparator()
			+ "predictor = EfficientViTSamPredictor(model)" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "globals()['shared_memory'] = shared_memory" + System.lineSeparator()
			+ "globals()['measure'] = measure" + System.lineSeparator()
			+ "globals()['np'] = np" + System.lineSeparator()
			+ "globals()['torch'] = torch" + System.lineSeparator()
			+ "globals()['predictor'] = predictor" + System.lineSeparator();
	/**
	 * String containing the Python imports code after it has been formatted with the correct 
	 * paths and names
	 */
	private String IMPORTS_FORMATED;

	/**
	 * Create an instance of the class to be able to run EfficientViTSAM in Java.
	 * 
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param type
	 * 	EfficientViTSAM model type that we want to use, it can be "l0", "l1", "l2", "xl1" or "xl2"
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	private EfficientViTSamJ(SamEnvManager manager, String type) throws IOException, RuntimeException, InterruptedException {
		this(manager, type, (t) -> {}, false);
	}

	/**
	 * Create an instance of the class to be able to run EfficientViTSAM in Java.
	 * 
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param type
	 * 	EfficientViTSAM model type that we want to use, it can be "l0", "l1", "l2", "xl1" or "xl2"
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and ouptut to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 * 
	 */
	private EfficientViTSamJ(SamEnvManager manager, String type,
	                      final DebugTextPrinter debugPrinter,
	                      final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {

		if (!MODELS_DICT.keySet().contains(type))
			throw new IllegalArgumentException("The model type should be one of hte following: " 
							+ MODELS_DICT.keySet().stream().collect(Collectors.toList()));
		this.debugPrinter = debugPrinter;
		this.isDebugging = printPythonCode;

		this.env = new Environment() {
			@Override public String base() { return manager.getEfficientViTSamEnv(); }
			};
		python = env.python();
		python.debug(debugPrinter::printText);
		IMPORTS_FORMATED = String.format(IMPORTS,
									manager.getEfficientViTSamEnv() + File.separator + SamEnvManager.EVITSAM_NAME,
									MODELS_DICT.get(type), MODELS_DICT.get(type), manager.getEfficientViTSAMWeightsPath(type));
		
		printScript(IMPORTS_FORMATED + PythonMethods.TRACE_EDGES, "Edges tracing code");
		Task task = python.task(IMPORTS_FORMATED + PythonMethods.TRACE_EDGES);
		System.out.println(IMPORTS_FORMATED + PythonMethods.TRACE_EDGES);
		task.waitFor();
		if (task.status == TaskStatus.CANCELED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.FAILED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.CRASHED)
			throw new RuntimeException();
	}

	/**
	 * Create an EfficientViTSamJ instance that allows to use EfficientViTSAM on an image.
	 * This method encodes the image provided, so depending on the computer and on the model
	 * it might take some time
	 * 
	 * @param <T>
	 * 	the ImgLib2 data type of the image provided
	 * @param modelType
	 * 	EfficientViTSAM model type that we want to use, it can be "l0", "l1", "l2", "xl1" or "xl2"
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param image
	 * 	the image where SAM is going to be run on
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and ouptut to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @return an instance of {@link EfficientViTSamJ} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(String modelType, SamEnvManager manager,
	              RandomAccessibleInterval<T> image,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		EfficientViTSamJ sam = null;
		try{
			sam = new EfficientViTSamJ(manager, modelType, debugPrinter, printPythonCode);
			sam.encodeCoords = new long[] {0, 0};
			sam.updateImage(image);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	/**
	 * Create an EfficientViTSamJ instance that allows to use EfficientViTSAM on an image.
	 * This method encodes the image provided, so depending on the computer and on the model
	 * it might take some time
	 * 
	 * @param <T>
	 * 	the ImgLib2 data type of the image provided
	 * @param modelType
	 * 	EfficientViTSAM model type that we want to use, it can be "l0", "l1", "l2", "xl1" or "xl2"
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param image
	 * 	the image where SAM is going to be run on
	 * @return an instance of {@link EfficientViTSamJ} that allows running EfficienTViTSAM on an image
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(String modelType, SamEnvManager manager, RandomAccessibleInterval<T> image) 
				throws IOException, RuntimeException, InterruptedException {
		EfficientViTSamJ sam = null;
		try{
			sam = new EfficientViTSamJ(manager, modelType);
			sam.encodeCoords = new long[] {0, 0};
			sam.updateImage(image);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	/**
	 * Create an EfficientViTSamJ instance that allows to use EfficientViTSAM on an image.
	 * This method encodes the image provided, so depending on the computer and on the model
	 * it might take some time.
	 * 
	 * The model used is the default one {@value SamEnvManager#DEFAULT_EVITSAM}
	 * 
	 * @param <T>
	 * 	the ImgLib2 data type of the image provided
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param image
	 * 	the image where SAM is going to be run on
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and ouptut to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @return an instance of {@link EfficientViTSamJ} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(SamEnvManager manager,
	              RandomAccessibleInterval<T> image,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(SamEnvManager.DEFAULT_EVITSAM, manager, image, debugPrinter, printPythonCode);
	}

	/**
	 * Create an EfficientViTSamJ instance that allows to use EfficientViTSAM on an image.
	 * This method encodes the image provided, so depending on the computer and on the model
	 * it might take some time.
	 * 
	 * The model used is the default one {@value SamEnvManager#DEFAULT_EVITSAM}
	 * 
	 * @param <T>
	 * 	the ImgLib2 data type of the image provided
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param image
	 * 	the image where SAM is going to be run on
	 * @return an instance of {@link EfficientViTSamJ} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(SamEnvManager manager, RandomAccessibleInterval<T> image) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(SamEnvManager.DEFAULT_EVITSAM, manager, image);
	}

	@Override
	protected <T extends RealType<T> & NativeType<T>> void setImageOfInterest(RandomAccessibleInterval<T> rai) {
		checkImageIsFine(rai);
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
	protected void createEncodeImageScript() {
		script = "";
		script += "im_shm = shared_memory.SharedMemory(name='"
				+ shma.getNameForPython() + "', size=" + shma.getSize() 
				+ ")" + System.lineSeparator();
		int size = 1;
		for (long l : targetDims) {size *= l;}
		script += "im = np.ndarray(" + size + ", dtype='" + CommonUtils.getDataTypeFromRAI(Cast.unchecked(shma.getSharedRAI()))
				+ "', buffer=im_shm.buf).reshape([";
		for (long ll : targetDims)
			script += ll + ", ";
		script = script.substring(0, script.length() - 2);
		script += "])" + System.lineSeparator();
		script += "im = np.transpose(im, (1, 0, 2))" + System.lineSeparator();
		//code += "np.save('/home/carlos/git/aa.npy', im)" + System.lineSeparator();
		script += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		script += ""
			+ "task.update(str(im.shape))" + System.lineSeparator()
			+ "predictor.set_image(im)";
	}

	@Override
	protected <T extends RealType<T> & NativeType<T>> void createSHMArray(RandomAccessibleInterval<T> imShared) {
		RandomAccessibleInterval<T> imageToBeSent = ImgLib2Utils.reescaleIfNeeded(imShared);
		long[] dims = imageToBeSent.dimensionsAsLongArray();
		shma = SharedMemoryArray.create(new long[] {dims[0], dims[1], dims[2]}, new UnsignedByteType(), false, false);
		adaptImageToModel(imageToBeSent, shma.getSharedRAI());
	}

	@Override
	protected void processMasksWithSam(SharedMemoryArray shmArr, boolean returnAll) {
		String code = "";
		code += "shm_mask = shared_memory.SharedMemory(name='" + shmArr.getNameForPython() + "')" + System.lineSeparator();
		code += "mask = np.frombuffer(buffer=shm_mask.buf, dtype='" + shmArr.getOriginalDataType() + "').reshape([";
		for (long l : shmArr.getOriginalShape()) 
			code += l + ",";
		code += "])" + System.lineSeparator();
		code += "different_mask_vals = np.unique(mask)" + System.lineSeparator();
		code += "contours_x = []" + System.lineSeparator();
		code += "contours_y = []" + System.lineSeparator();
		code += "for val in different_mask_vals:" + System.lineSeparator()
			  + "  if val < 1:" + System.lineSeparator()
			  + "    continue" + System.lineSeparator()
			  + "  locations = np.where(mask == val)" + System.lineSeparator()
			  + "  input_points_pos = np.zeros((locations[0].shape[0], 2))" + System.lineSeparator()
			  + "  input_labels_pos = np.ones((locations[0].shape[0]))" + System.lineSeparator()
			  + "  locations_neg = np.where((mask != val) & (mask != 0))" + System.lineSeparator()
			  + "  input_points_neg = np.zeros((locations_neg[0].shape[0], 2))" + System.lineSeparator()
			  + "  input_labels_neg = np.zeros((locations_neg[0].shape[0]))" + System.lineSeparator()
			  + "  input_points_pos[:, 0] = locations[0]" + System.lineSeparator()
			  + "  input_points_pos[:, 1] = locations[1]" + System.lineSeparator()
			  + "  input_points_neg[:, 0] = locations_neg[0]" + System.lineSeparator()
			  + "  input_points_neg[:, 1] = locations_neg[1]" + System.lineSeparator()
			  + "  input_points = np.concatenate((input_points_pos.reshape(-1, 2), input_points_neg.reshape(-1, 2)), axis=0)" + System.lineSeparator()
			  + "  input_label = np.concatenate((input_labels_pos, input_labels_neg), axis=0)" + System.lineSeparator()
			  + "  mask_val, _, _ = predictor.predict(" + System.lineSeparator()
			  + "    point_coords=input_points," + System.lineSeparator()
			  + "    point_labels=input_label," + System.lineSeparator()
			  + "    multimask_output=False," + System.lineSeparator()
			  + "    box=None,)" + System.lineSeparator()
			  //+ "np.save('/temp/aa.npy', mask)" + System.lineSeparator()
				+ (this.isIJROIManager ? "mask_val[0, 1:, 1:] += mask_val[0, :-1, :-1]" : "") + System.lineSeparator()
			  + "  contours_x_val,contours_y_val = get_polygons_from_binary_mask(mask_val[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
			  + "  contours_x += contours_x_val" + System.lineSeparator()
			  + "  contours_y += contours_y_val" + System.lineSeparator()
			  + "task.update('all contours traced')" + System.lineSeparator()
			  + "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
			  + "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		code += "mask = 0" + System.lineSeparator();
		code += "shm_mask.close()" + System.lineSeparator();
		code += "shm_mask.unlink()" + System.lineSeparator();
		this.script = code;
	}

	@Override
	protected void processPointsWithSAM(int nPoints, int nNegPoints, boolean returnAll) {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_points_list = []" + System.lineSeparator()
				+ "input_neg_points_list = []" + System.lineSeparator();
		for (int n = 0; n < nPoints; n ++)
			code += "input_points_list.append([input_points[" + n + "][0], input_points[" + n + "][1]])" + System.lineSeparator();
		for (int n = 0; n < nNegPoints; n ++)
			code += "input_neg_points_list.append([input_neg_points[" + n + "][0], input_neg_points[" + n + "][1]])" + System.lineSeparator();
		code += ""
				+ "input_points = np.concatenate("
						+ "(np.array(input_points_list).reshape(" + nPoints + ", 2), np.array(input_neg_points_list).reshape(" + nNegPoints + ", 2))"
						+ ", axis=0)" + System.lineSeparator()
				+ "input_label = np.array([1] * " + (nPoints + nNegPoints) + ")" + System.lineSeparator()
				+ "input_label[" + nPoints + ":] -= 1" + System.lineSeparator()
				+ "mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=input_points," + System.lineSeparator()
				+ "    point_labels=input_label," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ "    box=None,)" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				// TODO remove + "import matplotlib.pyplot as plt" + System.lineSeparator()
				// TODO remove + "plt.imsave('/tmp/aa.jpg', mask[0], cmap='gray')" + System.lineSeparator()
				+ (this.isIJROIManager ? "mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				//+ (this.isIJROIManager ? "mask[0, :, 1:] += mask[0, :, :-1]" : "") + System.lineSeparator()
				//+ "np.save('/home/carlos/git/aa.npy', mask)" + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}

	@Override
	protected void processBoxWithSAM(boolean returnAll) {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_box = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator()
				+ "mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=None," + System.lineSeparator()
				+ "    point_labels=None," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ "    box=input_box,)" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				//+ "np.save('/home/carlos/git/mask.npy', mask)" + System.lineSeparator()
				+ (this.isIJROIManager ? "mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}
	
	private <T extends RealType<T> & NativeType<T>> void checkImageIsFine(RandomAccessibleInterval<T> inImg) {
		long[] dims = inImg.dimensionsAsLongArray();
		if ((dims.length != 3 && dims.length != 2) || (dims.length == 3 && dims[2] != 3 && dims[2] != 1)){
			throw new IllegalArgumentException("Currently EfficientViTSAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'xyc', first dimension width, second height and third channels.");
		}
	}
	
	/**
	 * 
	 * @return the list of EfficientViTSAM models that are supported
	 */
	public static List<String> getListOfSupportedEfficientViTSAM(){
		return MODELS_DICT.keySet().stream().collect(Collectors.toList());
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void adaptImageToModel(RandomAccessibleInterval<T> ogImg, RandomAccessibleInterval<T> targetImg) {
		if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int i = 0; i < 3; i ++) 
				RealTypeConverters.copyFromTo( ImgLib2Utils.convertViewToRGB(Views.hyperSlice(ogImg, 2, i), this.debugPrinter), 
						Views.hyperSlice(targetImg, 2, i) );
		} else if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 1) {
			debugPrinter.printText("CONVERTED 1 CHANNEL IMAGE INTO 3 TO BE FEEDED TO SAMJ");
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
	
	/**
	 * Tests during development
	 * @param args
	 * 	nothing
	 * @throws IOException nothing
	 * @throws RuntimeException nothing
	 * @throws InterruptedException nothing
	 */
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		img = Views.addDimension(img, 1, 2);
		try (EfficientViTSamJ sam = initializeSam(SamEnvManager.create(), img)) {
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}

	@Override
	protected void cellSAM(List<int[]> grid, boolean returnAll) {
		// TODO Auto-generated method stub
		
	}
}
