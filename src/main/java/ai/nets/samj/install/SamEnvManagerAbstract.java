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
public abstract class SamEnvManagerAbstract {

	
	public abstract void checkSAMDepsInstalled();
	
	public abstract void installSAMDeps();
	
	public abstract void checkModelWeightsInstalled();
	
	public abstract void installModelWeigths();
	
	
	
	
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
	 * Check whether micromamba is installed or not in the directory of the {@link SamEnvManagerAbstract} instance.
	 * @return whether micromamba is installed or not in the directory of the {@link SamEnvManagerAbstract} instance.
	 */
	public boolean checkMambaInstalled() {
		File ff = new File(path + MAMBA_RELATIVE_PATH);
		if (!ff.exists()) return false;
		return mamba.checkMambaInstalled();
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
        mamba.pipInstallIn(envName, new String[] {mamba.getEnvsDir() + File.separator + envName + File.separator + APPOSE});
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
	 * 
	 * @return the path to the folder where all the SAMJ environments are created
	 */
	public String getEnvsPath() {
		return Paths.get(path, "envs").toFile().getAbsolutePath();
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
