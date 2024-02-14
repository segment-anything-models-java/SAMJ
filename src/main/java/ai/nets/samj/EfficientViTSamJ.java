package ai.nets.samj;

import java.lang.AutoCloseable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;

import io.bioimage.modelrunner.apposed.appose.Environment;
import io.bioimage.modelrunner.apposed.appose.Service;
import io.bioimage.modelrunner.apposed.appose.Service.Task;
import io.bioimage.modelrunner.apposed.appose.Service.TaskStatus;

import io.bioimage.modelrunner.tensor.shm.SharedMemoryArray;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.RealTypeConverters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class EfficientViTSamJ extends AbstractSamJ implements AutoCloseable {

	private final Environment env;
	
	private final Service python;
	
	private String script = "";
	
	private SharedMemoryArray shma;
	
	private long[] targetDims;
	
	private SamEnvManager manager;
	
	private static final HashMap<String, String> MODELS_DICT = new HashMap<String, String>();
	static {
		MODELS_DICT.put("l0", "efficientvit_sam_l0");
		MODELS_DICT.put("l1", "efficientvit_sam_l1");
		MODELS_DICT.put("l2", "efficientvit_sam_l2");
		MODELS_DICT.put("xl0", "efficientvit_sam_xl0");
		MODELS_DICT.put("xl1", "efficientvit_sam_xl1");
	}
	
	public static final String IMPORTS = ""
			+ "task.update('start')" + System.lineSeparator()
			+ "from skimage import measure" + System.lineSeparator()
			+ "import numpy as np" + System.lineSeparator()
			+ "import torch" + System.lineSeparator()
			+ "import sys" + System.lineSeparator()
			+ "import os" + System.lineSeparator()
			+ "sys.path.append('%s')" + System.lineSeparator()
			+ "from multiprocessing import shared_memory" + System.lineSeparator()
			+ "task.update('import sam')" + System.lineSeparator()
			+ "from efficientvit.models.nn.norm import set_norm_eps" + System.lineSeparator()
			+ "from efficientvit.models.efficientvit import EfficientViTSam, %s" + System.lineSeparator()
			+ "from efficientvit.models.efficientvit.sam import EfficientViTSamPredictor" + System.lineSeparator()
			+ "task.update('imported')" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "predictor = %s().cpu().eval()" + System.lineSeparator()
			+ "eps = 1e-6" + System.lineSeparator()
			+ "for m in predictor.modules():" + System.lineSeparator()
			+ "  if isinstance(m, (torch.nn.GroupNorm, torch.nn.LayerNorm, torch.nn.modules.batchnorm._BatchNorm)):" + System.lineSeparator()
			+ "    if eps is not None:" + System.lineSeparator()
			+ "      m.eps = eps" + System.lineSeparator()
			+ "file = os.path.realpath(os.path.expanduser(%S))" + System.lineSeparator()
			+ "weight = torch.load(file, map_location='cpu')" + System.lineSeparator()
			+ "if \"state_dict\" in weight:" + System.lineSeparator()
			+ "  weight = weight[\"state_dict\"]" + System.lineSeparator()
			+ "predictor.load_state_dict(weight)" + System.lineSeparator()
			+ "task.update('created predictor')" + System.lineSeparator()
			+ "globals()['shared_memory'] = shared_memory" + System.lineSeparator()
			+ "globals()['measure'] = measure" + System.lineSeparator()
			+ "globals()['np'] = np" + System.lineSeparator()
			+ "globals()['torch'] = torch" + System.lineSeparator()
			+ "globals()['predictor'] = predictor" + System.lineSeparator();
	private String IMPORTS_FORMATED;

	private EfficientViTSamJ(SamEnvManager manager, String type) throws IOException, RuntimeException, InterruptedException {
		this(manager, type, (t) -> {}, false);
	}

	private EfficientViTSamJ(SamEnvManager manager, String type,
	                      final DebugTextPrinter debugPrinter,
	                      final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {

		if (!MODELS_DICT.keySet().contains(type))
			throw new IllegalArgumentException("The model type should be one of hte following: " 
							+ MODELS_DICT.keySet().stream().collect(Collectors.toList()));
		this.debugPrinter = debugPrinter;
		this.isDebugging = printPythonCode;

		this.env = new Environment() {
			@Override public String base() { return manager.getPythonEnv(); }
			@Override public boolean useSystemPath() { return false; }
			};
		python = env.python();
		python.debug(debugPrinter::printText);
		IMPORTS_FORMATED = String.format(IMPORTS,
				manager.getEfficientSamEnv() + File.separator + SamEnvManager.ESAM_NAME,
				manager.getEfficientSAMSmallWeightsPath());
		printScript(IMPORTS_FORMATED + PythonMethods.TRACE_EDGES, "Edges tracing code");
		Task task = python.task(IMPORTS_FORMATED + PythonMethods.TRACE_EDGES);
		task.waitFor();
		if (task.status == TaskStatus.CANCELED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.FAILED)
			throw new RuntimeException();
		else if (task.status == TaskStatus.CRASHED)
			throw new RuntimeException();
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(String modelType, SamEnvManager manager,
	              RandomAccessibleInterval<T> image,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		EfficientViTSamJ sam = null;
		try{
			sam = new EfficientViTSamJ(manager, modelType, debugPrinter, printPythonCode);
			sam.addImage(image);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(String modelType, SamEnvManager manager, RandomAccessibleInterval<T> image) 
				throws IOException, RuntimeException, InterruptedException {
		EfficientViTSamJ sam = null;
		try{
			sam = new EfficientViTSamJ(manager, modelType);
			sam.addImage(image);
		} catch (IOException | RuntimeException | InterruptedException ex) {
			if (sam != null) sam.close();
			throw ex;
		}
		return sam;
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(SamEnvManager manager,
	              RandomAccessibleInterval<T> image,
	              final DebugTextPrinter debugPrinter,
	              final boolean printPythonCode) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(SamEnvManager.DEFAULT_EVITSAM, manager, image, debugPrinter, printPythonCode);
	}

	public static <T extends RealType<T> & NativeType<T>> EfficientViTSamJ
	initializeSam(SamEnvManager manager, RandomAccessibleInterval<T> image) throws IOException, RuntimeException, InterruptedException {
		return initializeSam(SamEnvManager.DEFAULT_EVITSAM, manager, image);
	}
	
	public <T extends RealType<T> & NativeType<T>>
	void updateImage(RandomAccessibleInterval<T> rai) throws IOException, RuntimeException, InterruptedException {
		addImage(rai);
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void addImage(RandomAccessibleInterval<T> rai) 
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		sendImgLib2AsNp(rai);
		this.script += ""
				+ "task.update(str(im.shape))" + System.lineSeparator()
				+ "predictor.set_image(im)";
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
	
	public List<Polygon> processPoints(List<int[]> pointsList)
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processPointsWithSAM(pointsList.size(), 0);
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_points", pointsList);
		printScript(script, "Points inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		debugPrinter.printText("processPoints() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	public List<Polygon> processPoints(List<int[]> pointsList, List<int[]> pointsNegList)
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processPointsWithSAM(pointsList.size(), pointsNegList.size());
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_points", pointsList);
		inputs.put("input_neg_points", pointsNegList);
		printScript(script, "Points and negative points inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		debugPrinter.printText("processPoints() obtained " + polys.size() + " polygons");
		return polys;
	}
	
	public List<Polygon> processBox(int[] boundingBox)
			throws IOException, RuntimeException, InterruptedException{
		this.script = "";
		processBoxWithSAM();
		HashMap<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("input_box", boundingBox);
		printScript(script, "Rectangle inference");
		List<Polygon> polys = processAndRetrieveContours(inputs);
		debugPrinter.printText("processBox() obtained " + polys.size() + " polygons");
		return polys;
	}


	@Override
	public void close() {
		python.close();
	}
	
	private <T extends RealType<T> & NativeType<T>> 
	void sendImgLib2AsNp(RandomAccessibleInterval<T> targetImg) {
		shma = createEfficientSAMInputSHM(targetImg);
		adaptImageToModel(targetImg, shma.getSharedRAI());
		String code = "";
		// This line wants to recreate the original numpy array. Should look like:
		// input0_appose_shm = shared_memory.SharedMemory(name=input0)
		// input0 = np.ndarray(size, dtype="float64", buffer=input0_appose_shm.buf).reshape([64, 64])
		code += IMPORTS_FORMATED+"im_shm = shared_memory.SharedMemory(name='"
							+ shma.getNameForPython() + "', size=" + shma.getSize() 
							+ ")" + System.lineSeparator();
		int size = 1;
		for (long l : targetDims) {size *= l;}
		code += "im = np.ndarray(" + size + ", dtype='uint8', buffer=im_shm.buf).reshape([";
		for (long ll : targetDims)
			code += ll + ", ";
		code = code.substring(0, code.length() - 2);
		code += "])" + System.lineSeparator();
		code += "im_shm.unlink()" + System.lineSeparator();
		//code += "box_shm.close()" + System.lineSeparator();
		this.script += code;
	}
	
	private void processPointsWithSAM(int nPoints, int nNegPoints) {
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
				+ "input_label[" + nPoints + ":] *= 2" + System.lineSeparator()
				+ "predicted_logits, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=input_points," + System.lineSeparator()
				+ "    point_labels=input_label," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ "    box=None,)" + System.lineSeparator()
				+ "sorted_ids = torch.argsort(predicted_iou, dim=-1, descending=True)" + System.lineSeparator()
				+ "predicted_iou = torch.take_along_dim(predicted_iou, sorted_ids, dim=2)" + System.lineSeparator()
				+ "predicted_logits = torch.take_along_dim(predicted_logits, sorted_ids[..., None, None], dim=2)" + System.lineSeparator()
				+ "mask = torch.ge(predicted_logits[0, 0, 0, :, :], 0).cpu().detach().numpy()" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				//+ "np.save('/temp/aa.npy', mask)" + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask)" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}
	
	private void processBoxWithSAM() {
		String code = "" + System.lineSeparator()
				+ "task.update('start predict')" + System.lineSeparator()
				+ "input_box = np.array([[input_box[0], input_box[1]], [input_box[2], input_box[3]]])" + System.lineSeparator()
				+ "predicted_logits, _ = predictor.predict(" + System.lineSeparator()
				+ "    point_coords=None," + System.lineSeparator()
				+ "    point_labels=None," + System.lineSeparator()
				+ "    multimask_output=False," + System.lineSeparator()
				+ "    box=input_box,)" + System.lineSeparator()
				+ "sorted_ids = torch.argsort(predicted_iou, dim=-1, descending=True)" + System.lineSeparator()
				+ "predicted_iou = torch.take_along_dim(predicted_iou, sorted_ids, dim=2)" + System.lineSeparator()
				+ "predicted_logits = torch.take_along_dim(predicted_logits, sorted_ids[..., None, None], dim=2)" + System.lineSeparator()
				+ "mask = torch.ge(predicted_logits[0, 0, 0, :, :], 0).cpu().detach().numpy()" + System.lineSeparator()
				+ "task.update('end predict')" + System.lineSeparator()
				+ "task.update(str(mask.shape))" + System.lineSeparator()
				//+ "np.save('/temp/aa.npy', mask)" + System.lineSeparator()
				+ "contours_x,contours_y = get_polygons_from_binary_mask(mask)" + System.lineSeparator()
				+ "task.update('all contours traced')" + System.lineSeparator()
				+ "task.outputs['contours_x'] = contours_x" + System.lineSeparator()
				+ "task.outputs['contours_y'] = contours_y" + System.lineSeparator();
		this.script = code;
	}
	
	private static <T extends RealType<T> & NativeType<T>>
	SharedMemoryArray  createEfficientSAMInputSHM(final RandomAccessibleInterval<T> inImg) {
		long[] dims = inImg.dimensionsAsLongArray();
		if ((dims.length != 3 && dims.length != 2) || (dims.length == 3 && dims[2] != 3 && dims[2] != 1)) {
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, float32, ...) 2D images."
					+ "The image dimensions order should be 'xyc', first dimension height, second width and third channels.");
		}
		return SharedMemoryArray.buildMemorySegmentForImage(new long[] {dims[0], dims[1], 3}, new UnsignedIntType());
	}
	
	private <T extends RealType<T> & NativeType<T>>
	void adaptImageToModel(final RandomAccessibleInterval<T> ogImg, RandomAccessibleInterval<UnsignedIntType> targetImg) {
		if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 3) {
			for (int i = 0; i < 3; i ++) 
				RealTypeConverters.copyFromTo( normalizedView(Views.hyperSlice(ogImg, 2, i)), Views.hyperSlice(targetImg, 2, i) );
		} else if (ogImg.numDimensions() == 3 && ogImg.dimensionsAsLongArray()[2] == 1) {
			debugPrinter.printText("CONVERTED 1 CHANNEL IMAGE INTO 3 TO BE FEEDED TO SAMJ");
			IntervalView<T> resIm = Views.interval( Views.expandMirrorDouble(normalizedView(ogImg), new long[] {0, 0, 2}), 
					Intervals.createMinMax(new long[] {0, 0, 0, ogImg.dimensionsAsLongArray()[0], ogImg.dimensionsAsLongArray()[1], 2}) );
			RealTypeConverters.copyFromTo( resIm, targetImg );
		} else if (ogImg.numDimensions() == 2) {
			adaptImageToModel(Views.addDimension(ogImg, 0, 0), targetImg);
		} else {
			throw new IllegalArgumentException("Currently SAMJ only supports 1-channel (grayscale) or 3-channel (RGB, BGR, ...) 2D images."
					+ "The image dimensions order should be 'yxc', first dimension height, second width and third channels.");
		}
		this.targetDims = targetImg.dimensionsAsLongArray();
	}
	
	public static void main(String[] args) throws IOException, RuntimeException, InterruptedException {
		RandomAccessibleInterval<UnsignedByteType> img = ArrayImgs.unsignedBytes(new long[] {50, 50, 3});
		img = Views.addDimension(img, 1, 2);
		try (EfficientViTSamJ sam = initializeSam(SamEnvManager.create(), img)) {
			sam.processBox(new int[] {0, 5, 10, 26});
		}
	}
	
	public static List<String> getListOfSupportedEfficientViTSAM(){
		return MODELS_DICT.keySet().stream().collect(Collectors.toList());
	}
}