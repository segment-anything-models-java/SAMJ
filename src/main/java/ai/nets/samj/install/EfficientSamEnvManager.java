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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.gui.tools.Files;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;

/*
 * Class that is manages the installation of SAM and EfficientSAM together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class EfficientSamEnvManager extends SamEnvManagerAbstract {
	/**
	 * Name of the file that contains the weights for EfficientSAM small
	 */
	final public static String ESAM_SMALL_WEIGHTS_NAME ="efficient_sam_vits.pt";
	/**
	 * Dependencies to be checked to make sure that the environment is able to load a SAM based model. 
	 * General for every supported model.
	 */
	// TODO update final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch=2.0.1", "torchvision=0.15.2", "skimage", "mkl=2024.0.0"});
	final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch", "torchvision", "skimage"});
	/**
	 * Dependencies that have to be installed in any SAMJ created environment using Mamba or Conda
	 */
	final public static List<String> INSTALL_CONDA_DEPS;
	static {
		if (!PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64) && !PlatformDetection.isUsingRosseta())
			INSTALL_CONDA_DEPS = Arrays.asList(new String[] {"libpng", "libjpeg-turbo", 
				"scikit-image", "pytorch=2.0.1", "torchvision=0.15.2", "cpuonly"});
		else 
			INSTALL_CONDA_DEPS = Arrays.asList(new String[] {"libpng", "libjpeg-turbo", 
					"scikit-image", "pytorch=2.0.1", "torchvision=0.15.2", "cpuonly"});
	}
	/**
	 * Dependencies for every environment that need to be installed using PIP
	 */
	final public static List<String> INSTALL_PIP_DEPS;
	static {
		if (!PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64) && !PlatformDetection.isUsingRosseta())
			INSTALL_PIP_DEPS = Arrays.asList(new String[] {"mkl=2023.2.2", "appose"});
		else 
			INSTALL_PIP_DEPS = Arrays.asList(new String[] {"appose"});
	}
	/**
	 * Byte size of the weights of EfficientSAM Small
	 */
	final public static long EFFICIENTSAM_BYTE_SIZE = 105742022 ;
	/**
	 * Name of the environment that contains the code and weigths to run EfficientSAM models
	 */
	final static public String ESAM_ENV_NAME = "efficient_sam_env";
	/**
	 * Name of the folder that contains the code and weigths for EfficientSAM models
	 */
	final static public String ESAM_NAME = "EfficientSAM";
	/**
	 * URL to download the EfficientSAM model 
	 */
	final static public String ESAMS_URL = "https://raw.githubusercontent.com/yformer/EfficientSAM/main/weights/efficient_sam_vits.pt.zip";
	
	/**
	 * Creates an instance of {@link EfficientSamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @return an instance of {@link EfficientSamEnvManager}
	 */
	public static EfficientSamEnvManager create(String path) {
		return create(path, (ss) -> {});
	}
	
	/**
	 * Creates an instance of {@link EfficientSamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link EfficientSamEnvManager}
	 */
	public static EfficientSamEnvManager create(String path, Consumer<String> consumer) {
		EfficientSamEnvManager installer = new EfficientSamEnvManager();
		installer.path = path;
		installer.consumer = consumer;
		installer.mamba = new Mamba(path);
		return installer;
	}
	
	/**
	 * Creates an instance of {@link EfficientSamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link EfficientSamEnvManager}
	 */
	public static EfficientSamEnvManager create() {
		return create(DEFAULT_DIR);
	}
	
	/**
	 * Creates an instance of {@link EfficientSamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link EfficientSamEnvManager}
	 */
	public static EfficientSamEnvManager create(Consumer<String> consumer) {
		return create(DEFAULT_DIR, consumer);
	}
	
	/**
	 * Check whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not. The environment folder should be named {@value #COMMON_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not
	 */
	public boolean checkSAMDepsInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", ESAM_ENV_NAME).toFile();
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
	 * Check whether the Python package to run EfficientSAM has been installed. The package will be in the folder
	 * {@value #ESAM_ENV_NAME}. The Python executable and other dependencies will be at {@value #COMMON_ENV_NAME}
	 * @return whether the Python package to run EfficientSAM has been installed.
	 */
	public boolean checkEfficientSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME).toFile();
		if (!pythonEnv.exists() || pythonEnv.list().length <= 1) return false;
		return true;
	}
	
	/**
	 * 
	 * @return whether the weights needed to run EfficientSAM Small (the standard EfficientSAM) have been 
	 * downloaded and installed or not
	 */
	public boolean checkModelWeightsInstalled() {
		File weightsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		if (!weightsFile.isFile()) return false;
		if (weightsFile.length() != EFFICIENTSAM_BYTE_SIZE) return false;
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
		if (!force && checkModelWeightsInstalled())
			return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTSAM WEIGHTS");
		String zipResourcePath = "efficient_sam_vits.pt.zip";
        String outputDirectory = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile().getAbsolutePath();
        try (
        	InputStream zipInputStream = EfficientSamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
                	if (Thread.interrupted()) throw new InterruptedException("EfficientSAM download interrupted");
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = zipInput.read(buffer)) != -1) {
                        entryOutput.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTSAM WEIGHTS INSTALLATION");
            throw ex;
        }
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTSAM WEIGHTS INSTALLED");
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EfficientSAM model.
	 * If Micromamba is not installed in the path of the {@link EfficientSamEnvManager} instance, this method
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
	 * If Micromamba is not installed in the path of the {@link EfficientSamEnvManager} instance, this method
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
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		if (!checkSAMDepsInstalled() || force) {
			try {
				mamba.create(ESAM_ENV_NAME, true, args);
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION");
				throw new MambaInstallException("Unable to install Python without first installing Mamba. ");
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION");
				throw e;
			}
			ArrayList<String> pipInstall = new ArrayList<String>();
			for (String ss : new String[] {"-m", "pip", "install"}) pipInstall.add(ss);
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- PYTHON ENVIRONMENT CREATED");
        // TODO remove
        installApposePackage(ESAM_ENV_NAME);
		installEfficientSAMPackage(force);
	}
	
	/**
	 * Install the Python package to run EfficientSAM.
	 * Does not overwrite the package if it already exists.
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	public void installEfficientSAMPackage() throws IOException, InterruptedException, MambaInstallException {
		installEfficientSAMPackage(false);
	}
	
	/**
	 * Install the Python package to run EfficientSAM
	 * @param force
	 * 	if the package already exists, whether to overwrite it or not
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	public void installEfficientSAMPackage(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (checkEfficientSAMPackageInstalled() && !force)
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to EfficientSAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'EFFICIENTSAM' PYTHON PACKAGE");
		try {
			mamba.create(ESAM_ENV_NAME, true);
		} catch (MambaInstallException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTSAM' PYTHON PACKAGE INSTALLATION");
			throw new MambaInstallException("Unable to install EfficientSAM without first installing Mamba.");
		} catch (IOException | InterruptedException | RuntimeException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTSAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		String zipResourcePath = "EfficientSAM.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + ESAM_ENV_NAME;
        try (
        	InputStream zipInputStream = EfficientSamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTSAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'EFFICIENTSAM' PYTHON PACKAGE INSATLLED");
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
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installEfficientSAMPackage();
		
		if (!this.checkModelWeightsInstalled()) this.installModelWeigths();
	}
	
	/**
	 * 
	 * @return the path to the EfficientSAM Small weights file
	 */
	public String getModelWeightsPath() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment needed to run EfficientSAM
	 */
	public String getModelEnv() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the official name of the EfficientSAM Small weights
	 */
	public String getModelWeigthsName() {
		return ESAM_SMALL_WEIGHTS_NAME;
	}

	@Override
	public String getModelWeigthPath() {
		return Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toAbsolutePath().toString();
	}

	@Override
	public boolean checkEverythingInstalled() {
		if (!this.checkMambaInstalled()) return false;
		
		if (!this.checkSAMDepsInstalled()) return false;
		
		if (!this.checkEfficientSAMPackageInstalled()) return false;
		
		if (!this.checkModelWeightsInstalled()) return false;
		
		return true;
	}

	@Override
	public void uninstall() {
		Files.deleteFolder(new File(this.getModelEnv()));
	}
}
