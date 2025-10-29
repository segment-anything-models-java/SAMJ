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
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.bioimage.modelrunner.system.PlatformDetection;

import org.apache.commons.compress.archivers.ArchiveException;

import org.apposed.appose.mamba.Mamba;

/*
 * Class that is manages the installation of SAM and EfficientSAM together with Python, their corresponding environments
 * and dependencies
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public abstract class SamEnvManagerAbstract {
	
	protected String path;
	/**
	 * {@link Mamba} instance used to create the environments
	 */
	protected Mamba mamba;
	/**
	 * Consumer used to keep providing info in the case of several threads working
	 */
	protected Consumer<String> consumer;
	/**
	 * Variable used to measer time intervals
	 */
	private static long millis = System.currentTimeMillis();
	/**
	 * Relative path to the mamba executable from the appose folder
	 */
	protected final static String MAMBA_RELATIVE_PATH = PlatformDetection.isWindows() ? 
			 File.separator + "Library" + File.separator + "bin" + File.separator + "micromamba.exe" 
			: File.separator + "bin" + File.separator + "micromamba";	
	/**
	 * Date format
	 */
	protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	/**
	 * Name of the folder that contains the code of Appose in Python
	 */
	final static public String APPOSE = "appose-python";
	/**
	 * Default directory where micromamba is installed and where all the environments are created
	 */
	static public String DEFAULT_DIR = new File("appose_"
			+ ((!PlatformDetection.isMacOS() || !PlatformDetection.isUsingRosseta()) ? PlatformDetection.getArch()
			: PlatformDetection.ARCH_ARM64 )).getAbsolutePath();

	
	public abstract boolean checkEverythingInstalled();
	
	public abstract boolean checkSAMDepsInstalled();
	
	public abstract void installSAMDeps() throws IOException, InterruptedException, ArchiveException, URISyntaxException;

	public abstract void installSAMDeps(boolean force) throws IOException, InterruptedException, ArchiveException, URISyntaxException;

	public abstract boolean checkModelWeightsInstalled();
	
	public abstract void installModelWeigths() throws IOException, InterruptedException;
	
	public abstract void installModelWeigths(boolean force) throws IOException, InterruptedException;
	
	public abstract void installEverything() throws IOException, InterruptedException, ArchiveException, URISyntaxException;
	
	public abstract String getModelWeigthsName();
	
	public abstract String getModelWeigthPath();
	
	public abstract String getModelEnv();
	
	public abstract void uninstall();
	
	
	public void setConsumer(Consumer<String> consumer) {
	}
	
	/**
	 * Send information as Strings to the consumer
	 * @param str
	 * 	String that is going to be sent to the consumer
	 */
	protected void passToConsumer(String str) {
		consumer.accept(str);
		millis = System.currentTimeMillis();
	}
	
	/**
	 * Check whether micromamba is installed or not in the directory of the {@link SamEnvManagerAbstract} instance.
	 * @return whether micromamba is installed or not in the directory of the {@link SamEnvManagerAbstract} instance.
	 */
	public boolean checkMambaInstalled() {
		File ff = new File(path + MAMBA_RELATIVE_PATH);
		if (!ff.exists()) return false;
		// TODO return mamba.checkMambaInstalled();
		return true;
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
	protected void installApposePackage(String envName) throws IOException, InterruptedException {
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
	 */
	protected void installApposePackage(String envName, boolean force) throws IOException, InterruptedException {
		if (!checkMambaInstalled())
			throw new IllegalArgumentException("Unable to SAM without first installing Mamba. ");
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING 'APPOSE' PYTHON PACKAGE");
		String zipResourcePath = "appose-python.zip";
		String outputDirectory = "";
        // TODO String outputDirectory = mamba.getEnvsDir() + File.separator + envName;
        try (
            	InputStream zipInputStream = SamEnvManagerAbstract.class.getClassLoader().getResourceAsStream(zipResourcePath);
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
        // TODO mamba.pipInstallIn(envName, new String[] {mamba.getEnvsDir() + File.separator + envName + File.separator + APPOSE});
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- 'APPOSE' PYTHON PACKAGE INSATLLED");
	}
	
	/**
	 * Method to install automatically Micromamba in the path of the corresponding {@link SamEnvManagerAbstract} instance.
	 * 
	 * @throws IOException if there is any file related error during the installation
	 * @throws InterruptedException if the installation is interrupted
	 * @throws ArchiveException if there is any error decompressing the micromamba installer files
	 * @throws URISyntaxException if there is any error with the url that points to the micromamba instance to download
	 */
	public void installMambaPython() throws IOException, InterruptedException, 
	ArchiveException, URISyntaxException {
		if (checkMambaInstalled()) return;
		Thread thread = reportProgress(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- INSTALLING MICROMAMBA");
		try {
			mamba.installMicromamba();
		} catch (IOException | InterruptedException | URISyntaxException e) {
			thread.interrupt();
			passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- FAILED MICROMAMBA INSTALLATION");
			throw e;
		}
		thread.interrupt();
		passToConsumer(LocalDateTime.now().format(DATE_FORMAT).toString() + " -- MICROMAMBA INSTALLED");
	}
	
	/**
	 * 
	 * @return the path to the folder where all the SAMJ environments are created
	 */
	public String getEnvsPath() {
		return Paths.get(path, "envs").toFile().getAbsolutePath();
	}
	
	/**
	 * For a fresh, installation, SAMJ might need to download first micromamba. In that case, this method
	 * returns the progress made for its download.
	 * @return progress made downloading Micromamba
	 */
	public double getMambaInstallationProcess() {
		// TODO return this.mamba.getMicromambaDownloadProgress();
		return 0.0;
	}
	
	public String getEnvCreationProgress() {
		return this.getEnvCreationProgress();
	}
	
	protected Thread reportProgress(String startStr) {
		Thread currentThread = Thread.currentThread();
		Thread thread = new Thread (() -> {
			passToConsumer(startStr);
			while (currentThread.isAlive()) {
				try {Thread.sleep(300);} catch (InterruptedException e) {break;}
				if (System.currentTimeMillis() - millis > 300 && currentThread.isAlive())
					passToConsumer("");
			}
		});
		thread.start();
		return thread;
	}
}
