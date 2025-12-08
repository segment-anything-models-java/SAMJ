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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.ArchiveException;

import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.download.FileDownloader;
import io.bioimage.modelrunner.apposed.appose.Mamba;

/**
 * Class that manages the installation of MedSAM together with Python, its corresponding environment
 * and dependencies.
 * 
 * MedSAM is a fine-tuned version of SAM (Segment Anything Model) specifically trained on medical imaging data.
 * 
 * @author SAMJ developers
 */
public class MedSamEnvManager extends SamEnvManagerAbstract {
	
	/**
	 * Name of the environment that contains the code and weights to run MedSAM models
	 */
	final static public String MEDSAM_ENV_NAME = "medsam";
	
	/**
	 * Name of the folder that contains the code and weights for MedSAM models
	 */
	final static public String MEDSAM_NAME = "medsam";
	
	/**
	 * URL to download the MedSAM vit_b model checkpoint
	 * The checkpoint is hosted on Zenodo for reliable, stable downloads
	 * Alternative: Google Drive link at https://drive.google.com/drive/folders/1ETWmi4AiniJeWOt6HAsYgTjYv_fkgzoN
	 */
	final static private String MEDSAM_URL = "https://zenodo.org/api/records/10689643/files/medsam_vit_b.pth/content";
	
	/**
	 * Filename for the MedSAM checkpoint
	 */
	final static private String MEDSAM_FNAME = "medsam_vit_b.pth";
	
	/**
	 * Byte size of the MedSAM model checkpoint (approximately 2.4 GB)
	 */
	final public static long MEDSAM_BYTE_SIZE = 375049145L;
	
	/**
	 * Dependencies to be checked to make sure that the environment is able to load MedSAM.
	 */
	final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {
			"appose", "torch", "torchvision", "segment-anything", "scikit-image"});
	
	/**
	 * Dependencies that have to be installed in any SAMJ created environment using Mamba or Conda
	 */
	final public static List<String> INSTALL_CONDA_DEPS = Arrays.asList(new String[] {
			"scikit-image", "pytorch", "torchvision"});
	
	/**
	 * Dependencies for every environment that need to be installed using PIP
	 */
	final public static List<String> INSTALL_PIP_DEPS = Arrays.asList(new String[] {
			"segment-anything", "pytest"});
	
	private MedSamEnvManager() {
	}
	
	/**
	 * Creates an instance of {@link MedSamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba should be installed
	 * @return an instance of {@link MedSamEnvManager}
	 */
	public static MedSamEnvManager create(String path) {
		return create(path, (ss) -> {});
	}
	
	/**
	 * Creates an instance of {@link MedSamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba should be installed
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link MedSamEnvManager}
	 */
	public static MedSamEnvManager create(String path, Consumer<String> consumer) {
		MedSamEnvManager installer = new MedSamEnvManager();
		installer.path = path;
		installer.consumer = consumer;
		installer.mamba = new Mamba(path);
		return installer;
	}
	
	/**
	 * Creates an instance of {@link MedSamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link MedSamEnvManager}
	 */
	public static MedSamEnvManager create() {
		return create(DEFAULT_DIR);
	}
	
	/**
	 * Creates an instance of {@link MedSamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link MedSamEnvManager}
	 */
	public static MedSamEnvManager create(Consumer<String> consumer) {
		return create(DEFAULT_DIR, consumer);
	}
	
	/**
	 * Check whether the weights for the MedSAM model are installed or not
	 * @return true if the model is installed and false otherwise
	 */
	public boolean checkModelWeightsInstalled() {
		File file = Paths.get(path, "envs", MEDSAM_ENV_NAME, MEDSAM_NAME, "weights", MEDSAM_FNAME).toFile();
		if (file.isFile() && file.length() == MEDSAM_BYTE_SIZE)
			return true;
		return false;
	}
	
	@Override
	public boolean checkSAMDepsInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", MEDSAM_ENV_NAME).toFile();
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
	 * Install the weights of MedSAM vit_b.
	 * @throws IOException if there is any error downloading the weights
	 * @throws InterruptedException if the download of weights is interrupted
	 */
	@Override
	public void installModelWeigths() throws IOException, InterruptedException {
		installModelWeigths(false);
	}
	
	/**
	 * Install the weights of MedSAM vit_b.
	 * @param force
	 * 	whether to overwrite the weights file if it already exists
	 * @throws InterruptedException if the download of weights is interrupted
	 * @throws IOException if there is any error downloading the weights
	 */
	@Override
	public void installModelWeigths(boolean force) throws IOException, InterruptedException {
		if (!force && this.checkModelWeightsInstalled())
			return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING MEDSAM WEIGHTS");
        try {
    		File file = Paths.get(path, "envs", MEDSAM_ENV_NAME, MEDSAM_NAME, "weights", MEDSAM_FNAME).toFile();
    		file.getParentFile().mkdirs();
    		URL url = FileDownloader.redirectedURL(new URL(MEDSAM_URL));
    		Thread parentThread = Thread.currentThread();
    		FileDownloader fd = new FileDownloader(url.toString(), file, false);
    		long size = fd.getOnlineFileSize();
    		Consumer<Double> dConsumer = (d) -> {
    			d = (double) (Math.round(d * 1000) / 10);
    			if (d < 0 || d > 100) passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- MEDSAM WEIGHTS DOWNLOAD: UNKNOWN%");
        		else passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- MEDSAM WEIGHTS DOWNLOAD: " + d + "%");
    		};
    		fd.setPartialProgressConsumer(dConsumer);
    		fd.download(parentThread);
        	if (size > 0 && size != file.length())
        		throw new IOException("Model MedSAM was not correctly downloaded. Expected size: " + size + ", actual: " + file.length());
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MEDSAM WEIGHTS INSTALLATION");
            throw ex;
        } catch (URISyntaxException e1) {
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MEDSAM WEIGHTS INSTALLATION");
            throw new IOException("Unable to find the download URL for MedSAM: " + MEDSAM_URL);
		} catch (ExecutionException e) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MEDSAM WEIGHTS INSTALLATION");
            throw new RuntimeException(e);
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- MEDSAM WEIGHTS INSTALLED");
	}
	
	/**
	 * Install the Python environment and dependencies required to run a MedSAM model.
	 * If Micromamba is not installed in the path of the {@link MedSamEnvManager} instance, this method
	 * installs it.
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installSAMDeps() throws IOException, InterruptedException, MambaInstallException {
		installSAMDeps(false);
	}
	
	/**
	 * Install the Python environment and dependencies required to run a MedSAM model.
	 * If Micromamba is not installed in the path of the {@link MedSamEnvManager} instance, this method
	 * installs it.
	 * @param force
	 * 	if the environment to be created already exists, whether to overwrite it or not
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installSAMDeps(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to install Python without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE MEDSAM PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.10", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		if (!this.checkSAMDepsInstalled() || force) {
			try {
				mamba.create(MEDSAM_ENV_NAME, true, args);
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MEDSAM PYTHON ENVIRONMENT CREATION");
				throw new MambaInstallException("Unable to install Python without first installing Mamba. ");
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MEDSAM PYTHON ENVIRONMENT CREATION");
				throw e;
			}
			ArrayList<String> pipInstall = new ArrayList<String>();
			for (String ss : new String[] {"-m", "pip", "install"}) pipInstall.add(ss);
			for (String ss : INSTALL_PIP_DEPS) pipInstall.add(ss);
			try {
				mamba.runPythonIn(MEDSAM_ENV_NAME, pipInstall.stream().toArray(String[]::new));
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MEDSAM PYTHON DEPENDENCIES INSTALLATION");
				throw e;
			}
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- MEDSAM PYTHON ENVIRONMENT CREATED");
	}
	
	/**
	 * Returns the path to the directory where the MedSAM model weights should be stored
	 * @return the path where the MedSAM model weights directory
	 */
	public String getModelWeightsPath() {
		return new File(path, "envs" + File.separator + MEDSAM_ENV_NAME + File.separator + MEDSAM_NAME + File.separator + "weights").getAbsolutePath();
	}
	
	/**
	 * Returns the path to the MedSAM model checkpoint file
	 * @return the full path to the MedSAM model checkpoint
	 */
	public String getModelWeightsFilePath() {
		return Paths.get(getModelWeightsPath(), MEDSAM_FNAME).toString();
	}
	
	@Override
	public String getModelWeigthPath() {
		return getModelWeightsFilePath();
	}
	
	@Override
	public String getModelWeigthsName() {
		return MEDSAM_FNAME;
	}
	
	@Override
	public String getModelEnv() {
		File file = Paths.get(path, "envs", MEDSAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	public String getEnvName() {
		return MEDSAM_ENV_NAME;
	}
	
	@Override
	public boolean checkEverythingInstalled() {
		return checkMambaInstalled() && checkSAMDepsInstalled() && checkModelWeightsInstalled();
	}
	
	@Override
	public void installEverything() throws IOException, InterruptedException, ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		if (!this.checkSAMDepsInstalled()) this.installSAMDeps();
		if (!this.checkModelWeightsInstalled()) this.installModelWeigths();
	}
	
	@Override
	public void uninstall() {
		File weightsFile = new File(this.getModelWeigthPath());
		if (weightsFile.exists()) {
			weightsFile.delete();
		}
		File weightsDir = weightsFile.getParentFile();
		if (weightsDir != null && weightsDir.exists() && weightsDir.list().length == 0) {
			weightsDir.delete();
		}
	}
}
