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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;
import org.apposed.appose.TaskException;

import ai.nets.samj.install.EfficientTamEnvManager;
import ai.nets.samj.install.SamEnvManagerAbstract;

import java.io.File;
import java.io.IOException;

import io.bioimage.modelrunner.tensor.Utils;
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
public class EfficientTamJ extends AbstractSamJ {
	
	/**
	 * List of encodings that are cached to avoid recalculating
	 */
	List<String> savedEncodings = new ArrayList<String>();
	
	private static final HashMap<String, String> MODEL_TYPE_MAP;
	static {
		MODEL_TYPE_MAP = new HashMap<String, String>();
		MODEL_TYPE_MAP.put("tiny", "ti");
		MODEL_TYPE_MAP.put("small", "s");
	}
	/**
	 * Map that associates the key for each of the existing EfficientViTSAM models to its complete name
	 */
	private static final List<String> MODELS_LIST = new ArrayList<>(MODEL_TYPE_MAP.keySet());
	/**
	 * String to find the config file used to load EfficientTAM
	 */
	private static final String CONFIG_STR = "configs/efficienttam/efficienttam_%s.yaml";
	private static final String PRE_IMPORTS = ""
			+ "import sys" + System.lineSeparator()
			+ "sys.path.append(r'%s')" + System.lineSeparator();
	/**
	 * All the Python imports and configurations needed to start using EfficientViTSAM.
	 */
	private static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "from skimage import measure" + System.lineSeparator()
			+ "measure.label(np.ones((10, 10)), connectivity=1)" + System.lineSeparator()
			+ "import torch" + System.lineSeparator()
			+ "device = 'cpu'" + System.lineSeparator()
			+ "if torch.cuda.is_available():" + System.lineSeparator()
			+ "  device = 'gpu'" + System.lineSeparator()
			+ ((!IS_APPLE_SILICON || true) ? "" // TODO Add a button so the user can decide whether to use accelerators or not (I tried enabling by default and some models might be out of memory)
					: "from torch.backends import mps" + System.lineSeparator()
					+ "if mps.is_built() and mps.is_available():" + System.lineSeparator()
					+ "  device = 'mps'" + System.lineSeparator())
			+ "from scipy.ndimage import binary_fill_holes" + System.lineSeparator()
			+ "from scipy.ndimage import label" + System.lineSeparator()
			+ "import os" + System.lineSeparator()
			+ "import platform" + System.lineSeparator()
			+ "import torch._dynamo" + System.lineSeparator() // TODO remove
		    + "torch._dynamo.config.suppress_errors = True" + System.lineSeparator() // TODO remove
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "from efficient_track_anything.build_efficienttam import build_efficienttam_video_predictor" + System.lineSeparator()
			+ "predictor = build_efficienttam_video_predictor(r'%s',r'%s', device=device)" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "encodings_map = {}" + System.lineSeparator()
			+ "task.export(encodings_map=encodings_map)" + System.lineSeparator()
			+ "task.export(measure=measure)" + System.lineSeparator()
			+ "task.export(shared_memory=shared_memory)" + System.lineSeparator()
			+ "task.export(torch=torch)" + System.lineSeparator()
			+ "task.export(label=label)" + System.lineSeparator()
			+ "task.export(binary_fill_holes=binary_fill_holes)" + System.lineSeparator()
			+ "task.export(predictor=predictor)" + System.lineSeparator()
			+ "task.export(device=device)" + System.lineSeparator()
			+ "import torch.nn.functional as F" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "def load_video_frames_from_xycz_array(" + System.lineSeparator()
			+ "    video_xycz: np.ndarray," + System.lineSeparator()
			+ "    image_size: int," + System.lineSeparator()
			+ "    offload_video_to_cpu: bool," + System.lineSeparator()
			+ "    img_mean=(0.485, 0.456, 0.406)," + System.lineSeparator()
			+ "    img_std=(0.229, 0.224, 0.225)," + System.lineSeparator()
			+ "    compute_device=torch.device(\"cuda\")," + System.lineSeparator()
			+ "):" + System.lineSeparator()
			+ "    \"\"\"" + System.lineSeparator()
			+ "    Convert a uint8 numpy array of shape (x, y, 3, z) into the same output format as" + System.lineSeparator()
			+ "    EfficientTAM's load_video_frames_from_jpg_images(...), using a batched torch path." + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "    Parameters" + System.lineSeparator()
			+ "    ----------" + System.lineSeparator()
			+ "    video_xycz : np.ndarray" + System.lineSeparator()
			+ "        Video array with shape (x, y, 3, z), dtype uint8." + System.lineSeparator()
			+ "    image_size : int" + System.lineSeparator()
			+ "        Spatial size expected by EfficientTAM." + System.lineSeparator()
			+ "    offload_video_to_cpu : bool" + System.lineSeparator()
			+ "        If False, move the output tensor to compute_device." + System.lineSeparator()
			+ "    img_mean : tuple" + System.lineSeparator()
			+ "        Per-channel mean used by EfficientTAM." + System.lineSeparator()
			+ "    img_std : tuple" + System.lineSeparator()
			+ "        Per-channel std used by EfficientTAM." + System.lineSeparator()
			+ "    compute_device : torch.device" + System.lineSeparator()
			+ "        Target device when offload_video_to_cpu is False." + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "    Returns" + System.lineSeparator()
			+ "    -------" + System.lineSeparator()
			+ "    images : torch.Tensor" + System.lineSeparator()
			+ "        Shape (z, 3, image_size, image_size), dtype float32, normalized." + System.lineSeparator()
			+ "    video_height : int" + System.lineSeparator()
			+ "        Original frame height (y)." + System.lineSeparator()
			+ "    video_width : int" + System.lineSeparator()
			+ "        Original frame width (x)." + System.lineSeparator()
			+ "    \"\"\"" + System.lineSeparator()
			+ "    if not isinstance(video_xycz, np.ndarray):" + System.lineSeparator()
			+ "        raise TypeError(\"video_xycz must be a numpy.ndarray\")" + System.lineSeparator()
			+ "    if video_xycz.dtype != np.uint8:" + System.lineSeparator()
			+ "        raise TypeError(f\"Expected np.uint8, got {video_xycz.dtype}\")" + System.lineSeparator()
			+ "    if video_xycz.ndim != 4:" + System.lineSeparator()
			+ "        raise ValueError(f\"Expected 4D array (x, y, 3, z), got shape {video_xycz.shape}\")" + System.lineSeparator()
			+ "    if video_xycz.shape[2] != 3:" + System.lineSeparator()
			+ "        raise ValueError(f\"Expected 3 channels on axis 2, got shape {video_xycz.shape}\")" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "    video_width = video_xycz.shape[0]" + System.lineSeparator()
			+ "    video_height = video_xycz.shape[1]" + System.lineSeparator()
			+ "    # (x, y, 3, z) -> (z, 3, y, x)" + System.lineSeparator()
			+ "    images = torch.from_numpy(video_xycz).permute(3, 2, 1, 0).contiguous()" + System.lineSeparator()
			+ "    images = images.to(torch.float32).div_(255.0)" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "    if not offload_video_to_cpu:" + System.lineSeparator()
			+ "        images = images.to(compute_device, non_blocking=True)" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "    images = F.interpolate(" + System.lineSeparator()
			+ "        images," + System.lineSeparator()
			+ "        size=(image_size, image_size)," + System.lineSeparator()
			+ "        mode=\"bilinear\"," + System.lineSeparator()
			+ "        align_corners=False," + System.lineSeparator()
			+ "        antialias=True," + System.lineSeparator()
			+ "    )" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "    img_mean = torch.tensor(img_mean, dtype=torch.float32, device=images.device)[None, :, None, None]" + System.lineSeparator()
			+ "    img_std = torch.tensor(img_std, dtype=torch.float32, device=images.device)[None, :, None, None]" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "    images.sub_(img_mean).div_(img_std)" + System.lineSeparator()
			+ "    return images, video_height, video_width" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "from collections import OrderedDict" + System.lineSeparator()
			+ "@torch.inference_mode()" + System.lineSeparator()
			+ "def init_state(" + System.lineSeparator()
			+ "    npy_video," + System.lineSeparator()
			+ "    offload_video_to_cpu=False," + System.lineSeparator()
			+ "    offload_state_to_cpu=False," + System.lineSeparator()
			+ "    frame_idx=0," + System.lineSeparator()
			+ "):" + System.lineSeparator()
			+ "    \"\"\"Initialize an inference state.\"\"\"" + System.lineSeparator()
			+ "    compute_device = predictor.device  # device of the model" + System.lineSeparator()
			+ "    images, video_height, video_width = load_video_frames_from_xycz_array(" + System.lineSeparator()
			+ "        video_xycz=npy_video," + System.lineSeparator()
			+ "        image_size=predictor.image_size," + System.lineSeparator()
			+ "        offload_video_to_cpu=offload_video_to_cpu," + System.lineSeparator()
			+ "        compute_device=compute_device," + System.lineSeparator()
			+ "    )" + System.lineSeparator()
			+ "    inference_state = {}" + System.lineSeparator()
			+ "    inference_state[\"images\"] = images" + System.lineSeparator()
			+ "    inference_state[\"num_frames\"] = len(images)" + System.lineSeparator()
			+ "    # whether to offload the video frames to CPU memory" + System.lineSeparator()
			+ "    # turning on this option saves the GPU memory with only a very small overhead" + System.lineSeparator()
			+ "    inference_state[\"offload_video_to_cpu\"] = offload_video_to_cpu" + System.lineSeparator()
			+ "    # whether to offload the inference state to CPU memory" + System.lineSeparator()
			+ "    # turning on this option saves the GPU memory at the cost of a lower tracking fps" + System.lineSeparator()
			+ "    # (e.g. in a test case of 768x768 model, fps dropped from 27 to 24 when tracking one object" + System.lineSeparator()
			+ "    # and from 24 to 21 when tracking two objects)" + System.lineSeparator()
			+ "    inference_state[\"offload_state_to_cpu\"] = offload_state_to_cpu" + System.lineSeparator()
			+ "    # the original video height and width, used for resizing final output scores" + System.lineSeparator()
			+ "    inference_state[\"video_height\"] = video_height" + System.lineSeparator()
			+ "    inference_state[\"video_width\"] = video_width" + System.lineSeparator()
			+ "    inference_state[\"device\"] = compute_device" + System.lineSeparator()
			+ "    if offload_state_to_cpu:" + System.lineSeparator()
			+ "        inference_state[\"storage_device\"] = torch.device(\"cpu\")" + System.lineSeparator()
			+ "    else:" + System.lineSeparator()
			+ "        inference_state[\"storage_device\"] = compute_device" + System.lineSeparator()
			+ "    # inputs on each frame" + System.lineSeparator()
			+ "    inference_state[\"point_inputs_per_obj\"] = {}" + System.lineSeparator()
			+ "    inference_state[\"mask_inputs_per_obj\"] = {}" + System.lineSeparator()
			+ "    # visual features on a small number of recently visited frames for quick interactions" + System.lineSeparator()
			+ "    inference_state[\"cached_features\"] = {}" + System.lineSeparator()
			+ "    # values that don't change across frames (so we only need to hold one copy of them)" + System.lineSeparator()
			+ "    inference_state[\"constants\"] = {}" + System.lineSeparator()
			+ "    # mapping between client-side object id and model-side object index" + System.lineSeparator()
			+ "    inference_state[\"obj_id_to_idx\"] = OrderedDict()" + System.lineSeparator()
			+ "    inference_state[\"obj_idx_to_id\"] = OrderedDict()" + System.lineSeparator()
			+ "    inference_state[\"obj_ids\"] = []" + System.lineSeparator()
			+ "    # Slice (view) of each object tracking results, sharing the same memory with \"output_dict\"" + System.lineSeparator()
			+ "    inference_state[\"output_dict_per_obj\"] = {}" + System.lineSeparator()
			+ "    # A temporary storage to hold new outputs when user interact with a frame" + System.lineSeparator()
			+ "    # to add clicks or mask (it's merged into \"output_dict\" before propagation starts)" + System.lineSeparator()
			+ "    inference_state[\"temp_output_dict_per_obj\"] = {}" + System.lineSeparator()
			+ "    # Frames that already holds consolidated outputs from click or mask inputs" + System.lineSeparator()
			+ "    # (we directly use their consolidated outputs during tracking)" + System.lineSeparator()
			+ "    # metadata for each tracking frame (e.g. which direction it's tracked)" + System.lineSeparator()
			+ "    inference_state[\"frames_tracked_per_obj\"] = {}" + System.lineSeparator()
			+ "    # Warm up the visual backbone and cache the image feature on frame 0" + System.lineSeparator()
			+ "    predictor._get_image_feature(inference_state, frame_idx=frame_idx, batch_size=1)" + System.lineSeparator()
			+ "    return inference_state" + System.lineSeparator()
			+ ""  + System.lineSeparator()
			+ ""  + System.lineSeparator()
			+ ""  + System.lineSeparator()
			+ ""
			+ "task.export(init_state=init_state)" + System.lineSeparator()
			+ "task.export(OrderedDict=OrderedDict)" + System.lineSeparator();
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
	 * @throws InterruptedException if the process is interrupted
	 * @throws BuildException if there is an error building the python environment
	 * @throws TaskException if there is any error running the Appose task
	 */
	private EfficientTamJ(SamEnvManagerAbstract manager, String type) throws InterruptedException, BuildException, TaskException {
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
	 * @throws InterruptedException if the process is interrupted
	 * @throws BuildException if there is an error building the python environment
	 * @throws TaskException if there is any error running the Appose task
	 * 
	 */
	private EfficientTamJ(SamEnvManagerAbstract manager, String type,
	                      final DebugTextPrinter debugPrinter,
	                      final boolean printPythonCode) throws InterruptedException, BuildException, TaskException {

		if (!MODELS_LIST.contains(type))
			throw new IllegalArgumentException("The model type should be one of the following: " 
							+ MODELS_LIST);
		this.debugPrinter = debugPrinter;
		this.isDebugging = printPythonCode;

		this.env = Appose.pixi().wrap(new File(manager.getModelEnv()));
		python = env.python();
		python.debug(debugPrinter::printText);
		String libPath = manager.getModelEnv() + File.separator + EfficientTamEnvManager.EFFTAM_NAME;
		String appendLibPath = String.format(PRE_IMPORTS, libPath);

		
		//python.init("import numpy as np" + System.lineSeparator() + appendLibPath);
		python.init("import numpy as np");
		IMPORTS_FORMATED = String.format(IMPORTS,
				String.format(CONFIG_STR, MODEL_TYPE_MAP.get(type)),
				manager.getModelWeigthPath());
		
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
	 * @return an instance of {@link EfficientTamJ} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws InterruptedException if the process is interrupted
	 * @throws BuildException if there is an error building the python environment
	 * @throws TaskException if there is any error running the Appose task
	 */
	public static EfficientTamJ
	initializeSam(String modelType, SamEnvManagerAbstract manager,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws InterruptedException,  TaskException, BuildException {
		EfficientTamJ sam = null;
		try{
			sam = new EfficientTamJ(manager, modelType, debugPrinter, printPythonCode);
			sam.encodeCoords = new long[] {0, 0};
		} catch (InterruptedException | BuildException | TaskException ex) {
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
	 * @return an instance of {@link EfficientTamJ} that allows running EfficienTViTSAM on an image
	 * @throws InterruptedException if the process is interrupted
	 * @throws BuildException if there is an error building the python environment
	 * @throws TaskException if there is any error running the Appose task
	 */
	public static EfficientTamJ initializeSam(String modelType, SamEnvManagerAbstract manager) 
				throws InterruptedException, BuildException, TaskException {
		EfficientTamJ sam = null;
		try{
			sam = new EfficientTamJ(manager, modelType);
			sam.encodeCoords = new long[] {0, 0};
		} catch (InterruptedException | BuildException | TaskException ex) {
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
	 * The model used is the default one {@value EfficientTamEnvManager#DEFAULT}
	 * 
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @param debugPrinter
	 * 	functional interface to redirect the Python process Appose text log and ouptut to be redirected anywhere
	 * @param printPythonCode
	 * 	whether to print the Python code that is going to be executed on the Python process or not
	 * @return an instance of {@link EfficientTamJ} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws InterruptedException if the process is interrupted
	 * @throws BuildException if there is an error building the python environment
	 * @throws TaskException if there is any error running the Appose task
	 */
	public static EfficientTamJ initializeSam(SamEnvManagerAbstract manager,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws InterruptedException, TaskException, BuildException {
		return initializeSam(EfficientTamEnvManager.DEFAULT, manager, debugPrinter, printPythonCode);
	}

	/**
	 * Create an EfficientViTSamJ instance that allows to use EfficientViTSAM on an image.
	 * This method encodes the image provided, so depending on the computer and on the model
	 * it might take some time.
	 * 
	 * The model used is the default one {@value EfficientTamEnvManager#DEFAULT}
	 * 
	 * @param manager
	 * 	environment manager that contians all the paths to the environments needed, Python executables and model weights
	 * @return an instance of {@link EfficientTamJ} that allows running EfficienTViTSAM on an image
	 * 	with the image already encoded
	 * @throws InterruptedException if the process is interrupted
	 * @throws BuildException if there is an error building the python environment
	 * @throws TaskException if there is any error running the Appose task
	 */
	public static EfficientTamJ initializeSam(SamEnvManagerAbstract manager) throws InterruptedException, BuildException, TaskException {
		return initializeSam(EfficientTamEnvManager.DEFAULT, manager);
	}

	@Override
	// TODO test extensively
	protected <T extends RealType<T> & NativeType<T>> void setImageOfInterest(RandomAccessibleInterval<T> rai) {
		checkImageIsFine(rai);
		long[] dims = rai.dimensionsAsLongArray();
		if (dims.length == 2) {
			rai = Views.interval( Views.expandMirrorDouble(Views.addDimension(rai, 0, 0), new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, dims[0] - 1, dims[1] - 1, 2}) );
			rai = Views.addDimension(rai, 0, 0);
			isSlice = true;
		} else if (dims.length == 3 && dims[2] == 1) {
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, dims[0] - 1, dims[1] - 1, 2}) );
			rai = Views.addDimension(rai, 0, 0);
			isSlice = true;
		} else if (dims.length == 3 && dims[2] == 3 && this.nFrames == 1 && this.nSlices == 1) {
			rai = Views.addDimension(rai, 0, 0);
			isSlice = true;
		} else if (dims.length == 3 && dims[2] > 1 && this.nFrames != 1) {
			rai = Views.addDimension(rai, 0, 0);
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, dims[2] - 1, 2}) );
			rai = Utils.rearangeAxes(rai, new int[] {0, 1, 3, 2});
			isSlice = false;
		} else if (dims.length == 3 && dims[2] > 1 && this.nSlices != 1) {
			rai = Views.addDimension(rai, 0, 0);
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, dims[2] - 1, 2}) );
			rai = Utils.rearangeAxes(rai, new int[] {0, 1, 3, 2});
			isSlice = true;
		} else if (dims.length == 3 && this.nSlices != 1 & this.nFrames != 1) {
			throw new IllegalArgumentException(String.format("Invalid array shape configuration, shape %s, slices: %s, frames: %s", Arrays.toString(dims), nSlices, nFrames));
		} else if (dims.length == 4 && dims[2] == 1 && dims[3] == 1) {
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2, 0}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, 2, dims[3] - 1}) );
			isSlice = true;
		} else if (dims.length == 4 && dims[2] == 1 && dims[3] != 1 && this.nFrames > 1 && this.nSlices == 1) {
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2, 0}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, 2, dims[3] - 1}) );
			isSlice = false;
		} else if (dims.length == 4 && dims[2] == 1 && dims[3] != 1 && this.nFrames == 1 && this.nSlices > 1) {
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2, 0}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, 2, dims[3] - 1}) );
			isSlice = true;
		} else if (dims.length == 4 && dims[2] == 3 && dims[3] == 1 && this.nFrames == 1 && this.nSlices == 1) {
			isSlice = true;
		} else if (dims.length == 4 && dims[2] > 1 && dims[3] == 1 && this.nFrames > 1 && this.nSlices == 1) {
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, dims[2] - 1, 2}) );
			rai = Utils.rearangeAxes(rai, new int[] {0, 1, 3, 2});
			isSlice = false;
		} else if (dims.length == 4 && dims[2] > 1 && dims[3] == 1 && this.nFrames == 1 && this.nSlices > 1) {
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, dims[2] - 1, 2}) );
			rai = Utils.rearangeAxes(rai, new int[] {0, 1, 3, 2});
			isSlice = true;
		} else if (dims.length == 4 && dims[2] > 1 && dims[3] > 1 && this.nFrames > 1 && this.nSlices > 1) {
			rai = Views.interval( rai, Intervals.createMinMax(new long[] {0, 0, 0, this.frameIdx, dims[0] - 1, dims[1] - 1, dims[2] - 1, this.frameIdx}));
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, dims[2] - 1, 2}) );
			rai = Utils.rearangeAxes(rai, new int[] {0, 1, 3, 2});
			isSlice = true;
		} else if (dims.length == 4 && dims[2] > 1 && dims[3] > 1 && (this.nFrames == 1 && this.nSlices == 1)) {
			throw new IllegalArgumentException("Channel dimension should always follow WH (WHC)");
		} else if (dims.length == 5 && dims[2] == 3 && dims[3] == 1 && dims[4] == 1 && this.nFrames == 1 && this.nSlices == 1) {
			isSlice = true;
		} else if (dims.length == 5 && dims[2] == 1 && dims[3] == 1 && dims[4] == 1 && this.nFrames == 1 && this.nSlices == 1) {
			rai = Views.hyperSlice(rai, 4, 0);
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2, 0}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, 2, dims[3] - 1}) );
			isSlice = true;
		} else if (dims.length == 5 && dims[2] > 1 && dims[3] == 1 && dims[4] == 1 && this.nFrames > 1 && this.nSlices == 1) {
			rai = Views.hyperSlice(rai, 4, 0);
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, dims[2] - 1, 2}) );
			rai = Utils.rearangeAxes(rai, new int[] {0, 1, 3, 2});
			isSlice = false;
		} else if (dims.length == 5 && dims[2] > 1 && dims[3] == 1 && dims[4] == 1 && this.nFrames == 1 && this.nSlices > 1) {
			rai = Views.hyperSlice(rai, 4, 0);
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, dims[2] - 1, 2}) );
			rai = Utils.rearangeAxes(rai, new int[] {0, 1, 3, 2});
			isSlice = true;
		} else if (dims.length == 5 && dims[2] == 1 && dims[3] > 1 && this.nSlices > 1) {
			rai = Views.hyperSlice(rai, 4, this.frameIdx);
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2, 0}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, 2, dims[3] - 1}) );
			isSlice = true;
		} else if (dims.length == 5 && dims[2] == 3 && dims[3] > 1 && this.nSlices > 1) {
			rai = Views.hyperSlice(rai, 4, this.frameIdx);
			isSlice = true;
			isSlice = true;
		} else if (dims.length == 5 && dims[2] == 3 && dims[3] == 1 && dims[4] > 1 && this.nSlices == 1 && this.nFrames > 1) {
			rai = Views.hyperSlice(rai, 3, this.sliceIdx);
			isSlice = false;
		} else if (dims.length == 5 && dims[2] == 1 && dims[3] == 1 && dims[4] > 1 && this.nSlices == 1 && this.nFrames > 1) {
			rai = Views.hyperSlice(rai, 3, this.sliceIdx);
			rai = Views.interval( Views.expandMirrorDouble(rai, new long[] {0, 0, 2, 0}), 
					Intervals.createMinMax(new long[] {0, 0, 0, 0, dims[0] - 1, dims[1] - 1, 2, dims[3] - 1}) );
			isSlice = false;
		} else {
			throw new IllegalArgumentException(String.format("Invalid array shape configuration, shape %s, slices: %s, frames: %s", Arrays.toString(dims), nSlices, nFrames));
		}
		
		dims = rai.dimensionsAsLongArray();
		if (dims.length != 4 || dims[2] != 3) {
		    throw new IllegalStateException("Expected XYCT with C=3, got " + Arrays.toString(dims));
		}
		this.img = rai;
		this.targetDims = img.dimensionsAsLongArray();
		
	}

	@Override
	protected void createEncodeImageScript() {
		script = "";
		script += "im_shm = shared_memory.SharedMemory(name='"
				+ shma.getNameForPython() + "', size=" + shma.getSize() 
				+ ")" + System.lineSeparator();
		int size = (int) (targetDims[2] * targetDims[3]);
		for (int i = 0; i < 2; i ++) {
			size *= Math.ceil(targetDims[i] / (double) scale);
			}
		script += "im = np.ndarray(" + size + ", dtype='" + CommonUtils.getDataTypeFromRAI(Cast.unchecked(shma.getSharedRAI()))
				+ "', buffer=im_shm.buf).reshape([";
		for (int i = 0; i < targetDims.length - 2; i ++)
			script += (int) Math.ceil(targetDims[i] / (double) scale) + ", ";
		script += targetDims[2] + ", " + targetDims[3];
		script += "])" + System.lineSeparator();
		//code += "np.save('/home/carlos/git/aa.npy', im)" + System.lineSeparator();
		script += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		script += ""
			+ "state = init_state(im," + System.lineSeparator()
		+ "    offload_video_to_cpu=False," + System.lineSeparator()
		+ "    offload_state_to_cpu=False,"
		+ "    frame_idx=" + frameIdx + ")" + System.lineSeparator()
		+ "task.export(state=state)" + System.lineSeparator();
	}

	@Override
	protected <T extends RealType<T> & NativeType<T>> void createSHMArray(RandomAccessibleInterval<T> imShared) {
		RandomAccessibleInterval<T> imageToBeSent = ImgLib2Utils.reescaleIfNeeded(imShared);
		long[] dims = imageToBeSent.dimensionsAsLongArray();
		shma = SharedMemoryArray.create(new long[] {dims[0], dims[1], dims[2], dims[3]}, new UnsignedByteType(), false, false);
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
				+ "task.outputs['frame_ids'] = len(contours_x) * [" + frameIdx + "]" + System.lineSeparator()
				+ "task.outputs['slice_ids'] = len(contours_x) * [" + frameIdx + "]" + System.lineSeparator()
				+ "task.outputs['rle'] = rle_masks" + System.lineSeparator();
		this.script = code;
	}

	@Override
	protected void processBoxWithSAM(boolean returnAll) {

		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_box = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator()
				+ "frame_idx, obj_ids, mask = predictor.add_new_points_or_box(" + System.lineSeparator()
				+ "    state," + System.lineSeparator()
				+ "    frame_idx=" + (this.isSlice ? sliceIdx : frameIdx) + "," + System.lineSeparator()
				+ "    obj_id=1," + System.lineSeparator()
				+ "    points=None," + System.lineSeparator()
				+ "    labels=None," + System.lineSeparator()
				+ "    clear_old_points=True," + System.lineSeparator()
				+ "    normalize_coords=True," + System.lineSeparator()
				+ "    box=input_box,)" + System.lineSeparator()
				//+ "np.save('/home/carlos/git/mask.npy', mask)" + System.lineSeparator()
				+ "mask = (mask[0] > 0.0).cpu().numpy()" + System.lineSeparator()
				+ (this.isIJROIManager ? "mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "contours_x, contours_y, rle_masks = get_polygons_from_binary_mask(mask[0], only_biggest=" + (!returnAll ? "True" : "False") + ")" + System.lineSeparator()
				+ "predictor.remove_object(state, 1, strict=False, need_output=True)" + System.lineSeparator()
				+ "task.export(state=state)" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator()
				+ "task.outputs['frame_ids'] = len(contours_x) * [" + frameIdx + "]" + System.lineSeparator()
				+ "task.outputs['slice_ids'] = len(contours_x) * [" + sliceIdx + "]" + System.lineSeparator()
				+ "task.outputs['rle'] = rle_masks" + System.lineSeparator();
		this.script = code;
	}

	@Override
	protected void processBoxWithSAMAndPropagate() {
		
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "seed_frame_idx = " + (this.isSlice ? sliceIdx : frameIdx) + System.lineSeparator()
				+ "end_frame_idx = " + (this.isSlice ? nSlices : nFrames) + System.lineSeparator()
				+ "input_box = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator()
				+ "frame_idx, obj_ids, mask = predictor.add_new_points_or_box(" + System.lineSeparator()
				+ "    state," + System.lineSeparator()
				+ "    frame_idx=seed_frame_idx," + System.lineSeparator()
				+ "    obj_id=1," + System.lineSeparator()
				+ "    points=None," + System.lineSeparator()
				+ "    labels=None," + System.lineSeparator()
				+ "    clear_old_points=True," + System.lineSeparator()
				+ "    normalize_coords=True," + System.lineSeparator()
				+ "    box=input_box,)" + System.lineSeparator()
				//+ "np.save('/home/carlos/git/mask.npy', mask)" + System.lineSeparator()
				+ "mask = (mask[0] > 0.0).cpu().numpy()" + System.lineSeparator()
				+ (this.isIJROIManager ? "mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "contours_x, contours_y, rle_masks = get_polygons_from_binary_mask(mask[0], only_biggest=True)" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "seen_frames = set()" + System.lineSeparator()
				+ "seen_frames.add(seed_frame_idx)" + System.lineSeparator()
				+ "frame_ids = [" + frameIdx + "]" + System.lineSeparator()
				+ "slice_ids = [" + sliceIdx + "]" + System.lineSeparator()
				+ "all_x = contours_x" + System.lineSeparator()
				+ "all_y = contours_y" + System.lineSeparator()
				+ "all_rle = rle_masks" + System.lineSeparator()
				+ "if seed_frame_idx < end_frame_idx:" + System.lineSeparator()
				+ "    max_frame_num_to_track = end_frame_idx - seed_frame_idx + 1" + System.lineSeparator()
				+ "    for frame_idx, obj_ids, mask in predictor.propagate_in_video(" + System.lineSeparator()
				+ "        state," + System.lineSeparator()
				+ "        start_frame_idx=seed_frame_idx," + System.lineSeparator()
				+ "        max_frame_num_to_track=max_frame_num_to_track," + System.lineSeparator()
				+ "        reverse=False," + System.lineSeparator()
				+ "    ):" + System.lineSeparator()
				+ "        if frame_idx in seen_frames:" + System.lineSeparator()
				+ "            continue" + System.lineSeparator()
				+ "        mask = (mask[0] > 0.0).cpu().numpy()" + System.lineSeparator()
				+ (this.isIJROIManager ? "        mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "        contours_x, contours_y, rle_masks = get_polygons_from_binary_mask(mask[0], only_biggest=True)" + System.lineSeparator()
				+ "        all_x.extend(contours_x)" + System.lineSeparator()
				+ "        all_y.extend(contours_y)" + System.lineSeparator()
				+ "        all_rle.extend(rle_masks)" + System.lineSeparator()
				+ "        frame_ids.append(" + (this.isSlice ? this.frameIdx : "frame_idx") + ")" + System.lineSeparator()
				+ "        slice_ids.append(" + (this.isSlice ? "frame_idx" : this.sliceIdx) + ")" + System.lineSeparator()
				+ "        seen_frames.add(frame_idx)" + System.lineSeparator()
				+ "" + System.lineSeparator()
				+ "if seed_frame_idx > 0:" + System.lineSeparator()
				+ "    max_frame_num_to_track = seed_frame_idx - 0 + 1" + System.lineSeparator()
				+ "    for frame_idx, obj_ids, mask in predictor.propagate_in_video(" + System.lineSeparator()
				+ "        state," + System.lineSeparator()
				+ "        start_frame_idx=seed_frame_idx," + System.lineSeparator()
				+ "        max_frame_num_to_track=max_frame_num_to_track," + System.lineSeparator()
				+ "        reverse=True," + System.lineSeparator()
				+ "    ):" + System.lineSeparator()
				+ "        if frame_idx in seen_frames:" + System.lineSeparator()
				+ "            continue" + System.lineSeparator()
				+ "        mask = (mask[0] > 0.0).cpu().numpy()" + System.lineSeparator()
				+ (this.isIJROIManager ? "        mask[0, 1:, 1:] += mask[0, :-1, :-1]" : "") + System.lineSeparator()
				+ "        contours_x, contours_y, rle_masks = get_polygons_from_binary_mask(mask[0], only_biggest=True)" + System.lineSeparator()
				+ "        all_x.extend(contours_x)" + System.lineSeparator()
				+ "        all_y.extend(contours_y)" + System.lineSeparator()
				+ "        all_rle.extend(rle_masks)" + System.lineSeparator()
				+ "        frame_ids.append(" + (this.isSlice ? this.frameIdx : "frame_idx") + ")" + System.lineSeparator()
				+ "        slice_ids.append(" + (this.isSlice ? "frame_idx" : this.sliceIdx) + ")" + System.lineSeparator()
				+ "        seen_frames.add(frame_idx)" + System.lineSeparator()
				+ "predictor.remove_object(state, 1, strict=False, need_output=False)" + System.lineSeparator()
				+ "task.export(state=state)" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = all_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = all_y" + System.lineSeparator()
				+ "task.outputs['frame_ids'] = frame_ids" + System.lineSeparator()
				+ "task.outputs['slice_ids'] = slice_ids" + System.lineSeparator()
				+ "task.outputs['rle'] = all_rle" + System.lineSeparator();
		this.script = code;
	}
	
	private <T extends RealType<T> & NativeType<T>> void checkImageIsFine(RandomAccessibleInterval<T> inImg) {
		long[] dims = inImg.dimensionsAsLongArray();
		if ((dims.length < 2)){
			throw new IllegalArgumentException("Image should have at least 2 dimensions");
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
		if (ogImg.numDimensions() == 4 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int j = 0; j < ogImg.dimensionsAsLongArray()[3]; j ++) {
				RandomAccessibleInterval<T> frame = Views.hyperSlice(ogImg, 3, j);
				for (int i = 0; i < 3; i ++) {
					RealTypeConverters.copyFromTo( ImgLib2Utils.convertViewToRGB(Views.hyperSlice(frame, 2, i)), 
							Views.hyperSlice(Views.hyperSlice(targetImg, 3, j), 2, i) );
				}
			}
		} else if (ogImg.numDimensions() == 4 && ogImg.dimensionsAsLongArray()[2] == 1) {
			for (int j = 0; j < ogImg.numDimensions(); j ++) {
				IntervalView<UnsignedByteType> resIm = 
						Views.interval( Views.expandMirrorDouble(ImgLib2Utils.convertViewToRGB(ogImg), new long[] {0, 0, 2, 0}), 
						Intervals.createMinMax(new long[] {0, 0, 0, 0,
								ogImg.dimensionsAsLongArray()[0] - 1, ogImg.dimensionsAsLongArray()[1] - 1, 2, ogImg.dimensionsAsLongArray()[3] - 1}) );
				RealTypeConverters.copyFromTo( resIm, targetImg );
			}
		} else {
			throw new IllegalArgumentException("Error preprocessing the image");
		}
	}
	
	/**
	 * Tests during development
	 * @param args
	 * 	nothing
	 * @throws IOException nothing
	 * @throws RuntimeException nothing
	 * @throws InterruptedException nothing
	 * @throws TaskException 
	 * @throws BuildException 
	 */
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException, TaskException, BuildException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		img = Views.addDimension(img, 1, 2);
		try (EfficientTamJ sam = initializeSam(EfficientTamEnvManager.create())) {
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
				+ "  task.export(threading=threading)" + System.lineSeparator()
				+ "if \"ThreadPoolExecutor\" not in globals().keys():" + System.lineSeparator()
				+ "  from concurrent.futures import ThreadPoolExecutor, as_completed" + System.lineSeparator()
				+ "  task.export(ThreadPoolExecutor=ThreadPoolExecutor)" + System.lineSeparator()
				+ "  task.export(as_completed=as_completed)" + System.lineSeparator()
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
	
	/**
	 * REturn the abbreviated key for the model type that is used internally
	 * @param modelType
	 * 	the verbose string representing the model type. Should be either: "tiny" or "small"
	 * @return the abbreviated modelType
	 */
	public static String abbreviateModelType(String modelType) {
		return MODEL_TYPE_MAP.get(modelType);
	}

	@Override
	protected void processPointsWithSAMAndPropagate(int nPoints, int nNegPoints) {
		// TODO Auto-generated method stub
		
	}
}
