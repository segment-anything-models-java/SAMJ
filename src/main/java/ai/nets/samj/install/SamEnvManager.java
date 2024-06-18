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
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.bioimageio.download.DownloadModel;
import io.bioimage.modelrunner.engine.installation.FileDownloader;
import io.bioimage.modelrunner.system.PlatformDetection;
import io.bioimage.modelrunner.utils.CommonUtils;

import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.models.EfficientViTSamJ;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;
import io.bioimage.modelrunner.apposed.appose.MambaInstallerUtils;

/*
 * Class that is manages the installation of SAM and EfficientSAM together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class SamEnvManager {
	/**
	 * Name of the file that contains the weights of SAM Huge
	 */
	final public static String SAM_WEIGHTS_NAME = "sam_vit_h_4b8939.pth";
	/**
	 * Name of the file that contains the weights for EfficientSAM small
	 */
	final public static String ESAM_SMALL_WEIGHTS_NAME ="efficient_sam_vits.pt";
	/**
	 * Name of the encoder (ViT Huge) that is going to be used for SAM in SAMJ
	 */
	final public static String SAM_MODEL_TYPE = "vit_h";
	/**
	 * Default version for the family of EfficientViTSAM models
	 */
	final public static String DEFAULT_EVITSAM = "l0";
	/**
	 * Dependencies to be checked to make sure that the environment is able to load a SAM based model. 
	 * General for every supported model.
	 */
	// TODO update final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch=2.0.1", "torchvision=0.15.2", "skimage", "mkl=2024.0.0"});
	final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch", "torchvision", "skimage"});
	/**
	 * Dependencies to be checked to make sure that an environment can run an EfficientViTSAM model
	 */
	final public static List<String> CHECK_DEPS_EVSAM = Arrays.asList(new String[] {"onnxsim", "timm", "onnx", "segment_anything"});
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
	 * Dependencies that have to be installed using Mamba or Conda in environments that are going
	 * to be used to run EfficientViTSAM
	 */
	final public static List<String> INSTALL_EVSAM_CONDA_DEPS = Arrays.asList(new String[] {"cmake", "onnx", "onnxruntime", "timm=0.6.13"});
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
	 * Dependencies for EfficientViTSAM environments that need to be installed using PIP
	 */
	final public static List<String> INSTALL_EVSAM_PIP_DEPS = Arrays.asList(new String[] {"onnxsim", "segment_anything"});
	/**
	 * Byte size of the weights of SAM Huge
	 */
	final public static long SAM_BYTE_SIZE = 375042383;
	/**
	 * Byte size of the weights of EfficientSAM Small
	 */
	final public static long EFFICIENTSAM_BYTE_SIZE = 105742022 ;
	/**
	 * Byte sizes of all the EfficientViTSAM options
	 */
	final public static HashMap<String, Long> EFFICIENTVITSAM_BYTE_SIZES_MAP;
	static {
		EFFICIENTVITSAM_BYTE_SIZES_MAP = new HashMap<String, Long>();
		EFFICIENTVITSAM_BYTE_SIZES_MAP.put("l0", (long) 139410184);
		EFFICIENTVITSAM_BYTE_SIZES_MAP.put("l1", (long) 190916834);
		EFFICIENTVITSAM_BYTE_SIZES_MAP.put("l2", (long) 245724244);
		EFFICIENTVITSAM_BYTE_SIZES_MAP.put("xl0", (long) 468211343);
		EFFICIENTVITSAM_BYTE_SIZES_MAP.put("xl1", (long) 814022923);
	}
	/**
	 * Name that will be given to the environment that contains the dependencies needed to load EfficientSAM or SAM
	 */
	final static public String COMMON_ENV_NAME = "sam_common_env";
	/**
	 * Name of the environment that contains the code and weigths to run EfficientSAM models
	 */
	final static public String ESAM_ENV_NAME = "efficient_sam_env";
	/**
	 * Name of the environment that contains the code, dependencies and weigths to load EfficientViTSAM models
	 */
	final static public String EVITSAM_ENV_NAME = "efficientvit_sam_env";
	/**
	 * Name of the environment that contains the code and weights to load SAM
	 */
	final static public String SAM_ENV_NAME = "sam_env";
	/**
	 * Name of the folder that contains the code and weigths for EfficientSAM models
	 */
	final static public String ESAM_NAME = "EfficientSAM";
	/**
	 * Name of the folder that contains the code and weigths for EfficientViTSAM models
	 */
	final static public String EVITSAM_NAME = "efficientvit";
	/**
	 * Name of the folder that contains the code and weigths for SAM models
	 */
	final static public String SAM_NAME = "SAM";
	/**
	 * Name of the folder that contains the code of Appose in Python
	 */
	final static public String APPOSE = "appose-python";
	/**
	 * URL to download the EfficientSAM model 
	 */
	final static public String ESAMS_URL = "https://raw.githubusercontent.com/yformer/EfficientSAM/main/weights/efficient_sam_vits.pt.zip";
	/**
	 * URL to download the EfficientViTSAM model. It needs to be used with String.format(EVITSAM_URL, "l0"), whre l0 could be any of 
	 * the existing EfficientVitSAM model 
	 */
	final static private String EVITSAM_URL = "https://huggingface.co/han-cai/efficientvit-sam/resolve/main/%s.pt?download=true";
	/**
	 * Default directory where micromamba is installed and where all the environments are created
	 */
	final static public String DEFAULT_DIR = new File("appose_x86_64").getAbsolutePath();
	/**
	 * Date format
	 */
	private static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	/**
	 * Variable used to measer time intervals
	 */
	private static long millis = System.currentTimeMillis();
	/**
	 * Relative path to the mamba executable from the appose folder
	 */
	private final static String MAMBA_RELATIVE_PATH = PlatformDetection.isWindows() ? 
			 File.separator + "Library" + File.separator + "bin" + File.separator + "micromamba.exe" 
			: File.separator + "bin" + File.separator + "micromamba";	
	/**
	 * Path where Micromamba is going to be installed
	 */
	private String path;
	/**
	 * {@link Mamba} instance used to create the environments
	 */
	private Mamba mamba;
	/**
	 * Consumer used to keep providing info in the case of several threads working
	 */
	private Consumer<String> consumer;
	
	/**
	 * Creates an instance of {@link SamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @return an instance of {@link SamEnvManager}
	 */
	public static SamEnvManager create(String path) {
		return create(path, (ss) -> {});
	}
	
	/**
	 * Creates an instance of {@link SamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link SamEnvManager}
	 */
	public static SamEnvManager create(String path, Consumer<String> consumer) {
		SamEnvManager installer = new SamEnvManager();
		installer.path = path;
		installer.consumer = consumer;
		installer.mamba = new Mamba(path);
		return installer;
	}
	
	/**
	 * Creates an instance of {@link SamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link SamEnvManager}
	 */
	public static SamEnvManager create() {
		return create(DEFAULT_DIR);
	}
	
	/**
	 * Creates an instance of {@link SamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link SamEnvManager}
	 */
	public static SamEnvManager create(Consumer<String> consumer) {
		return create(DEFAULT_DIR, consumer);
	}
	
	/**
	 * Send information as Strings to the consumer
	 * @param str
	 * 	String that is going to be sent to the consumer
	 */
	private void passToConsumer(String str) {
		consumer.accept(str);
		millis = System.currentTimeMillis();
	}
	
	/**
	 * Check whether micromamba is installed or not in the directory of the {@link SamEnvManager} instance.
	 * @return whether micromamba is installed or not in the directory of the {@link SamEnvManager} instance.
	 */
	public boolean checkMambaInstalled() {
		File ff = new File(path + MAMBA_RELATIVE_PATH);
		if (!ff.exists()) return false;
		return mamba.checkMambaInstalled();
	}
	
	/**
	 * Check whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not. The environment folder should be named {@value #COMMON_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not
	 */
	public boolean checkEfficientSAMPythonInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", COMMON_ENV_NAME).toFile();
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
	 * Check whether the Python environment with the corresponding packages needed to run an EfficientViTSAM
	 * model has been installed or not. The environment folder should be named {@value #EVITSAM_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not
	 */
	public boolean checkEfficientViTSAMPythonInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", EVITSAM_ENV_NAME).toFile();
		if (!pythonEnv.exists()) return false;
		
		List<String> uninstalled;
		try {
			uninstalled = mamba.checkUninstalledDependenciesInEnv(pythonEnv.getAbsolutePath(), CHECK_DEPS_EVSAM);
		} catch (MambaInstallException e) {
			return false;
		}
		
		return uninstalled.size() == 0;
	}
	
	/**
	 * Check whether the Python package to run SAM has been installed. The package will be in the folder
	 * {@value #SAM_ENV_NAME}. The Python executable and other dependencies will be at {@value #COMMON_ENV_NAME}
	 * @return whether the Python package to run SAM has been installed.
	 */
	public boolean checkSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", SAM_ENV_NAME, SAM_NAME).toFile();
		if (!pythonEnv.exists() || pythonEnv.list().length <= 1) return false;
		return true;
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
	 * Check whether the Python package to run an EfficientViTSAM model has been installed. The package will be in the folder
	 * {@value #EVITSAM_NAME} inside of {@value #EVITSAM_ENV_NAME}. The Python executable and other dependencies will be
	 * in the same environment {@value #EVITSAM_ENV_NAME}
	 * @return whether the Python package to run an EfficientViTSAM model has been installed.
	 */
	public boolean checkEfficientViTSAMPackageInstalled() {
		if (!checkMambaInstalled()) return false;
		File pythonEnv = Paths.get(this.path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME).toFile();
		if (!pythonEnv.exists() || pythonEnv.list().length <= 1) return false;
		return true;
	}
	
	/**
	 * 
	 * @return whether the weights needed to run EfficientSAM Small (the standard EfficientSAM) have been 
	 * downloaded and installed or not
	 */
	public boolean checkEfficientSAMSmallWeightsDownloaded() {
		File weightsFile = Paths.get(this.path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		if (!weightsFile.isFile()) return false;
		if (weightsFile.length() != EFFICIENTSAM_BYTE_SIZE) return false;
		return true;
	}

	/**
	 * Check whether the weight file needed to run one of the pretrained EfficientViTSAM has been installed 
	 * or not.
	 * @param modelType
	 * 	the model type. The available model types are "l0", "l1", "l2", "xl0" and "xl1"
	 * @return whether the weight file needed to run one of the pretrained EfficientViTSAM has been installed 
	 * or not.
	 */
	public boolean checkEfficientViTSAMWeightsDownloaded(String modelType) {
		if (!EfficientViTSamJ.getListOfSupportedEfficientViTSAM().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientViT models: " 
												+ EfficientViTSamJ.getListOfSupportedEfficientViTSAM());
		File weightsFile = Paths.get(this.path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", modelType + ".pt").toFile();
		if (!weightsFile.isFile()) return false;
		if (weightsFile.length() != EFFICIENTVITSAM_BYTE_SIZES_MAP.get(modelType)) return false;
		return true;
	}
	
	/**
	 * 
	 * @return whether the weights needed to run SAM Huge have been 
	 * downloaded and installed or not
	 */
	public boolean checkSAMWeightsDownloaded() {
		File weigthsFile = Paths.get(this.path, "envs", SAM_ENV_NAME, SAM_NAME, "weights", SAM_WEIGHTS_NAME).toFile();
		return weigthsFile.isFile();
	}
	
	/**
	 * Download and install all the weights of SAM Huge.
	 */
	public void downloadSAMWeigths() {
		downloadSAMWeigths(false);
	}
	
	/**
	 * Download and install the weights of SAM Huge.
	 * @param force
	 * 	whether to overwrite the weights file if it already exists
	 */
	public void downloadSAMWeigths(boolean force) {
		if (!force && checkSAMWeightsDownloaded())
			return;
	}
	
	/**
	 * Install the weights of EfficientSAM Small.
	 * Does not overwrite the weights file if it already exists.
	 */
	public void downloadESAMSmallWeights() throws IOException, InterruptedException {
		downloadESAMSmallWeights(false);
	}
	
	/**
	 * Install the weights of EfficientSAM Small.
	 * @param force
	 * 	whether to overwrite the weights file if it already exists
	 * @throws InterruptedException if the download of weights is interrupted
	 */
	public void downloadESAMSmallWeights(boolean force) throws IOException, InterruptedException {
		if (!force && checkEfficientSAMSmallWeightsDownloaded())
			return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTSAM WEIGHTS");
		String zipResourcePath = "efficient_sam_vits.pt.zip";
        String outputDirectory = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights").toFile().getAbsolutePath();
        try (
        	InputStream zipInputStream = SamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
	 * Download and install the weights of EfficientViTSAM. This method always downloads "l0" model,
	 * to download another model please use {@link #downloadEfficientViTSAMWeights(String)}
	 * @throws IOException if there is any issue downloading the weights file
	 * @throws InterruptedException if the download of the weights is interrupted
	 */
	public void downloadEfficientViTSAMWeights() throws IOException, InterruptedException {
		downloadEfficientViTSAMWeights(DEFAULT_EVITSAM, false);
	}
	/**
	 * Download and install the weights of EfficientViTSAM. This method always downloads "l0" model,
	 * to download another model please use {@link #downloadEfficientViTSAMWeights(String, boolean)}
	 * @param force
	 * 	whether to overwrite the existing weights file or not
	 * @throws IOException if there is any issue downloading the weights file
	 * @throws InterruptedException if the download of the weights is interrupted
	 */
	public void downloadEfficientViTSAMWeights(boolean force) throws IOException, InterruptedException {
		downloadEfficientViTSAMWeights(DEFAULT_EVITSAM, force);
	}
	
	/**
	 * Download and install the weights of the wanted EfficientViTSAM model. 
	 * @param modelType
	 * 	the type of model to be downloaded. The available models are "l0", "l1", "l2", "xl0" and "xl1"
	 * @throws IOException if there is any issue downloading the weights file
	 * @throws InterruptedException if the download of the weights is interrupted
	 */
	public void downloadEfficientViTSAMWeights(String modelType) throws IOException, InterruptedException {
		downloadEfficientViTSAMWeights(modelType, false);
	}
	
	/**
	 * Download and install the weights of the wanted EfficientViTSAM model. 
	 * @param modelType
	 * 	the type of model to be downloaded. The available models are "l0", "l1", "l2", "xl0" and "xl1"
	 * @param force
	 * 	whether to overwrite the existing weights file or not
	 * @throws IOException if there is any issue downloading the weights file
	 * @throws InterruptedException if the download of the weights is interrupted
	 */
	public void downloadEfficientViTSAMWeights(String modelType, boolean force) throws IOException, InterruptedException {
		if (!EfficientViTSamJ.getListOfSupportedEfficientViTSAM().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientViT models: " 
												+ EfficientViTSamJ.getListOfSupportedEfficientViTSAM());
		if (!force && checkEfficientViTSAMWeightsDownloaded(modelType))
			return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING EFFICIENTVITSAM WEIGHTS (" + modelType + ")");
        try {
    		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", DownloadModel.getFileNameFromURLString(String.format(EVITSAM_URL, modelType))).toFile();
    		file.getParentFile().mkdirs();
    		URL url = MambaInstallerUtils.redirectedURL(new URL(String.format(EVITSAM_URL, modelType)));
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
        		if (progress < 0 || progress > 100) passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM WEIGHTS DOWNLOAD: UNKNOWN%");
        		else passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM WEIGHTS DOWNLOAD: " + progress + "%");
        	}
        	if (size != file.length())
        		throw new IOException("Model EfficientViTSAM-" + modelType + " was not correctly downloaded");
        } catch (IOException ex) {
            thread.interrupt();
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM WEIGHTS INSTALLATION");
            throw ex;
        } catch (URISyntaxException e1) {
            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM WEIGHTS INSTALLATION");
            throw new IOException("Unable to find the download URL for EfficientViTSAM " + modelType + ": " + String.format(EVITSAM_URL, modelType));

		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM WEIGHTS INSTALLED");
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EfficientSAM model.
	 * If Micromamba is not installed in the path of the {@link SamEnvManager} instance, this method
	 * installs it.
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer files
	 * @throws URISyntaxException if there is any error witht the URL to download micromamba
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installEfficientSAMPython() throws IOException, InterruptedException, ArchiveException, URISyntaxException, MambaInstallException {
		installEfficientSAMPython(false);
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EfficientSAM model.
	 * If Micromamba is not installed in the path of the {@link SamEnvManager} instance, this method
	 * installs it.
	 * @param force
	 * 	if the environment to be created already exists, whether to overwrite it or not
	 * 
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installEfficientSAMPython(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to install Python without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		if (!checkEfficientSAMPythonInstalled() || force) {
			try {
				mamba.create(COMMON_ENV_NAME, true, args);
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
			// TODO until appose new release for (String ss : INSTALL_PIP_DEPS) pipInstall.add(ss);
			/*
			try {
				Mamba.runPythonIn(Paths.get(path,  "envs", COMMON_ENV_NAME).toFile(), pipInstall.stream().toArray( String[]::new ));
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION WHEN INSTALLING PIP DEPENDENCIES");
				throw e;
			}
			*/
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- PYTHON ENVIRONMENT CREATED");
        // TODO remove
        installApposePackage(COMMON_ENV_NAME);
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EfficientViTSAM model.
	 * If Micromamba is not installed in the path of the {@link SamEnvManager} instance, this method
	 * installs it.
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws ArchiveException if there is any downloading Micromamba
	 * @throws URISyntaxException if there is any error witht the URL to download micromamba
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installEfficientViTSAMPython() throws IOException, InterruptedException, ArchiveException, URISyntaxException, MambaInstallException {
		installEfficientViTSAMPython(false);
	}
	
	/**
	 * Install the Python environment and dependencies required to run an EfficientViTSAM model.
	 * If Micromamba is not installed in the path of the {@link SamEnvManager} instance, this method
	 * installs it.
	 * @param force
	 * 	if the environment to be created already exists, whether to overwrite it or not
	 * 
	 * @throws IOException if there is any file error installing any of the requirements
	 * @throws InterruptedException if the installation is interrupted
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installEfficientViTSAMPython(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to install Python without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE EFFICIENTVITSAM PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size() + INSTALL_EVSAM_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		for (String ss : INSTALL_EVSAM_CONDA_DEPS) args[c ++] = ss;
		if (!checkEfficientViTSAMPythonInstalled() || force) {
			try {
				mamba.create(EVITSAM_ENV_NAME, true, args);
			} catch (MambaInstallException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM PYTHON ENVIRONMENT CREATION");
				throw new MambaInstallException("Unable to install Python without first installing Mamba. ");
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM PYTHON ENVIRONMENT CREATION");
				throw e;
			}
			try {
				installOnnxsim(Paths.get(path, "envs", EVITSAM_ENV_NAME).toFile() );
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED EFFICIENTVITSAM PYTHON ENVIRONMENT CREATION WHEN INSTALLING PIP DEPENDENCIES");
				throw e;
			}
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM PYTHON ENVIRONMENT CREATED");
        // TODO remove
        installApposePackage(EVITSAM_ENV_NAME);
	}
	
	// TODO move this to mamba
	private void installOnnxsim(File envFile) throws IOException, InterruptedException {
		final List< String > cmd = new ArrayList<>();
		if ( PlatformDetection.isWindows() )
			cmd.addAll( Arrays.asList( "cmd.exe", "/c" ) );
		cmd.add( Paths.get( envFile.getAbsolutePath(), (PlatformDetection.isWindows() ? "python.exe" : "bin/python") ).toAbsolutePath().toString() );
		cmd.addAll( Arrays.asList( new String[] {"-m", "pip", "install"} ) );
		// TODO until appose new release cmd.addAll( INSTALL_PIP_DEPS );
		cmd.addAll( INSTALL_EVSAM_PIP_DEPS );
		final ProcessBuilder builder = new ProcessBuilder().directory( envFile );
		//builder.inheritIO();
		if ( PlatformDetection.isWindows() )
		{
			final Map< String, String > envs = builder.environment();
			final String envDir = envFile.getAbsolutePath();
			envs.put( "Path", envDir + ";" + envs.get( "Path" ) );
			envs.put( "Path", Paths.get( envDir, "Scripts" ).toString() + ";" + envs.get( "Path" ) );
			envs.put( "Path", Paths.get( envDir, "Library" ).toString() + ";" + envs.get( "Path" ) );
			envs.put( "Path", Paths.get( envDir, "Library", "Bin" ).toString() + ";" + envs.get( "Path" ) );
		} else {
			final Map< String, String > envs = builder.environment();
			final String envDir = envFile.getAbsolutePath();
			envs.put( "PATH", envDir + ":" + envs.get( "PATH" ) );
			envs.put( "PATH", Paths.get( envDir, "bin" ).toString() + ":" + envs.get( "PATH" ) );
		}
		if ( builder.command( cmd ).start().waitFor() != 0 )
			throw new RuntimeException();
	}
	
	/**
	 * TODO keep until release of stable Appose
	 * Install the Python package to run Appose in Python
	 * @param envName
	 * 	environment where Appose is going to be installed
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	private void installApposePackage(String envName) throws IOException, InterruptedException, MambaInstallException {
		installApposePackage(envName, false);
	}
	
	/**
	 * TODO keep until release of stable Appose
	 * Install the Python package to run Appose in Python
	 * @param envName
	 * 	environment where Appose is going to be installed
	 * @param force
	 * 	if the package already exists, whether to overwrite it or not
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	private void installApposePackage(String envName, boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to SAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'APPOSE' PYTHON PACKAGE");
		String zipResourcePath = "appose-python.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + envName;
        try (
            	InputStream zipInputStream = SamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
    			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'APPOSE' PYTHON PACKAGE INSTALLATION");
    			throw e;
    		}
        mamba.pipInstallIn(envName, new String[] {mamba.getEnvsDir() + File.separator + envName + File.separator + APPOSE});
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'APPOSE' PYTHON PACKAGE INSATLLED");
	}
	
	/**
	 * Install the Python package to run SAM
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	public void installSAMPackage() throws IOException, InterruptedException, MambaInstallException {
		installSAMPackage(false);
	}
	
	/**
	 * Install the Python package to run SAM
	 * @param force
	 * 	if the package already exists, whether to overwrite it or not
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	public void installSAMPackage(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (checkSAMPackageInstalled() && !force)
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to SAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'SAM' PYTHON PACKAGE");
		try {
			mamba.create(ESAM_ENV_NAME, true);
		} catch (MambaInstallException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'SAM' PYTHON PACKAGE INSTALLATION");
			throw new MambaInstallException("Unable to SAM without first installing Mamba.");
		} catch (IOException | InterruptedException | RuntimeException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'SAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		String zipResourcePath = "SAM.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + SAM_ENV_NAME + File.separator + SAM_NAME;
        try (
        	InputStream zipInputStream = SamEnvManager.class.getResourceAsStream(zipResourcePath);
        	ZipInputStream zipInput = new ZipInputStream(zipInputStream);
        		) {
        	ZipEntry entry;
        	while ((entry = zipInput.getNextEntry()) != null) {
                File entryFile = new File(outputDirectory + File.separator + entry.getName());
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
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'SAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'SAM' PYTHON PACKAGE INSATLLED");
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
        	InputStream zipInputStream = SamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
	 * Install the Python package to run EfficientViTSAM
	 * 
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	public void installEfficientViTSAMPackage() throws IOException, InterruptedException, MambaInstallException {
		installEfficientViTSAMPackage(false);
	}
	
	/**
	 * Install the Python package to run EfficientViTSAM
	 * 
	 * @param force
	 * 	if the package already exists, whether to overwrite it or not
	 * 
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	public void installEfficientViTSAMPackage(boolean force) throws IOException, InterruptedException, MambaInstallException {
		if (checkEfficientViTSAMPackageInstalled() && !force)
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to EfficientViTSAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'EFFICIENTVITSAM' PYTHON PACKAGE");
		String zipResourcePath = "efficientvit.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + EVITSAM_ENV_NAME;
        try (
        	InputStream zipInputStream = SamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED 'EFFICIENTVITSAM' PYTHON PACKAGE INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'EFFICIENTVITSAM' PYTHON PACKAGE INSATLLED");
	}
	
	/**
	 * Method to install automatically Micromamba in the path of the corresponding {@link SamEnvManager} instance.
	 * 
	 * @throws IOException if there is any file related error during the installation
	 * @throws InterruptedException if the installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer files
	 * @throws URISyntaxException if there is any error with the url that points to the micromamba instance to download
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installMambaPython() throws IOException, InterruptedException, 
	ArchiveException, URISyntaxException, MambaInstallException{
		if (checkMambaInstalled()) return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING MICROMAMBA");
		try {
			mamba.installMicromamba();
		} catch (IOException | InterruptedException | ArchiveException | URISyntaxException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MICROMAMBA INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- MICROMAMBA INSTALLED");
	}
	
	/**
	 * Install all the requirements to run SAM. First, checks if micromamba is installed, if not installs it;
	 * then checks if the Python environment and packages needed to run SAM are installed and if not installs it
	 * and finally checks whether the weights are installed, and if not installs them too.
	 * 
	 * 
	 * @throws IOException if there is any file related error in the model installation
	 * @throws InterruptedException if the model installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer
	 * @throws URISyntaxException if there is any error with the URL to the micromamba installer download page
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installSAM() throws IOException, InterruptedException, 
									ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkEfficientSAMPythonInstalled()) this.installEfficientSAMPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadSAMWeigths(false);
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
	public void installEfficientSAMSmall() throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkEfficientSAMPythonInstalled()) this.installEfficientSAMPython();
		
		if (!this.checkEfficientSAMPackageInstalled()) this.installEfficientSAMPackage();
		
		if (!this.checkEfficientSAMSmallWeightsDownloaded()) this.downloadESAMSmallWeights(false);
	}
	
	/**
	 * Install all the requirements to run EfficientViTSAM. First, checks if micromamba is installed, if not installs it;
	 * then checks if the Python environment and packages needed to run EfficientViTSAM are installed and if not installs it
	 * and finally checks whether the weights are installed, and if not installs them too.
	 * This method installs the default trained model {@value #DEFAULT_EVITSAM}, to install any other available model 
	 * use {@link #installEfficientViTSAM(String)}
	 * 
	 * 
	 * @throws IOException if there is any file related error in the model installation
	 * @throws InterruptedException if the model installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer
	 * @throws URISyntaxException if there is any error with the URL to the micromamba installer download page
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installEfficientViTSAM() throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		installEfficientViTSAM(DEFAULT_EVITSAM);
	}
	
	/**
	 * Install all the requirements to run EfficientViTSAM. First, checks if micromamba is installed, if not installs it;
	 * then checks if the Python environment and packages needed to run EfficientViTSAM are installed and if not installs it
	 * and finally checks whether the weights are installed, and if not installs them too.
	 * The available model versions are defined at {@link EfficientViTSamJ#getListOfSupportedEfficientViTSAM}
	 *
	 * 
	 * @throws IOException if there is any file related error in the model installation
	 * @throws InterruptedException if the model installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer
	 * @throws URISyntaxException if there is any error with the URL to the micromamba installer download page
	 * @throws MambaInstallException if there is any error installing micromamba
	 */
	public void installEfficientViTSAM(String modelType) throws IOException, InterruptedException, 
													ArchiveException, URISyntaxException, MambaInstallException {
		if (!this.checkMambaInstalled()) this.installMambaPython();
		
		if (!this.checkEfficientViTSAMPythonInstalled()) this.installEfficientViTSAMPython();
		
		if (!this.checkEfficientViTSAMPackageInstalled()) this.installEfficientViTSAMPackage();
		
		if (!this.checkEfficientViTSAMWeightsDownloaded(modelType)) this.downloadEfficientViTSAMWeights(modelType, false);
	}
	
	/**
	 * 
	 * @return the path to the EfficientSAM Small weights file
	 */
	public String getEfficientSAMSmallWeightsPath() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME, ESAM_NAME, "weights", ESAM_SMALL_WEIGHTS_NAME).toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the path to the EfficientViTSAM weights file for model {@value #DEFAULT_EVITSAM}
	 */
	public String getEfficientViTSAMWeightsPath() {
		return getEfficientViTSAMWeightsPath(DEFAULT_EVITSAM);
	}
	
	/**
	 * 
	 * @param modelType
	 * 	the EfficientViTSAM version. The versions are defined at {@link EfficientViTSamJ#getListOfSupportedEfficientViTSAM}
	 * @return the path to the EfficientViTSAM model of interest
	 */
	public String getEfficientViTSAMWeightsPath(String modelType) {
		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", modelType + ".pt").toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the path to the SAM Huge weights file
	 */
	public String getSAMWeightsPath() {
		File file = Paths.get(path, "envs", SAM_ENV_NAME, SAM_NAME, "weights", SAM_WEIGHTS_NAME).toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment needed to run EfficientSAM
	 */
	public String getEfficientSAMPythonEnv() {
		File file = Paths.get(path, "envs", COMMON_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment where the EfficientSAM Python package is installed
	 */
	public String getEfficientSamEnv() {
		File file = Paths.get(path, "envs", ESAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment needed to run EfficientViTSAM
	 */
	public String getEfficientViTSamEnv() {
		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the path to the folder where all the SAMJ environments are created
	 */
	public String getEnvsPath() {
		return Paths.get(path, "envs").toFile().getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the official name of the SAM Huge weights
	 */
	public static String getSAMWeightsName() {
		return SAM_WEIGHTS_NAME;
	}
	
	/**
	 * 
	 * @return the official name of the EfficientSAM Small weights
	 */
	public static String getEfficientSAMSmallWeightsName() {
		return ESAM_SMALL_WEIGHTS_NAME;
	}
	
	/**
	 * Method that downloads a file
	 * @param downloadURL
	 * 	url of the file to be downloaded
	 * @param targetFile
	 * 	file where the file from the url will be downloaded too
	 * @throws IOException if there si any error downloading the file
	 * @throws URISyntaxException if there is any error in the URL syntax
	 * @throws InterruptedException if the parent thread is stopped and the download stopped
	 */
	public void downloadFile(String downloadURL, File targetFile, Thread parentThread) 
								throws IOException, URISyntaxException, InterruptedException {
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			URL website = new URL(downloadURL);
	        HttpURLConnection conn = (HttpURLConnection) website.openConnection();
	        conn.setRequestMethod("GET");
	        conn.setRequestProperty("User-Agent", CommonUtils.getJDLLUserAgent());//"jdll/0.5.6 (Linux; Java 1.8.0_292)");
	        rbc = Channels.newChannel(conn.getInputStream());
			// TODO rbc = Channels.newChannel(website.openStream());
			// Create the new model file as a zip
			fos = new FileOutputStream(targetFile);
			// Send the correct parameters to the progress screen
			FileDownloader downloader = new FileDownloader(rbc, fos);
			downloader.call(parentThread);
		} finally {
			if (fos != null)
				fos.close();
			if (rbc != null)
				rbc.close();
		}
	}
	
	/**
	 * For a fresh, installation, SAMJ might need to download first micromamba. In that case, this method
	 * returns the progress made for its download.
	 * @return progress made downloading Micromamba
	 */
	public double getMambaInstallationProcess() {
		return this.mamba.getMicromambaDownloadProgress();
	}
	
	public String getEnvCreationProgress() {
		return this.getEnvCreationProgress();
	}
	
	private Thread reportProgress(String startStr) {
		Thread currentThread = Thread.currentThread();
		Thread thread = new Thread (() -> {
			passToConsumer(startStr);
			while (currentThread.isAlive()) {
				try {Thread.sleep(300);} catch (InterruptedException e) {break;}
				if (System.currentTimeMillis() - millis > 300)
					passToConsumer("");
			}
		});
		thread.start();
		return thread;
	}
}
