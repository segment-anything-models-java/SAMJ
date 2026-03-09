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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apposed.appose.BuildException;

import ai.nets.samj.gui.tools.FileUtils;
import ai.nets.samj.models.EfficientTamJ;
import ai.nets.samj.models.Sam2;
import io.bioimage.modelrunner.apposed.appose.Types;
import io.bioimage.modelrunner.download.FileDownloader;

/*
 * Class that is manages the installation of SAM and EFFICIENTTAM together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class EfficientTamEnvManager extends Sam2EnvManager {
	
	private final String modelType;
	/**
	 * Default version for the family of EFFICIENTTAM models
	 */
	final public static String DEFAULT = "tiny";
	/**
	 * Name of the folder that contains the code and weigths for EFFICIENTTAM models
	 */
	final static public String EFFTAM_NAME = "EfficientTAM";
	/**
	 * Byte sizes of all the EFFICIENTTAM options
	 */
	final public static HashMap<String, Long> EFFTAM_BYTE_SIZES_MAP;
	static {
		EFFTAM_BYTE_SIZES_MAP = new HashMap<String, Long>();
		EFFTAM_BYTE_SIZES_MAP.put("small", (long) 136_375_868);
		EFFTAM_BYTE_SIZES_MAP.put("tiny", (long) 71_616_316 );
	}
	/**
	 * URL to download the EFFICIENTTAM model 
	 */
	final static private String EFFTAM_URL = "https://huggingface.co/yunyangx/efficient-track-anything/resolve/main/efficienttam_%s.pt?download=true";
	/**
	 * URL to download the EFFICIENTTAM model 
	 */
	final static private String EFFTAM_FNAME = "efficienttam_%s.pt";
	final static private String EFFTAM_WHEEL = "efficient_track_anything-1.0-py3-none-any.whl";
	
	private EfficientTamEnvManager(String modelType, String path) throws BuildException {
		super(Sam2EnvManager.DEFAULT_SAM2, path);
		List<String> modelTypes = EFFTAM_BYTE_SIZES_MAP.keySet().stream().collect(Collectors.toList());
		if (!modelTypes.contains(modelType) && !modelType.equals("base")) {
			throw new IllegalArgumentException("Invalid model variant chosen: '" + modelType + "'."
					+ "The only supported variants are: " + modelTypes);
		}
		if (modelType.equals("base"))
			modelType = "base_plus";
		this.modelType = modelType;
	}

    // TODO currently not supported setting the path
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param modelType
	 * 	which of the possible SAM2 wants to be used. The possible variants are the keys of the 
	 * following map: {@link #SAM2_BYTE_SIZES_MAP}
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @return an instance of {@link EfficientTamEnvManager}
	 * @throws BuildException 
	 */
	private static EfficientTamEnvManager create(String modelType, Path path) throws BuildException {

		return new EfficientTamEnvManager(modelType, path.toAbsolutePath().toString());
	}
	
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param modelType
	 * 	which of the possible SAM2 wants to be used. The possible variants are the keys of the 
	 * following map: {@link #SAM2_BYTE_SIZES_MAP}
	 * @return an instance of {@link EfficientTamEnvManager}
	 * @throws BuildException 
	 */
	public static EfficientTamEnvManager create(String modelType) throws BuildException {

		return new EfficientTamEnvManager(modelType, DEFAULT_DIR);
	}

    // TODO currently not supported setting the path
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @return an instance of {@link EfficientTamEnvManager}
	 * @throws BuildException 
	 */
	private static EfficientTamEnvManager create(Path path) throws BuildException {

		return new EfficientTamEnvManager(null, path.toAbsolutePath().toString());
	}
	
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}. Uses the default model {@link #DEFAULT_SAM2}
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link EfficientTamEnvManager}
	 * @throws BuildException 
	 */
	public static EfficientTamEnvManager create() throws BuildException {
		return new EfficientTamEnvManager(null, DEFAULT_DIR);
	}
	
	/**
	 * 
	 * @return which of the possible EFFICIENTTAM this is. The possible variants are the keys of the following map: {@link #EFFTAM_BYTE_SIZES_MAPf}
	 */
	public String getModelType() {
		return this.modelType;
	}
	
	/**
	 * 
	 * @return whether the weights needed to run EFFICIENTTAM Small (the standard EFFICIENTTAM) have been 
	 * downloaded and installed or not
	 */
	public boolean checkModelWeightsInstalled() {
		if (!EfficientTamJ.getListOfSupportedVariants().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EFFICIENTTAM models: " 
												+ EfficientTamJ.getListOfSupportedVariants());
		File weightsFile = Paths.get(this.getModelWeigthPath()).toFile();
		if (!weightsFile.isFile()) return false;
		if (weightsFile.length() != EFFTAM_BYTE_SIZES_MAP.get(modelType)) return false;
		return true;
	}
	
	/**
	 * Install the weights of EFFICIENTTAM Small.
	 * Does not overwrite the weights file if it already exists.
	 */
	public void installModelWeigths() throws IOException, InterruptedException {
		installModelWeigths(false);
	}
	
	/**
	 * Install the weights of EFFICIENTTAM Small.
	 * @param force
	 * 	whether to overwrite the weights file if it already exists
	 * @throws InterruptedException if the download of weights is interrupted
	 */
	public void installModelWeigths(boolean force) throws IOException, InterruptedException {
		if (!EfficientTamJ.getListOfSupportedVariants().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientTAM models: " 
												+ EfficientTamJ.getListOfSupportedVariants());
		if (!force && this.checkModelWeightsInstalled())
			return;
		if (modelType.equals(DEFAULT))
			extractWeights();
		else
			downloadWeights();
	}
	
	private void downloadWeights() throws IOException, InterruptedException {
		if (!Sam2.getListOfSupportedVariants().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientTAM models: " 
												+ EfficientTamJ.getListOfSupportedVariants());
        try {
    		File file = Paths.get(path, SAM2_ENV_NAME, SAM2_NAME, "weights", FileDownloader.getFileNameFromURLString(String.format(EFFTAM_URL, modelType))).toFile();
    		file.getParentFile().mkdirs();
    		URL url = FileDownloader.redirectedURL(new URL(String.format(EFFTAM_URL, modelType)));
    		Thread parentThread = Thread.currentThread();
    		FileDownloader fd = new FileDownloader(url.toString(), file, false);
    		long size = fd.getOnlineFileSize();
    		Consumer<Double> dConsumer = (d) -> {
    			d = (double) (Math.round(d * 1000) / 10);
    			if (d < 0 || d > 100) this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 WEIGHTS DOWNLOAD: UNKNOWN%");
        		else this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EfficientTAM WEIGHTS DOWNLOAD: " + d + "%");
    		};
    		fd.setPartialProgressConsumer(dConsumer);
    		fd.download(parentThread);    		    		
        	if (size != file.length())
        		throw new IOException("Model EfficientTAM" + modelType + " was not correctly downloaded");
        } catch (IOException ex) {
            this.errConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EfficientTAM WEIGHTS INSTALLATION");
            throw ex;
        } catch (URISyntaxException e1) {
        	this.errConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EfficientTAM WEIGHTS INSTALLATION");
            throw new IOException("Unable to find the download URL for EfficientTAM " + modelType + ": " + String.format(EFFTAM_URL, modelType));
		} catch (ExecutionException e) {
            this.errConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EfficientTAM WEIGHTS INSTALLATION");
            throw new RuntimeException(e);
		}
        this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EfficientTAM WEIGHTS INSTALLED");
	}
	
	private void extractWeights() throws InterruptedException, IOException {
		this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTTAM WEIGHTS");
		String zipResourcePath = "efficienttam_ti.zip";
        String outputDirectory = Paths.get(path, SAM2_ENV_NAME, EFFTAM_NAME, "weights").toFile().getAbsolutePath();
        try (
        	InputStream zipInputStream = EfficientTamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
        	ZipInputStream zipInput = new ZipInputStream(zipInputStream);
        		) {
        	ZipEntry entry;
        	while ((entry = zipInput.getNextEntry()) != null) {
                File entryFile = new File(outputDirectory + File.separator + entry.getName());
                if (entry.isDirectory()) {
                	entryFile.mkdirs();
                	continue;
                }
            	entryFile.getParentFile().mkdirs();
                try (OutputStream entryOutput = new FileOutputStream(entryFile)) {
                	if (Thread.interrupted()) throw new InterruptedException("EfficientTAM installation interrupted");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInput.read(buffer)) != -1) {
                        entryOutput.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException ex) {
        	this.errConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTTAM WEIGHTS INSTALLATION");
            throw ex;
        }
        this.outConsumer.accept(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTTAM WEIGHTS INSTALLED");
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EFFICIENTTAM model.
	 * If Micromamba is not installed in the path of the {@link EfficientTamEnvManager} instance, this method
	 * installs it.
	 * 
	 * @throws InterruptedException if the installation is interrupted
	 * @throws BuildException if there is any error creating the python environment
	 */
	public void installSAMDeps() throws InterruptedException, BuildException {
		installSAMDeps(false);
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EFFICIENTTAM model.
	 * If Micromamba is not installed in the path of the {@link EfficientTamEnvManager} instance, this method
	 * installs it.
	 * @param force
	 * 	if the environment to be created already exists, whether to overwrite it or not
	 * 
	 * 
	 * @throws InterruptedException if the installation is interrupted
	 * @throws BuildException if there is any error installing the environment
	 */
	public void installSAMDeps(boolean force) throws InterruptedException, BuildException {
		super.installSAMDeps(force);
	    installEffTAMWheel();
	}

	private void installEffTAMWheel() throws BuildException {
		try {
			installWheelFromResource("/" + EFFTAM_WHEEL, pixi.build());
		} catch (IOException e) {
			throw new BuildException("Failed to install EfficientTAM from wheel: " 
									+ System.lineSeparator() + Types.stackTrace(e));
		}	
	}
	
	/**
	 * 
	 * @return the official name of the EFFICIENTTAM Small weights
	 */
	public String getModelWeigthsName() {
		try {
			return FileDownloader.getFileNameFromURLString(String.format(EFFTAM_URL, modelType));
		} catch (MalformedURLException e) {
			return String.format(EFFTAM_FNAME, modelType);
		}
	}

	@Override
	public String getModelWeigthPath() {
		File file;
		try {
			file = Paths.get(path, SAM2_ENV_NAME, EFFTAM_NAME, "weights", FileDownloader.getFileNameFromURLString(String.format(EFFTAM_URL, EfficientTamJ.abbreviateModelType(modelType)))).toFile();
		} catch (MalformedURLException e) {
			file = Paths.get(path, SAM2_ENV_NAME, EFFTAM_NAME, "weights", String.format(EFFTAM_FNAME, EfficientTamJ.abbreviateModelType(modelType))).toFile();
		}

		return file.getAbsolutePath();
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
		else installEffTAMWheel();

		if (!this.checkModelWeightsInstalled()) this.installModelWeigths();
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
