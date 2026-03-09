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
package ai.nets.samj.install;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;


import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.gui.tools.FileUtils;
import ai.nets.samj.models.Sam2;

import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.builder.PixiBuilderFactory;
import org.apposed.appose.tool.Pixi;

import io.bioimage.modelrunner.apposed.appose.Types;
import io.bioimage.modelrunner.download.FileDownloader;

/*
 * Class that is manages the installation of SAM and SAM2 together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class Sam2EnvManager extends SamEnvManagerAbstract {
	
	private final String modelType;
	private String installEnv;
	/**
	 * Default version for the family of SAM2 models
	 */
	final public static String DEFAULT_SAM2 = "tiny";

	/**
	 * Dependencies to be checked to make sure that the environment is able to load a SAM based model. 
	 * General for every supported model.
	 */
	final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch=2.5.1", 
			"torchvision=0.20.1", "skimage", "SAM-2=1.0", "pytest"});
	/**
	 * Byte sizes of all the SAM2.1 options
	 */
	final public static HashMap<String, Long> SAM2_1_BYTE_SIZES_MAP;
	static {
		SAM2_1_BYTE_SIZES_MAP = new HashMap<String, Long>();
		SAM2_1_BYTE_SIZES_MAP.put("tiny", (long) 156_008_466);
		SAM2_1_BYTE_SIZES_MAP.put("small", (long) 184_416_285);
		SAM2_1_BYTE_SIZES_MAP.put("base_plus", (long) 323_606_802);
		SAM2_1_BYTE_SIZES_MAP.put("large", (long) 898_083_611);
	}
	/**
	 * Name of the environment that contains the code and weigths to run SAM2 models
	 */
	final static public String SAM2_ENV_NAME = "samj-env";
	/**
	 * Name of the folder that contains the code and weigths for SAM2 models
	 */
	final static public String SAM2_NAME = "sam2";
	/**
	 * URL to download the SAM2 model 
	 */
	final static private String SAM2_1_URL = "https://dl.fbaipublicfiles.com/segment_anything_2/092824/sam2.1_hiera_%s.pt";
	/**
	 * URL to download the SAM2 model 
	 */
	final static private String SAM2_1_FNAME = "sam2.1_hiera_%s.pt";
	/**
	 * Name of the SAM2 wheel
	 */
	final static private String SAM2_WHEEL = "sam_2-1.0-py3-none-any.whl";
	/**
	 * List of supported CUDA versions
	 */
	final static private ArrayList<String> COMPAT_CUDAS = new ArrayList<>(Arrays.asList("12.1", "12.4", "11.8"));
	
	
	protected Sam2EnvManager(String modelType, String path) throws BuildException {
		if (modelType == null)
			modelType = DEFAULT_SAM2;
		this.path = path;
		List<String> modelTypes = SAM2_1_BYTE_SIZES_MAP.keySet().stream().collect(Collectors.toList());
		if (!modelTypes.contains(modelType) && !modelType.equals("base")) {
			throw new IllegalArgumentException("Invalid model variant chosen: '" + modelType + "'."
					+ "The only supported variants are: " + modelTypes);
		}
		if (modelType.equals("base"))
			modelType = "base_plus";
		this.modelType = modelType;
	    final String pixiTemplate = readClasspathResourceAsString("/pixi.toml");
	    final String cudaVersion = pickCudaVersion(pixiTemplate);

	    final String renderedPixi;
	    if (cudaVersion == null) {
		    renderedPixi = String.format(
		            Locale.ROOT,
		            pixiTemplate,
		            SAM2_ENV_NAME,
		            "", ""
		    );
		    this.installEnv = "cpu";
	    } else {
		    renderedPixi = String.format(
		            Locale.ROOT,
		            pixiTemplate,
		            SAM2_ENV_NAME,
		            cudaVersion.replace(".", ""),
		            cudaVersion.replace(".", "")
		    );
		    this.installEnv = "cuda";
	    }

	    // TODO currently not supported setting the path
	    pixi = Appose.pixi().content(renderedPixi);
	}

    // TODO currently not supported setting the path
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param modelType
	 * 	which of the possible SAM2 wants to be used. The possible variants are the keys of the 
	 * following map: {@link #SAM2_BYTE_SIZES_MAP}
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @return an instance of {@link Sam2EnvManager}
	 * @throws BuildException 
	 */
	private static Sam2EnvManager create(String modelType, Path path) throws BuildException {

		return new Sam2EnvManager(modelType, path.toAbsolutePath().toString());
	}
	
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param modelType
	 * 	which of the possible SAM2 wants to be used. The possible variants are the keys of the 
	 * following map: {@link #SAM2_BYTE_SIZES_MAP}
	 * @return an instance of {@link Sam2EnvManager}
	 * @throws BuildException 
	 */
	public static Sam2EnvManager create(String modelType) throws BuildException {

		return new Sam2EnvManager(modelType, DEFAULT_DIR);
	}

    // TODO currently not supported setting the path
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @return an instance of {@link Sam2EnvManager}
	 * @throws BuildException 
	 */
	private static Sam2EnvManager create(Path path) throws BuildException {

		return new Sam2EnvManager(null, path.toAbsolutePath().toString());
	}
	
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}. Uses the default model {@link #DEFAULT_SAM2}
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link Sam2EnvManager}
	 * @throws BuildException 
	 */
	public static Sam2EnvManager create() throws BuildException {
		return new Sam2EnvManager(null, DEFAULT_DIR);
	}
	
	/**
	 * 
	 * @return which of the possible SAM2 this is. The possible variants are the keys of the following map: {@link #SAM2_BYTE_SIZES_MAP}
	 */
	public String getModelType() {
		return this.modelType;
	}
	
	/**
	 * Check whether the Python environment with the corresponding packages needed to run SAM2
	 * has been installed or not. The environment folder should be named {@value #SAM2_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run SAM2
	 * has been installed or not
	 */
	public boolean checkPixiEnvIsThere() {
		PixiBuilderFactory builder = new PixiBuilderFactory();
		return builder.canWrap(new File(Pixi.BASE_PATH, SAM2_ENV_NAME));
	}
	
	/**
	 * Check whether the Python environment with the corresponding packages needed to run SAM2
	 * has been installed or not. The environment folder should be named {@value #SAM2_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run SAM2
	 * has been installed or not
	 */
	public boolean checkSAMDepsInstalled() {
		File pythonEnv = Paths.get(this.path, SAM2_ENV_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		
		List<String> uninstalled = new ArrayList<String>();
		try {
			uninstalled = DependencyChecker.checkUninstalledDependenciesInEnv(pixi.build(), CHECK_DEPS);
		} catch (Exception e) {
			return false;
		}
		
		return uninstalled.size() == 0;
	}
	
	/**
	 * 
	 * @return whether the weights needed to run SAM2 Small (the standard SAM2) have been 
	 * downloaded and installed or not
	 */
	public boolean checkModelWeightsInstalled() {
		if (!Sam2.getListOfSupportedVariants().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported SAM2 models: " 
												+ Sam2.getListOfSupportedVariants());
		File weightsFile = Paths.get(this.getModelWeigthPath()).toFile();
		if (!weightsFile.isFile()) return false;
		if (weightsFile.length() != SAM2_1_BYTE_SIZES_MAP.get(modelType)) return false;
		return true;
	}
	
	/**
	 * Install the weights of SAM2 Small.
	 * Does not overwrite the weights file if it already exists.
	 */
	public void installModelWeigths() throws IOException, InterruptedException {
		installModelWeigths(false);
	}
	
	/**
	 * Install the weights of SAM2 Small.
	 * @param force
	 * 	whether to overwrite the weights file if it already exists
	 * @throws InterruptedException if the download of weights is interrupted
	 */
	public void installModelWeigths(boolean force) throws IOException, InterruptedException {
		if (!Sam2.getListOfSupportedVariants().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported SAM2 models: " 
												+ Sam2.getListOfSupportedVariants());
		if (!force && this.checkModelWeightsInstalled())
			return;
		this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING SAM2 WEIGHTS");
        try {
    		File file = Paths.get(path, SAM2_ENV_NAME, SAM2_NAME, "weights", FileDownloader.getFileNameFromURLString(String.format(SAM2_1_URL, modelType))).toFile();
    		file.getParentFile().mkdirs();
    		URL url = FileDownloader.redirectedURL(new URL(String.format(SAM2_1_URL, modelType)));
    		Thread parentThread = Thread.currentThread();
    		FileDownloader fd = new FileDownloader(url.toString(), file, false);
    		long size = fd.getOnlineFileSize();
    		Consumer<Double> dConsumer = (d) -> {
    			d = (double) (Math.round(d * 1000) / 10);
    			if (d < 0 || d > 100) this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 WEIGHTS DOWNLOAD: UNKNOWN%");
        		else this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 WEIGHTS DOWNLOAD: " + d + "%");
    		};
    		fd.setPartialProgressConsumer(dConsumer);
    		fd.download(parentThread);    		    		
        	if (size != file.length())
        		throw new IOException("Model SAM2" + modelType + " was not correctly downloaded");
        } catch (IOException ex) {
            this.errConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED SAM2 WEIGHTS INSTALLATION");
            throw ex;
        } catch (URISyntaxException e1) {
        	this.errConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED SAM2 WEIGHTS INSTALLATION");
            throw new IOException("Unable to find the download URL for SAM2 " + modelType + ": " + String.format(SAM2_1_URL, modelType));
		} catch (ExecutionException e) {
            this.errConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED SAM2 WEIGHTS INSTALLATION");
            throw new RuntimeException(e);
		}
        this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 WEIGHTS INSTALLED");
	}
	
	/**
	 * Install the Python environment and dependencies required to run an SAM2 model.
	 * If Micromamba is not installed in the path of the {@link Sam2EnvManager} instance, this method
	 * installs it.
	 * 
	 * @throws InterruptedException if the installation is interrupted
	 * @throws BuildException if there is any error building the environment
	 */
	public void installSAMDeps() throws InterruptedException, BuildException {
		installSAMDeps(false);
	}
	
	/**
	 * Install the Python environment and dependencies required to run an SAM2 model.
	 * If Micromamba is not installed in the path of the {@link Sam2EnvManager} instance, this method
	 * installs it.
	 * @param force
	 * 	if the environment to be created already exists, whether to overwrite it or not
	 * 
	 * 
	 * @throws InterruptedException if the installation is interrupted
	 * @throws BuildException if there is any error building the environment
	 */
	public void installSAMDeps(boolean force) throws InterruptedException, BuildException {
	    if (!force && this.checkPixiEnvIsThere())
	        return;
	    if (this.outConsumer != null)
	    	pixi.subscribeOutput(this.outConsumer);
	    if (this.errConsumer != null)
	    	pixi.subscribeError(this.errConsumer);
	    if (this.pixiConsumer != null)
	    	pixi.subscribeProgress(this.pixiConsumer);
	    pixi.build();
	    //pixi.environment(installEnv).rebuild();
	    installSAM2Wheel();
	}

	private void installSAM2Wheel() throws BuildException {
		try {
			installWheelFromResource("/" + SAM2_WHEEL, pixi.build());
		} catch (IOException e) {
			throw new BuildException("Failed to install SAM2 from wheel: " 
									+ System.lineSeparator() + Types.stackTrace(e));
		}	
	}
	
	/**
	 * Reads a classpath resource fully as UTF-8 text.
	 * Wraps IO/resource errors as BuildException so callers don't need to handle IOException.
	 */
	private String readClasspathResourceAsString(String absoluteResourcePath) throws BuildException {
	    Objects.requireNonNull(absoluteResourcePath, "absoluteResourcePath");

	    try (InputStream is = Sam2EnvManager.class.getResourceAsStream(absoluteResourcePath)) {
	        if (is == null) {
	            throw new BuildException("Required resource not found on classpath: " + absoluteResourcePath);
	        }
	        return new String(readAllBytesJava8(is), StandardCharsets.UTF_8);
	    } catch (IOException e) {
	        throw new BuildException("Failed to read resource: " + absoluteResourcePath, e);
	    }
	}

	/**
	 * Java 8-compatible InputStream -> byte[].
	 */
	private static byte[] readAllBytesJava8(InputStream is) throws IOException {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buffer = new byte[8192];
	    int len;
	    while ((len = is.read(buffer)) != -1) {
	        baos.write(buffer, 0, len);
	    }
	    return baos.toByteArray();
	}

	/**
	 * Pick the first compatible CUDA version based on your template.
	 * Adjust this to match what GpuCompatibility actually needs.
	 */
	private String pickCudaVersion(String pixiTemplate) {
	    for (String cv : COMPAT_CUDAS) {
	        if (GpuCompatibility.canInstallCudaInEnv(cv)) {
	            return cv;
	        }
	    }
	    return null;
	}
	
	/**
	 * Install all the requirements to run SAM2. First, checks if micromamba is installed, if not installs it;
	 * then checks if the Python environment and packages needed to run SAM2 are installed and if not installs it
	 * and finally checks whether the weights are installed, and if not installs them too.
	 * 
	 * 
	 * @throws IOException if there is any file related error in the model installation
	 * @throws InterruptedException if the model installation is interrupted
	 * @throws BuildException if there is any error building the environment
	 */
	public void installEverything() throws IOException, InterruptedException, BuildException {		
		// TODO remove if (!this.checkSAMDepsInstalled()) this.installSAMDeps();
		if (!this.checkPixiEnvIsThere()) this.installSAMDeps();

		if (!this.checkModelWeightsInstalled()) this.installModelWeigths();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment needed to run SAM2
	 */
	public String getModelEnv() {
		File file = Paths.get(path, SAM2_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the official name of the SAM2 Small weights
	 */
	public String getModelWeigthsName() {
		try {
			return FileDownloader.getFileNameFromURLString(String.format(SAM2_1_URL, modelType));
		} catch (MalformedURLException e) {
			return String.format(SAM2_1_FNAME, modelType);
		}
	}

	@Override
	public String getModelWeigthPath() {
		File file;
		try {
			file = Paths.get(path, SAM2_ENV_NAME, SAM2_NAME, "weights", FileDownloader.getFileNameFromURLString(String.format(SAM2_1_URL, modelType))).toFile();
		} catch (MalformedURLException e) {
			file = Paths.get(path, SAM2_ENV_NAME, SAM2_NAME, "weights", String.format(SAM2_1_FNAME, modelType)).toFile();
		}

		return file.getAbsolutePath();
	}

	@Override
	public boolean checkEverythingInstalled() {		
		// TODO remove if (!this.checkSAMDepsInstalled()) return false;
		if (!this.checkPixiEnvIsThere()) return false;

		if (!this.checkModelWeightsInstalled()) return false;
		
		return true;
	}

	@Override
	public void uninstall() {
		if (new File(this.getModelWeigthPath()).getParentFile().list().length != 1)
			FileUtils.deleteFolder(new File(this.getModelWeigthPath()));
		else
			FileUtils.deleteFolder(new File(this.getModelEnv()));
	}
}
