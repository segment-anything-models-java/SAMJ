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

import java.util.ArrayList;
import java.util.List;

import ai.nets.samj.install.Sam2EnvManager;
import ai.nets.samj.install.SamEnvManagerAbstract;

import java.io.File;
import java.io.IOException;

import org.apposed.appose.Appose;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

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
 * Class that provides the needed methods to run EfficientViTSAM models from Java
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class Sam2 extends AbstractSamJ {
	
	/**
	 * List of encodings that are cached to avoid recalculating
	 */
	List<String> savedEncodings = new ArrayList<String>();
	/**
	 * Map that associates the key for each of the existing EfficientViTSAM models to its complete name
	 */
	private static final List<String> MODELS_LIST = new ArrayList<String>();
	static {
		MODELS_LIST.add("tiny");
		MODELS_LIST.add("small");
		MODELS_LIST.add("base_plus");
		MODELS_LIST.add("large");
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
			+ "device = 'cpu'" + System.lineSeparator()
			+ ((!IS_APPLE_SILICON || true) ? "" // TODO Add a button so the user can decide whether to use accelerators or not (I tried enabling by default and some models might be out of memory)
					: "from torch.backends import mps" + System.lineSeparator()
					+ "if mps.is_built() and mps.is_available():" + System.lineSeparator()
					+ "  device = 'mps'" + System.lineSeparator())
			+ "from scipy.ndimage import binary_fill_holes" + System.lineSeparator()
			+ "from scipy.ndimage import label" + System.lineSeparator()
			+ "import sys" + System.lineSeparator()
			+ "import os" + System.lineSeparator()
			+ "import platform" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "from sam2.build_sam import build_sam2" + System.lineSeparator()
			+ "from sam2.sam2_image_predictor import SAM2ImagePredictor" + System.lineSeparator()
			+ "from sam2.utils.misc import variant_to_config_mapping" + System.lineSeparator()
			+ "model = build_sam2(variant_to_config_mapping['%s'],r'%s').to(device)" + System.lineSeparator()
			+ "predictor = SAM2ImagePredictor(model)" + System.lineSeparator()
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
	 * String containing the Python imports code after it has been formated with the correct 
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
	private Sam2(SamEnvManagerAbstract manager, String type) throws IOException, RuntimeException, InterruptedException {
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
	private Sam2(SamEnvManagerAbstract manager, String type,
	                      final DebugTextPrinter debugPrinter,
	                      final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {

		if (type.equals("base")) type = "base_plus";
		if (!MODELS_LIST.contains(type))
			throw new IllegalArgumentException("The model type should be one of the following: " 
							+ MODELS_LIST);
		this.debugPrinter = debugPrinter;
		this.isDebugging = printPythonCode;

		this.env = Appose.build(new File(manager.getModelEnv()));
		python = env.python();
		python.debug(debugPrinter::printText);
		IMPORTS_FORMATED = String.format(IMPORTS, type, manager.getModelWeigthPath());
		
		//printScript(IMPORTS_FORMATED + PythonMethods.RLE_METHOD + PythonMethods.TRACE_EDGES, "Edges tracing code");
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
	 * Create an EfficientViTSamJ instance that allows to use EfficientViTSAM on an image.
	 * This method encodes the image provided, so depending on the computer and on the model
	 * it might take some time
	 * 
	 * @param modelType
	 * 	EfficientViTSAM model type that we want to use, it can be "l0", "l1", "l2", "xl1" or "xl2"
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and ouptut to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @return an instance of {@link Sam2} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static Sam2
	initializeSam(String modelType, SamEnvManagerAbstract manager,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		Sam2 sam = null;
		try{
			sam = new Sam2(manager, modelType, debugPrinter, printPythonCode);
			sam.encodeCoords = new long[] {0, 0};
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
	 * @param modelType
	 * 	EfficientViTSAM model type that we want to use, it can be "l0", "l1", "l2", "xl1" or "xl2"
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @return an instance of {@link Sam2} that allows running EfficienTViTSAM on an image
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static Sam2 initializeSam(String modelType, SamEnvManagerAbstract manager) 
				throws IOException, RuntimeException, InterruptedException {
		Sam2 sam = null;
		try{
			sam = new Sam2(manager, modelType);
			sam.encodeCoords = new long[] {0, 0};
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
	 * The model used is the default one {@value Sam2EnvManager#DEFAULT_SAM2}
	 * 
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and ouptut to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @return an instance of {@link Sam2} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static Sam2 initializeSam(SamEnvManagerAbstract manager,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(Sam2EnvManager.DEFAULT_SAM2, manager, debugPrinter, printPythonCode);
	}

	/**
	 * Create an EfficientViTSamJ instance that allows to use EfficientViTSAM on an image.
	 * This method encodes the image provided, so depending on the computer and on the model
	 * it might take some time.
	 * 
	 * The model used is the default one {@value Sam2EnvManager#DEFAULT_SAM2}
	 * 
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @return an instance of {@link Sam2} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws IOException if any of the files to create a Python process is missing
	 * @throws RuntimeException if there is any error running the Python code
	 * @throws InterruptedException if the process is interrupted
	 */
	public static Sam2 initializeSam(SamEnvManagerAbstract manager) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(Sam2EnvManager.DEFAULT_SAM2, manager);
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
		//code += "np.save('/home/carlos/git/aa.npy', im)" + System.lineSeparator();
		script += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		script += ""
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
				+ (this.isIJROIManager ? "mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "contours_x, contours_y, rle_masks = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator()
				+ "task.outputs['rle'] = rle_masks" + System.lineSeparator();
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
				//+ "np.save('/home/carlos/git/mask.npy', mask)" + System.lineSeparator()
				+ (this.isIJROIManager ? "mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "contours_x, contours_y, rle_masks = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator()
				+ "task.outputs['rle'] = rle_masks" + System.lineSeparator();
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
	public static List<String> getListOfSupportedVariants(){
		return MODELS_LIST;
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void adaptImageToModel(RandomAccessibleInterval<T> ogImg, RandomAccessibleInterval<T> targetImg) {
		if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int i = 0; i < 3; i ++) {
				RealTypeConverters.copyFromTo( ImgLib2Utils.convertViewToRGB(Views.hyperSlice(ogImg, 2, i), this.debugPrinter), 
						Views.hyperSlice(targetImg, 2, i) );
			}
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
		try (Sam2 sam = initializeSam(Sam2EnvManager.create())) {
			sam.setImage(img);
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}

	@Override
	protected void cellSAM(List<int[]> grid, boolean returnAll) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String persistEncodingScript(String encodingName) {
		return "encodings_map['" + encodingName + "'] = predictor.features";
	}

	@Override
	public String selectEncodingScript(String encodingName) {
		return "predictor.features = encodings_map['" + encodingName + "']";		
	}

	@Override
	public String deleteEncodingScript(String encodingName) {
		return "del encodings_map['" + encodingName + "']";
	}

	@Override
	protected void processPromptsBatchWithSAM(SharedMemoryArray shmArr, boolean returnAll) {
		String code = ""
				+ "num_threads = 3" + System.lineSeparator()
				+ "finished_threads = []" + System.lineSeparator()
				+ "print(('threading' not in globals().keys()))" + System.lineSeparator()
				+ "if \"threading\" not in globals().keys():" + System.lineSeparator()
				+ "  import threading" + System.lineSeparator()
				+ "  globals()['threading'] = threading" + System.lineSeparator()
				+ "if \"ThreadPoolExecutor\" not in globals().keys():" + System.lineSeparator()
				+ "  from concurrent.futures import ThreadPoolExecutor, as_completed" + System.lineSeparator()
				+ "  globals()['ThreadPoolExecutor'] = ThreadPoolExecutor" + System.lineSeparator()
				+ "  globals()['as_completed'] = as_completed" + System.lineSeparator()
				+ "lock = threading.Lock()" + System.lineSeparator()
				+ "def respond_in_thread(task, args, inds, lock, finished_threads):" + System.lineSeparator()
				+ "  task._respond(ResponseType.UPDATE, args)" + System.lineSeparator()
				+ "  with lock:" + System.lineSeparator()
				+ "    finished_threads.extend(inds)" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "def cancel_unstarted_tasks(futures):" + System.lineSeparator()
				+ "    for future in futures:" + System.lineSeparator()
				+ "        if not future.running() and not future.done():" + System.lineSeparator()
				+ "            future.cancel()" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "num_features = 0" + System.lineSeparator();
		if (shmArr != null) {
			code += ""
					+ "shm_mask = shared_memory.SharedMemory(name='" + shmArr.getNameForPython() + "')" + System.lineSeparator()
					+ "mask_batch = np.ndarray(%s, buffer=shm_mask.buf, dtype='" 
					+ shmArr.getOriginalDataType() + "').reshape([";
			long size = 1;
			for (long l : shmArr.getOriginalShape()) {
				code += l + ",";
				size *= l;
			}
			code = String.format(code, size);
			code += "])" + System.lineSeparator();
			code += "labeled_array, num_features = label(mask_batch)" + System.lineSeparator();
		}
		code += ""
				+ "contours_x = []" + System.lineSeparator()
				+ "contours_y = []" + System.lineSeparator()
				+ "rle_masks = []" + System.lineSeparator()
				+ "ntot = num_features + len(point_prompts) + len(rect_prompts)" + System.lineSeparator()
				+ "args = {\"outputs\": {'n': str(ntot)}, \"message\": '" + AbstractSamJ.UPDATE_ID_N_CONTOURS + "'}" + System.lineSeparator()
				+ "task._respond(ResponseType.UPDATE, args)" + System.lineSeparator()
				// TODO right now is geetting the mask after each prompt
				// TODO test processing first every prompt and then getting the masks
				+ "with ThreadPoolExecutor(max_workers=num_threads) as executor:" + System.lineSeparator()
				+ "  futures = []" + System.lineSeparator()
				+ "  n_objects = 0" + System.lineSeparator()
				+ "  for n_feat in range(1, num_features + 1):" + System.lineSeparator()
				+ "    extracted_point_prompts = []" + System.lineSeparator()
				+ "    extracted_point_labels = []" + System.lineSeparator()
				+ "    inds = np.where(labeled_array == n_feat)" + System.lineSeparator()
				+ "    n_points = np.min([3, inds[0].shape[0]])" + System.lineSeparator()
				+ "    random_positions = np.random.choice(inds[0].shape[0], n_points, replace=False)" + System.lineSeparator()
				+ "    for pp in range(n_points):" + System.lineSeparator()
				+ "      extracted_point_prompts += [[inds[0][random_positions[pp]], inds[1][random_positions[pp]]]]" + System.lineSeparator()
				+ "      extracted_point_labels += [1]" + System.lineSeparator()
				+ "    mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "      point_coords=np.array(extracted_point_prompts)," + System.lineSeparator()
				+ "      point_labels=np.array(extracted_point_labels)," + System.lineSeparator()
				+ "      multimask_output=False," + System.lineSeparator()
				+ "      box=None,)" + System.lineSeparator()
				+ (this.isIJROIManager ? "    mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "    c_x, c_y, r_m = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "    contours_x += c_x" + System.lineSeparator()
				+ "    contours_y += c_y" + System.lineSeparator()
				+ "    rle_masks += r_m" + System.lineSeparator()
				+ "    args = {\"outputs\": {'temp_x': c_x, 'temp_y': c_y, 'temp_mask': r_m}, \"message\": '" + AbstractSamJ.UPDATE_ID_CONTOUR + "'}" + System.lineSeparator()
				+ "    it_list = list(range(n_objects, n_objects := n_objects + len(r_m)))" + System.lineSeparator()
				+ "    future = executor.submit(respond_in_thread, task, args, it_list, lock, finished_threads)" + System.lineSeparator()
				+ "    futures.append(future)" + System.lineSeparator()
				// TODO + "    task._respond(ResponseType.UPDATE, args)" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "  for p_prompt in point_prompts:" + System.lineSeparator()
				+ "    mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "      point_coords=np.array(p_prompt).reshape(1, 2)," + System.lineSeparator()
				+ "      point_labels=np.array([1])," + System.lineSeparator()
				+ "      multimask_output=False," + System.lineSeparator()
				+ "      box=None,)" + System.lineSeparator()
				+ (this.isIJROIManager ? "    mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "    c_x, c_y, r_m = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "    contours_x += c_x" + System.lineSeparator()
				+ "    contours_y += c_y" + System.lineSeparator()
				+ "    rle_masks += r_m" + System.lineSeparator()
				+ "    args = {\"outputs\": {'point': p_prompt, 'temp_x': c_x, 'temp_y': c_y, 'temp_mask': r_m}, \"message\": '" + AbstractSamJ.UPDATE_ID_CONTOUR + "'}" + System.lineSeparator()
				+ "    it_list = list(range(n_objects, n_objects := n_objects + len(r_m)))" + System.lineSeparator()
				+ "    future = executor.submit(respond_in_thread, task, args, it_list, lock, finished_threads)" + System.lineSeparator()
				+ "    futures.append(future)" + System.lineSeparator()
				// TODO + "    task._respond(ResponseType.UPDATE, args)" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "  for rect_prompt in rect_prompts:" + System.lineSeparator()
				+ "    input_box = np.array([[rect_prompt[0], rect_prompt[1]], [rect_prompt[2], rect_prompt[3]]])" + System.lineSeparator()
				+ "    mask, _, _ = predictor.predict(" + System.lineSeparator()
				+ "      point_coords=None," + System.lineSeparator()
				+ "      point_labels=np.array([1])," + System.lineSeparator()
				+ "      multimask_output=False," + System.lineSeparator()
				+ "      box=input_box,)" + System.lineSeparator()
				+ (this.isIJROIManager ? "    mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "    c_x, c_y, r_m = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "    contours_x += c_x" + System.lineSeparator()
				+ "    contours_y += c_y" + System.lineSeparator()
				+ "    rle_masks += r_m" + System.lineSeparator()
				+ "    args = {\"outputs\": {'rect': rect_prompt, 'temp_x': c_x, 'temp_y': c_y, 'temp_mask': r_m}, \"message\": '" + AbstractSamJ.UPDATE_ID_CONTOUR + "'}" + System.lineSeparator()
				+ "    it_list = list(range(n_objects, n_objects := n_objects + len(r_m)))" + System.lineSeparator()
				+ "    future = executor.submit(respond_in_thread, task, args, it_list, lock, finished_threads)" + System.lineSeparator()
				+ "    futures.append(future)" + System.lineSeparator()
				// TODO + "    task._respond(ResponseType.UPDATE, args)" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "  finished_threads.sort()" + System.lineSeparator()
				+ "  cancel_unstarted_tasks(futures)" + System.lineSeparator()
				+ "  for i, future in enumerate(futures[::-1]):" + System.lineSeparator()
				+ "    if not future.cancelled():" + System.lineSeparator()
				+ "      future.result()" + System.lineSeparator()
				+ "  for i in finished_threads[::-1]:" + System.lineSeparator()
				+ "      contours_x.pop(i)" + System.lineSeparator()
				+ "      contours_y.pop(i)" + System.lineSeparator()
				+ "      rle_masks.pop(i)" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator()
				+ "task.outputs['rle'] = rle_masks" + System.lineSeparator();
		code += "mask_batch = None" + System.lineSeparator();
		if (shmArr != null) {
			code += "shm_mask.close()" + System.lineSeparator();
			code += "shm_mask.unlink()" + System.lineSeparator();
		}
		this.script = code;
	}
}
