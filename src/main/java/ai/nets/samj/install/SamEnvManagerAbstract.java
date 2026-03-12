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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.bioimage.modelrunner.system.PlatformDetection;

import org.apposed.appose.BuildException;
import org.apposed.appose.Builder.ProgressConsumer;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.apposed.appose.builder.PixiBuilder;
import org.apposed.appose.tool.Pixi;


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
	protected PixiBuilder pixi;
	/**
	 * Environment being used for pixi
	 */
	protected String installEnv = "default";
	/**
	 * Consumer to transmit information about the output
	 */
	protected Consumer<String> outConsumer;
	/**
	 * Consumer to transmit the information about errors
	 */
	protected Consumer<String> errConsumer;
	/**
	 * Consumer to transmit the information about the progress downloading pixi
	 */
	protected ProgressConsumer pixiConsumer;
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
	static public String DEFAULT_DIR = Paths.get(Pixi.BASE_PATH).toAbsolutePath().toString();

	
	public abstract boolean checkEverythingInstalled();
	
	public abstract boolean checkSAMDepsInstalled();
	
	public abstract void installSAMDeps() throws InterruptedException, BuildException;

	public abstract void installSAMDeps(boolean force) throws InterruptedException, BuildException;

	public abstract boolean checkModelWeightsInstalled();
	
	public abstract void installModelWeigths() throws IOException, InterruptedException;
	
	public abstract void installModelWeigths(boolean force) throws IOException, InterruptedException;
	
	public abstract void installEverything() throws IOException, InterruptedException, BuildException;
	
	public abstract String getModelWeigthsName();
	
	public abstract String getModelWeigthPath();
	
	public abstract String getModelEnv();
	
	public abstract void uninstall();

	public String getPixiEnv() {
		return this.installEnv;
	}
	
	public void setOutputConsumer(Consumer<String> consumer) {
		this.outConsumer = consumer;
	}
	
	public void setErrorConsumer(Consumer<String> consumer) {
		this.errConsumer = consumer;
	}
	
	public void setProgressConsumer(ProgressConsumer consumer) {
		this.pixiConsumer = consumer;
	}

    public static void installWheel(String wheelPath, Environment env) {
		List<String> pythonExes = Arrays.asList("python", "python3", "python.exe");
        try {
			Service serv = env.service(pythonExes, "-m", "pip", "install", "--no-deps", wheelPath).start();
			serv.waitFor();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
        
    }

    public static void installWheelFromResource(String wheelResourcePath, Environment env)
            throws IOException {

    	Path wheel = extractToTemp(wheelResourcePath);
        try {
            installWheel(wheel.toAbsolutePath().toString(), env);
        } finally {
        	deleteRecursively(wheel);
        }
    }
    
    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                  .forEach(new Consumer<Path>() {
                      @Override
                      public void accept(Path p) {
                          try {
                              Files.deleteIfExists(p);
                          } catch (IOException e) {
                              throw new UncheckedIOException(e);
                          }
                      }
                  });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static Path extractToTemp(String resourcePath) throws IOException {
        try (InputStream in = Sam2EnvManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            String fileName = Paths.get(resourcePath).getFileName().toString();

            Path tempDir = Files.createTempDirectory("sam2-wheel-");
            Path wheelPath = tempDir.resolve(fileName);

            Files.copy(in, wheelPath, StandardCopyOption.REPLACE_EXISTING);
            return wheelPath;
        }
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
}
