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
package ai.nets.samj.communication.model;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.nets.samj.models.AbstractSamJ;
import ai.nets.samj.models.MedSam;
import ai.nets.samj.install.MedSamEnvManager;
import ai.nets.samj.ui.SAMJLogger;

/**
 * Instance to communicate with MedSAM - a fine-tuned version of SAM for medical imaging
 * @author SAMJ developers
 */
public class MedSAM extends SAMModel {
	/**
	 * Name of the model
	 */
	public static final String FULL_NAME = "MedSAM";
	/**
	 * Axes order required for the input image by the model
	 */
	public static final String INPUT_IMAGE_AXES = "xyc";

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 */
	public MedSAM() {
		this.isHeavy = true;
		this.fullName = "MedSAM: Segment Anything in Medical Images";
		this.githubLink = "https://github.com/bowang-lab/MedSAM";
		this.paperLink = "https://www.nature.com/articles/s41467-024-44824-z";
		this.githubName = "bowang-lab/MedSAM";
		this.paperName = "Segment anything in medical images";
		this.speedRank = 3;
		this.performanceRank = 5;
		this.size = Math.round(10 * MedSamEnvManager.MEDSAM_BYTE_SIZE / ((double) (1024 * 1024))) / 10.0;
		this.manager = MedSamEnvManager.create(MedSamEnvManager.DEFAULT_DIR);
	}

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 * @param manager
	 * 	the model manager {@link MedSamEnvManager} that contains the info about where the model
	 * 	environment and model weights are installed
	 */
	public MedSAM(MedSamEnvManager manager) {
		this.isHeavy = true;
		this.fullName = "MedSAM: Segment Anything in Medical Images";
		this.githubLink = "https://github.com/bowang-lab/MedSAM";
		this.paperLink = "https://www.nature.com/articles/s41467-024-44824-z";
		this.githubName = "bowang-lab/MedSAM";
		this.paperName = "Segment anything in medical images";
		this.speedRank = 3;
		this.performanceRank = 5;
		this.size = Math.round(10 * MedSamEnvManager.MEDSAM_BYTE_SIZE / ((double) (1024 * 1024))) / 10.0;
		this.manager = manager;
	}

	@Override
	public String getName() {
		return FULL_NAME;
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> void setImage(final RandomAccessibleInterval<T> image, final SAMJLogger useThisLoggerForIt) 
			throws IOException, InterruptedException, RuntimeException {
		Objects.requireNonNull(image, "The image cannot be null.");
		if (useThisLoggerForIt != null) 
			this.log = useThisLoggerForIt;
		AbstractSamJ.DebugTextPrinter filteringLogger = text -> {
			int idx = text.indexOf("\"responseType\": \"COMPLETION\"");
			int idxProgress = text.indexOf(AbstractSamJ.getProgressString());
			if (idxProgress != -1) return;
			if (idx > 0) {
				String regex = "\"outputs\"\\s*:\\s*\\{.*?\\}(,)?";
		        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
		        Matcher matcher = pattern.matcher(text);
		        text = matcher.replaceAll("");
			}
			this.log.info(text);
		};
		if (this.samj == null)
			samj = MedSam.initializeSam(manager, filteringLogger, false);
		try {
			this.samj.setImage(Cast.unchecked(image));
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME + " experienced an error: " + e.getMessage());
			throw e;
		}
	}

	@Override
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}
}
