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
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.gui.tools.Files;
import ai.nets.samj.models.EfficientTamJ;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.download.FileDownloader;

/*
 * Class that is manages the installation of SAM and EFFICIENTTAM together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class EfficientTamEnvManager extends SamEnvManagerAbstract {
	
	private final String modelType;
	/**
	 * Default version for the family of EFFICIENTTAM models
	 */
	final public static String DEFAULT = "tiny";

	/**
	 * Dependencies to be checked to make sure that the environment is able to load a SAM based model. 
	 * General for every supported model.
	 */
	final public static List<String> CHECK_DEPS = Sam2EnvManager.CHECK_DEPS;
	/**
	 * Dependencies that have to be installed in any SAMJ created environment using Mamba or Conda
	 */
	final public static List<String> INSTALL_CONDA_DEPS = Sam2EnvManager.INSTALL_CONDA_DEPS;
	/**
	 * Dependencies for every environment that need to be installed using PIP
	 */
	final public static List<String> INSTALL_PIP_DEPS = Sam2EnvManager.INSTALL_PIP_DEPS;
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
	 * Name of the environment that contains the code and weigths to run EFFICIENTTAM models
	 */
	final static public String EFFTAM_ENV_NAME = Sam2EnvManager.SAM2_ENV_NAME;
	/**
	 * Name of the folder that contains the code and weigths for EFFICIENTTAM models
	 */
	final static public String EFFTAM_NAME = "EfficientTAM";
	/**
	 * URL to download the EFFICIENTTAM model 
	 */
	final static private String EFFTAM_URL = "https://huggingface.co/yunyangx/efficient-track-anything/resolve/main/efficienttam_%s.pt?download=true";
	/**
	 * URL to download the EFFICIENTTAM model 
	 */
	final static private String EFFTAM_FNAME = "efficienttam_%s.pt";
	
	private EfficientTamEnvManager(String modelType) {
		List<String> modelTypes = EFFTAM_BYTE_SIZES_MAP.keySet().stream().collect(Collectors.toList());
		if (!modelTypes.contains(modelType) && !modelType.equals("base")) {
			throw new IllegalArgumentException("Invalid model variant chosen: '" + modelType + "'."
					+ "The only supported variants are: " + modelTypes);
		}
		if (modelType.equals("base"))
			modelType = "base_plus";
		this.modelType = modelType;
	}
	
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param modelType
	 * 	which of the possible EFFICIENTTAM wants to be used. The possible variants are the keys of the following map: {@link #EFFTAM_BYTE_SIZES_MAP}
	 * @return an instance of {@link EfficientTamEnvManager}
	 */
	public static EfficientTamEnvManager create(String path, String modelType) {
		return create(path, modelType == null ? DEFAULT : modelType, (ss) -> {});
	}
	
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param modelType
	 * 	which of the possible EFFICIENTTAM wants to be used. The possible variants are the keys of the following map: {@link #EFFTAM_BYTE_SIZES_MAP}
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link EfficientTamEnvManager}
	 */
	public static EfficientTamEnvManager create(String path, String modelType, Consumer<String> consumer) {
		EfficientTamEnvManager installer = new EfficientTamEnvManager(modelType);
		installer.path = path;
		installer.consumer = consumer;
		installer.mamba = new Mamba(path);
		return installer;
	}
	
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}. Uses the default model {@link #DEFAULT}
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link EfficientTamEnvManager}
	 */
	public static EfficientTamEnvManager create() {
		return create(DEFAULT_DIR, null);
	}
	
	/**
	 * Creates an instance of {@link EfficientTamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link EfficientTamEnvManager}
	 */
	public static EfficientTamEnvManager create(Consumer<String> consumer) {
		return create(DEFAULT_DIR, null, consumer);
	}
	
	/**
	 * 
	 * @return which of the possible EFFICIENTTAM this is. The possible variants are the keys of the following map: {@link #EFFTAM_BYTE_SIZES_MAPf}
	 */
	public String getModelType() {
		return this.modelType;
	}
	
	/**
	 * Check whether the Python environment with the corresponding packages needed to run EFFICIENTTAM
	 * has been installed or not. The environment folder should be named {@value #EFFTAM_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run EFFICIENTTAM
	 * has been installed or not
	 */
	public boolean checkSAMDepsInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", EFFTAM_ENV_NAME).toFile();
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
	 * Check whether the Python package to run EfficientTAM has been installed. The package will be in the folder
	 * {@value #ESAM_ENV_NAME}. The Python executable and other dependencies will be at {@value #ESAM_ENV_NAME}
	 * @return whether the Python package to run EfficientTAM has been installed.
	 */
	private boolean checkEfficientTAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", EFFTAM_ENV_NAME, EFFTAM_NAME).toFile();
		if (!pythonEnv.exists() || pythonEnv.list().length <= 1) return false;
		return true;
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
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTTAM WEIGHTS (" + modelType + ")");
        try {
    		File file = Paths.get(path, "envs", EFFTAM_ENV_NAME, EFFTAM_NAME, "weights", FileDownloader.getFileNameFromURLString(String.format(EFFTAM_URL, modelType))).toFile();
    		file.getParentFile().mkdirs();
    		URL url = FileDownloader.redirectedURL(new URL(String.format(EFFTAM_URL, EfficientTamJ.abbreviateModelType(modelType))));
    		Thread parentThread = Thread.currentThread();
    		FileDownloader fd = new FileDownloader(url.toString(), file, false);
    		long size = fd.getOnlineFileSize();
    		Consumer<Double> dConsumer = (d) -> {
    			d = (double) (Math.round(d * 1000) / 10);
    			if (d < 0 || d > 100) passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTTAM WEIGHTS DOWNLOAD: UNKNOWN%");
        		else passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTTAM WEIGHTS DOWNLOAD: " + d + "%");
    		};
    		fd.setPartialProgressConsumer(dConsumer);
    		fd.download(parentThread);    		    		
        	if (size != file.length())
        		throw new IOException("Model EFFICIENTTAM" + modelType + " was not correctly downloaded");
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTTAM WEIGHTS INSTALLATION");
            throw ex;
        } catch (URISyntaxException e1) {
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTTAM WEIGHTS INSTALLATION");
            throw new IOException("Unable to find the download URL for EFFICIENTTAM " + modelType + ": " + String.format(EFFTAM_URL, EfficientTamJ.abbreviateModelType(modelType)));
		} catch (ExecutionException e) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTTAM WEIGHTS INSTALLATION");
            throw new RuntimeException(e);
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTTAM WEIGHTS INSTALLED");
	}
	
	private void extractWeights() throws InterruptedException, IOException {
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTTAM WEIGHTS");
		String zipResourcePath = "efficienttam_ti.zip";
        String outputDirectory = Paths.get(path, "envs", EFFTAM_ENV_NAME, EFFTAM_NAME, "weights").toFile().getAbsolutePath();
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
                	if (Thread.interrupted()) throw new InterruptedException("EfficientTAM download interrupted");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInput.read(buffer)) != -1) {
                        entryOutput.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTTAM WEIGHTS INSTALLATION");
            throw ex;
        }
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTTAM WEIGHTS INSTALLED");
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EFFICIENTTAM model.
	 * If Micromamba is not installed in the path of the {@link EfficientTamEnvManager} instance, this method
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
	 * Install the Python environment and dependencies required to run an EFFICIENTTAM model.
	 * If Micromamba is not installed in the path of the {@link EfficientTamEnvManager} instance, this method
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
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE EFFICIENTTAM PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		if (!this.checkSAMDepsInstalled() || force) {
			try {
				mamba.create(EFFTAM_ENV_NAME, true, args);
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTTAM PYTHON ENVIRONMENT CREATION");
				throw new MambaInstallException("Unable to install Python without first installing Mamba. ");
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTTAM PYTHON ENVIRONMENT CREATION");
				throw e;
			}
			ArrayList<String> pipInstall = new ArrayList<String>();
			for (String ss : new String[] {"-m", "pip", "install"}) pipInstall.add(ss);
			for (String ss : INSTALL_PIP_DEPS) pipInstall.add(ss);
			try {
				Mamba.runPythonIn(Paths.get(path,  "envs", EFFTAM_ENV_NAME).toFile(), pipInstall.stream().toArray( String[]::new ));
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION WHEN INSTALLING PIP DEPENDENCIES");
				throw e;
			}
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTTAM PYTHON ENVIRONMENT CREATED");
        installApposePackage(EFFTAM_ENV_NAME);
	}
	
	/**
	 * Install the Python package to run EfficientTAM
	 * @param force
	 * 	if the package already exists, whether to overwrite it or not
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	private void installEfficientTAMPackage() throws IOException, InterruptedException, MambaInstallException {
		if (checkEfficientTAMPackageInstalled())
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to EfficientTAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'EFFICIENTTAM' PYTHON PACKAGE");
		String zipResourcePath = "EfficientTAM.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + EFFTAM_ENV_NAME;
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
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInput.read(buffer)) != -1) {
                        entryOutput.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTTAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'EFFICIENTTAM' PYTHON PACKAGE INSATLLED");
	}
	
	/**
	 * Install all the requirements to run EFFICIENTTAM. First, checks if micromamba is installed, if not installs it;
	 * then checks if the Python environment and packages needed to run EFFICIENTTAM are installed and if not installs it
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
		
		if (!this.checkEfficientTAMPackageInstalled()) this.installEfficientTAMPackage();
		
		if (!this.checkModelWeightsInstalled()) this.installModelWeigths();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment needed to run EFFICIENTTAM
	 */
	public String getModelEnv() {
		File file = Paths.get(path, "envs", EFFTAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
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
			file = Paths.get(path, "envs", EFFTAM_ENV_NAME, EFFTAM_NAME, "weights", FileDownloader.getFileNameFromURLString(String.format(EFFTAM_URL, EfficientTamJ.abbreviateModelType(modelType)))).toFile();
		} catch (MalformedURLException e) {
			file = Paths.get(path, "envs", EFFTAM_ENV_NAME, EFFTAM_NAME, "weights", String.format(EFFTAM_FNAME, EfficientTamJ.abbreviateModelType(modelType))).toFile();
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
