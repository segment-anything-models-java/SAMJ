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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.gui.tools.Files;
import ai.nets.samj.models.Sam2;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.apposed.appose.MambaInstallerUtils;
import io.bioimage.modelrunner.bioimageio.download.DownloadModel;

/*
 * Class that is manages the installation of SAM and EfficientSAM together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class Sam2EnvManager extends SamEnvManagerAbstract {
	
	private final String modelType;
	/**
	 * Default version for the family of SAM2 models
	 */
	final public static String DEFAULT_SAM2 = "tiny";

	/**
	 * Dependencies to be checked to make sure that the environment is able to load a SAM based model. 
	 * General for every supported model.
	 */
	final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch=2.4.0", 
			"torchvision=0.19.0", "skimage", "sam2", "pytest"});
	/**
	 * Dependencies that have to be installed in any SAMJ created environment using Mamba or Conda
	 */
	final public static List<String> INSTALL_CONDA_DEPS;
	static {
		if (!PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64) && !PlatformDetection.isUsingRosseta())
			INSTALL_CONDA_DEPS = Arrays.asList(new String[] {"scikit-image", "pytorch=2.4.0", "torchvision=0.19.0"});
		else 
			INSTALL_CONDA_DEPS = Arrays.asList(new String[] {"scikit-image", "pytorch=2.4.0", "torchvision=0.19.0"});
	}
	/**
	 * Dependencies for every environment that need to be installed using PIP
	 */
	final public static List<String> INSTALL_PIP_DEPS;
	static {
		if (!PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64) && !PlatformDetection.isUsingRosseta() && PlatformDetection.isMacOS())
			INSTALL_PIP_DEPS = Arrays.asList(new String[] {"mkl==2023.2.2", "samv2==0.0.4", "pytest"});
		else if (!PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64) && !PlatformDetection.isUsingRosseta())
			INSTALL_PIP_DEPS = Arrays.asList(new String[] {"mkl==2024.0.0", "samv2==0.0.4", "pytest"});
		else 
			INSTALL_PIP_DEPS = Arrays.asList(new String[] {"samv2==0.0.4", "pytest"});
	}
	/**
	 * Byte sizes of all the SAM2 options
	 */
	final public static HashMap<String, Long> SAM2_BYTE_SIZES_MAP;
	static {
		SAM2_BYTE_SIZES_MAP = new HashMap<String, Long>();
		SAM2_BYTE_SIZES_MAP.put("tiny", (long) 155906050);
		SAM2_BYTE_SIZES_MAP.put("small", (long) 184309650);
		SAM2_BYTE_SIZES_MAP.put("base_plus", (long) -1);
		SAM2_BYTE_SIZES_MAP.put("large", (long) 897952466);
	}
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
	 * Name of the environment that contains the code and weigths to run EfficientSAM models
	 */
	final static public String SAM2_ENV_NAME = "sam2";
	/**
	 * Name of the folder that contains the code and weigths for EfficientSAM models
	 */
	final static public String SAM2_NAME = "sam2";
	/**
	 * URL to download the SAM2 model 
	 */
	final static private String SAM2_URL = "https://dl.fbaipublicfiles.com/segment_anything_2/092824/sam2.1_hiera_%s.pt";
	/**
	 * URL to download the SAM2 model 
	 */
	final static private String SAM2_FNAME = "sam2.1_hiera_%s.pt";
	
	private Sam2EnvManager(String modelType) {
		List<String> modelTypes = SAM2_BYTE_SIZES_MAP.keySet().stream().collect(Collectors.toList());
		if (!modelTypes.contains(modelType) && !modelType.equals("base")) {
			throw new IllegalArgumentException("Invalid model variant chosen: '" + modelType + "'."
					+ "The only supported variants are: " + modelTypes);
		}
		if (modelType.equals("base"))
			modelType = "base_plus";
		this.modelType = modelType;
	}
	
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param modelType
	 * 	which of the possible SAM2 wants to be used. The possible variants are the keys of the following map: {@link #SAM2_BYTE_SIZES_MAP}
	 * @return an instance of {@link Sam2EnvManager}
	 */
	public static Sam2EnvManager create(String path, String modelType) {
		return create(path, modelType == null ? DEFAULT_SAM2 : modelType, (ss) -> {});
	}
	
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param modelType
	 * 	which of the possible SAM2 wants to be used. The possible variants are the keys of the following map: {@link #SAM2_BYTE_SIZES_MAP}
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link Sam2EnvManager}
	 */
	public static Sam2EnvManager create(String path, String modelType, Consumer<String> consumer) {
		Sam2EnvManager installer = new Sam2EnvManager(modelType);
		installer.path = path;
		installer.consumer = consumer;
		installer.mamba = new Mamba(path);
		return installer;
	}
	
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}. Uses the default model {@link #DEFAULT_SAM2}
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link Sam2EnvManager}
	 */
	public static Sam2EnvManager create() {
		return create(DEFAULT_DIR, null);
	}
	
	/**
	 * Creates an instance of {@link Sam2EnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link Sam2EnvManager}
	 */
	public static Sam2EnvManager create(Consumer<String> consumer) {
		return create(DEFAULT_DIR, null, consumer);
	}
	
	/**
	 * Check whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not. The environment folder should be named {@value #SAM2_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not
	 */
	public boolean checkSAMDepsInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", SAM2_ENV_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		
		List<String> uninstalled;
		try {
			uninstalled = mamba.checkUninstalledDependenciesInEnv(pythonEnv.getAbsolutePath(), CHECK_DEPS);
		} catch (MambaInstallException e) {
			return false;
		}
		
		return uninstalled.size() == 0;
	}
	
	/**
	 * 
	 * @return whether the weights needed to run EfficientSAM Small (the standard EfficientSAM) have been 
	 * downloaded and installed or not
	 */
	public boolean checkModelWeightsInstalled() {
		if (!Sam2.getListOfSupportedVariants().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported SAM2 models: " 
												+ Sam2.getListOfSupportedVariants());
		File weightsFile = Paths.get(this.getModelWeigthPath()).toFile();
		if (!weightsFile.isFile()) return false;
		if (weightsFile.length() != SAM2_BYTE_SIZES_MAP.get(modelType)) return false;
		return true;
	}
	
	/**
	 * Install the weights of EfficientSAM Small.
	 * Does not overwrite the weights file if it already exists.
	 */
	public void installModelWeigths() throws IOException, InterruptedException {
		installModelWeigths(false);
	}
	
	/**
	 * Install the weights of EfficientSAM Small.
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
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING SAM2 WEIGHTS (" + modelType + ")");
        try {
    		File file = Paths.get(path, "envs", SAM2_ENV_NAME, SAM2_NAME, "weights", DownloadModel.getFileNameFromURLString(String.format(SAM2_URL, modelType))).toFile();
    		file.getParentFile().mkdirs();
    		URL url = MambaInstallerUtils.redirectedURL(new URL(String.format(SAM2_URL, modelType)));
    		Thread parentThread = Thread.currentThread();
    		Thread downloadThread = new Thread(() -> {
    			try {
    				downloadFile(url.toString(), file, parentThread);
    			} catch (IOException | URISyntaxException | InterruptedException e) {
    				e.printStackTrace();
    			}
            });
    		downloadThread.start();
    		long size = DownloadModel.getFileSize(url);
        	while (downloadThread.isAlive()) {
        		try {Thread.sleep(280);} catch (InterruptedException e) {break;}
        		double progress = Math.round( (double) 100 * file.length() / size ); 
        		if (progress < 0 || progress > 100) passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 WEIGHTS DOWNLOAD: UNKNOWN%");
        		else passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 WEIGHTS DOWNLOAD: " + progress + "%");
        	}
        	if (size != file.length())
        		throw new IOException("Model SAM2" + modelType + " was not correctly downloaded");
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED SAM2 WEIGHTS INSTALLATION");
            throw ex;
        } catch (URISyntaxException e1) {
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED SAM2 WEIGHTS INSTALLATION");
            throw new IOException("Unable to find the download URL for SAM2 " + modelType + ": " + String.format(SAM2_URL, modelType));

		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 WEIGHTS INSTALLED");
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EfficientSAM model.
	 * If Micromamba is not installed in the path of the {@link Sam2EnvManager} instance, this method
	 * installs it.
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer files
	 * @throws URISyntaxException if there is any error witht the URL to download micromamba
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installSAMDeps() throws IOException, InterruptedException, ArchiveException, URISyntaxException, MambaInstallException {
		installSAMDeps(false);
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EfficientSAM model.
	 * If Micromamba is not installed in the path of the {@link Sam2EnvManager} instance, this method
	 * installs it.
	 * @param force
	 * 	if the environment to be created already exists, whether to overwrite it or not
	 * 
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installSAMDeps(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to install Python without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE SAM2 PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		if (!this.checkSAMDepsInstalled() || force) {
			try {
				mamba.create(SAM2_ENV_NAME, true, args);
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED SAM2 PYTHON ENVIRONMENT CREATION");
				throw new MambaInstallException("Unable to install Python without first installing Mamba. ");
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED SAM2 PYTHON ENVIRONMENT CREATION");
				throw e;
			}
			ArrayList<String> pipInstall = new ArrayList<String>();
			for (String ss : new String[] {"-m", "pip", "install"}) pipInstall.add(ss);
			for (String ss : INSTALL_PIP_DEPS) pipInstall.add(ss);
			try {
				Mamba.runPythonIn(Paths.get(path,  "envs", SAM2_ENV_NAME).toFile(), pipInstall.stream().toArray( String[]::new ));
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION WHEN INSTALLING PIP DEPENDENCIES");
				throw e;
			}
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- SAM2 PYTHON ENVIRONMENT CREATED");
        installApposePackage(SAM2_ENV_NAME);
	}
	
	/**
	 * Install all the requirements to run EfficientSAM. First, checks if micromamba is installed, if not installs it;
	 * then checks if the Python environment and packages needed to run EfficientSAM are installed and if not installs it
	 * and finally checks whether the weights are installed, and if not installs them too.
	 * 
	 * 
	 * @throws IOException if there is any file related error in the model installation
	 * @throws InterruptedException if the model installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer
	 * @throws URISyntaxException if there is any error with the URL to the micromamba installer download page
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installEverything() throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkSAMDepsInstalled()) this.installSAMDeps();
		
		if (!this.checkModelWeightsInstalled()) this.installModelWeigths();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment needed to run EfficientSAM
	 */
	public String getModelEnv() {
		File file = Paths.get(path, "envs", SAM2_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the official name of the EfficientSAM Small weights
	 */
	public String getModelWeigthsName() {
		try {
			return DownloadModel.getFileNameFromURLString(String.format(SAM2_URL, modelType));
		} catch (MalformedURLException e) {
			return String.format(SAM2_FNAME, modelType);
		}
	}

	@Override
	public String getModelWeigthPath() {
		File file;
		try {
			file = Paths.get(path, "envs", SAM2_ENV_NAME, SAM2_NAME, "weights", DownloadModel.getFileNameFromURLString(String.format(SAM2_URL, modelType))).toFile();
		} catch (MalformedURLException e) {
			file = Paths.get(path, "envs", SAM2_ENV_NAME, SAM2_NAME, "weights", String.format(SAM2_FNAME, modelType)).toFile();
		}

		return file.getAbsolutePath();
	}

	@Override
	public boolean checkEverythingInstalled() {
		if (!this.checkMambaInstalled()) return false;
		
		if (!this.checkSAMDepsInstalled()) return false;
		
		if (!this.checkModelWeightsInstalled()) return false;
		
		return true;
	}

	@Override
	public void uninstall() {
		if (new File(this.getModelWeigthPath()).getParentFile().list().length != 1)
			Files.deleteFolder(new File(this.getModelWeigthPath()));
		else
			Files.deleteFolder(new File(this.getModelEnv()));
	}
}
