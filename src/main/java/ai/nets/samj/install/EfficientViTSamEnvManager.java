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
import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.gui.tools.Files;
import ai.nets.samj.models.EfficientViTSamJ;
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
public class EfficientViTSamEnvManager extends SamEnvManagerAbstract {
	
	private final String modelType;
	/**
	 * Default version for the family of EfficientViTSAM models
	 */
	final public static String DEFAULT_EVITSAM = "l0";
	/**
	 * Dependencies to be checked to make sure that the environment is able to load a SAM based model. 
	 * General for every supported model.
	 */
	// TODO update final public static List<String> CHECK_DEPS = Arrays.asList(new String[] {"appose", "torch=2.0.1", "torchvision=0.15.2", "skimage", "mkl=2024.0.0"});
	final public static List<String> CHECK_DEPS_EVSAM;
	static {
		if (!PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64) && !PlatformDetection.isUsingRosseta())
			CHECK_DEPS_EVSAM = Arrays.asList(new String[] {"appose", "torch", "torchvision", 
					"skimage", "onnxsim", "timm", "onnx", "segment_anything", "mkl"});
		else 
			CHECK_DEPS_EVSAM = Arrays.asList(new String[] {"appose", "torch", "torchvision", 
					"skimage", "onnxsim", "timm", "onnx", "segment_anything"});
	}
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
			INSTALL_PIP_DEPS = Arrays.asList(new String[] {"mkl==2024.0.0", "appose"});
		else 
			INSTALL_PIP_DEPS = Arrays.asList(new String[] {"appose"});
	}
	/**
	 * Dependencies for EfficientViTSAM environments that need to be installed using PIP
	 */
	final public static List<String> INSTALL_EVSAM_PIP_DEPS = Arrays.asList(new String[] {"onnxsim", "segment_anything"});
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
	 * Name of the environment that contains the code, dependencies and weigths to load EfficientViTSAM models
	 */
	final static public String EVITSAM_ENV_NAME = "efficientvit_sam_env";
	/**
	 * Name of the folder that contains the code and weigths for EfficientViTSAM models
	 */
	final static public String EVITSAM_NAME = "efficientvit";
	/**
	 * URL to download the EfficientViTSAM model. It needs to be used with String.format(EVITSAM_URL, "l0"), whre l0 could be any of 
	 * the existing EfficientVitSAM model 
	 */
	final static private String EVITSAM_URL = "https://huggingface.co/han-cai/efficientvit-sam/resolve/main/%s.pt?download=true";
	
	private EfficientViTSamEnvManager(String modelType) {
		List<String> modelTypes = EFFICIENTVITSAM_BYTE_SIZES_MAP.keySet().stream().collect(Collectors.toList());
		if (!modelTypes.contains(modelType)) {
			throw new IllegalArgumentException("Invalid model variant chosen: '" + modelType + "'."
					+ "The only supported variants are: " + modelTypes.toString());
		}
		this.modelType = modelType;
	}
	
	/**
	 * Creates an instance of {@link EfficientViTSamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param modelType
	 * 	which of the possible EfficientViT SAM wants to be used. The possible variants are the keys of the {@value #EFFICIENTVITSAM_BYTE_SIZES_MAP} map
	 * @return an instance of {@link EfficientViTSamEnvManager}
	 */
	public static EfficientViTSamEnvManager create(String path, String modelType) {
		return create(path, modelType == null ? DEFAULT_EVITSAM : modelType, (ss) -> {});
	}
	
	/**
	 * Creates an instance of {@link EfficientViTSamEnvManager} that uses a micromamba installed at the argument
	 * provided by 'path'.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param path
	 * 	the path where the corresponding micromamba shuold be installed
	 * @param modelType
	 * 	each of the possible model types (sizes) that EfficientiTSAM can have. They are the keys of
	 * 	the following map {@link #EFFICIENTVITSAM_BYTE_SIZES_MAP}
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link EfficientViTSamEnvManager}
	 */
	public static EfficientViTSamEnvManager create(String path, String modelType, Consumer<String> consumer) {
		EfficientViTSamEnvManager installer = new EfficientViTSamEnvManager(modelType);
		installer.path = path;
		installer.consumer = consumer;
		installer.mamba = new Mamba(path);
		return installer;
	}
	
	/**
	 * Creates an instance of {@link EfficientViTSamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}. Uses the default model {@link #DEFAULT_EVITSAM}
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @return an instance of {@link EfficientViTSamEnvManager}
	 */
	public static EfficientViTSamEnvManager create() {
		return create(DEFAULT_DIR, null);
	}
	
	/**
	 * Creates an instance of {@link EfficientViTSamEnvManager} that uses a micromamba installed at the default
	 * directory {@link #DEFAULT_DIR}.
	 * Micromamba does not need to be installed as the code will install it automatically.
	 * @param consumer
	 * 	an specific consumer where info about the installation is going to be communicated
	 * @return an instance of {@link EfficientViTSamEnvManager}
	 */
	public static EfficientViTSamEnvManager create(Consumer<String> consumer) {
		return create(DEFAULT_DIR, null, consumer);
	}
	
	/**
	 * Check whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not. The environment folder should be named {@value #EVITSAM_ENV_NAME} 
	 * @return whether the Python environment with the corresponding packages needed to run EfficientSAM
	 * has been installed or not
	 */
	public boolean checkSAMDepsInstalled() {
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
	 * Check whether the Python package to run EfficientSAM has been installed. The package will be in the folder
	 * {@value #ESAM_ENV_NAME}. The Python executable and other dependencies will be at {@value #COMMON_ENV_NAME}
	 * @return whether the Python package to run EfficientSAM has been installed.
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
	public boolean checkModelWeightsInstalled() {
		if (!EfficientViTSamJ.getListOfSupportedEfficientViTSAM().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientViT models: " 
												+ EfficientViTSamJ.getListOfSupportedEfficientViTSAM());
		File weightsFile = Paths.get(this.path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", modelType + ".pt").toFile();
		if (!weightsFile.isFile()) return false;
		if (weightsFile.length() != EFFICIENTVITSAM_BYTE_SIZES_MAP.get(modelType)) return false;
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
		if (!EfficientViTSamJ.getListOfSupportedEfficientViTSAM().contains(modelType))
			throw new IllegalArgumentException("The provided model is not one of the supported EfficientViT models: " 
												+ EfficientViTSamJ.getListOfSupportedEfficientViTSAM());
		if (!force && this.checkModelWeightsInstalled())
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
	 * If Micromamba is not installed in the path of the {@link EfficientViTSamEnvManager} instance, this method
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
	 * If Micromamba is not installed in the path of the {@link EfficientViTSamEnvManager} instance, this method
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
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- CREATING THE EFFICIENTVITSAM PYTHON ENVIRONMENT WITH ITS DEPENDENCIES");
		String[] pythonArgs = new String[] {"-c", "conda-forge", "python=3.11", "-c", "pytorch"};
		String[] args = new String[pythonArgs.length + INSTALL_CONDA_DEPS.size() + INSTALL_EVSAM_CONDA_DEPS.size()];
		int c = 0;
		for (String ss : pythonArgs) args[c ++] = ss;
		for (String ss : INSTALL_CONDA_DEPS) args[c ++] = ss;
		for (String ss : INSTALL_EVSAM_CONDA_DEPS) args[c ++] = ss;
		if (!this.checkSAMDepsInstalled() || force) {
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
			ArrayList<String> pipInstall = new ArrayList<String>();
			for (String ss : new String[] {"-m", "pip", "install"}) pipInstall.add(ss);
			for (String ss : INSTALL_PIP_DEPS) pipInstall.add(ss);
			try {
				Mamba.runPythonIn(Paths.get(path,  "envs", EVITSAM_ENV_NAME).toFile(), pipInstall.stream().toArray( String[]::new ));
			} catch (IOException | InterruptedException e) {
	            thread.interrupt();
	            passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED PYTHON ENVIRONMENT CREATION WHEN INSTALLING PIP DEPENDENCIES");
				throw e;
			}
		}
        thread.interrupt();
        passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- EFFICIENTVITSAM PYTHON ENVIRONMENT CREATED");
        // TODO remove
        installApposePackage(EVITSAM_ENV_NAME);
        installEfficientViTSAMPackage();
	}
	
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
	 * Install the Python package to run EfficientSAM
	 * @throws IOException if there is any file creation related issue
	 * @throws InterruptedException if the package installation is interrupted
	 * @throws MambaInstallException if there is any error with the Mamba installation
	 */
	private void installEfficientViTSAMPackage() throws IOException, InterruptedException, MambaInstallException {
		if (checkEfficientViTSAMPackageInstalled())
			return;
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to EfficientViTSAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'EFFICIENTVITSAM' PYTHON PACKAGE");
		String zipResourcePath = "efficientvit.zip";
        String outputDirectory = mamba.getEnvsDir() + File.separator + EVITSAM_ENV_NAME;
        try (
        	InputStream zipInputStream = EfficientViTSamEnvManager.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
		
		if (!this.checkEfficientViTSAMPackageInstalled()) this.installEfficientViTSAMPackage();
		
		if (!this.checkModelWeightsInstalled()) this.installModelWeigths();
	}
	
	/**
	 * 
	 * @return the path to the EfficientSAM Small weights file
	 */
	public String getModelWeightsPath() {
		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", modelType + ".pt").toFile();
		if (!file.isFile()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the the path to the Python environment needed to run EfficientSAM
	 */
	public String getModelEnv() {
		File file = Paths.get(path, "envs", EVITSAM_ENV_NAME).toFile();
		if (!file.isDirectory()) return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * 
	 * @return the official name of the EfficientSAM Small weights
	 */
	public String getModelWeigthsName() {
		return modelType + ".pt";
	}

	@Override
	public String getModelWeigthPath() {
		return Paths.get(this.path, "envs", EVITSAM_ENV_NAME, EVITSAM_NAME, "weights", modelType + ".pt").toAbsolutePath().toString();
	}

	@Override
	public boolean checkEverythingInstalled() {
		if (!this.checkMambaInstalled()) return false;
		
		if (!this.checkSAMDepsInstalled()) return false;
		
		if (!this.checkEfficientViTSAMPackageInstalled()) return false;
		
		if (!this.checkModelWeightsInstalled()) return false;
		
		return true;
	}

	@Override
	public void uninstall() {
		if (new File(this.getModelWeightsPath()).getParentFile().list().length != 1)
			Files.deleteFolder(new File(this.getModelWeightsPath()));
		else
			Files.deleteFolder(new File(this.getModelEnv()));
	}
}
