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
import ai.nets.samj.models.EfficientViTSamJ;
import ai.nets.samj.install.EfficientViTSamEnvManager;
import ai.nets.samj.ui.SAMJLogger;

/**
 * Instance to communicate with EfficientViTSAM xl0
 * @author Carlos Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class EfficientViTSAMXL0 extends SAMModel {
	/**
	 * Name of the model
	 */
	public static final String FULL_NAME = "EfficientViTSAM-xl0";
	/**
	 * Axes order required for the input image by the model
	 */
	public static final String INPUT_IMAGE_AXES = "xyc";
	
	private static final String ID = "xl0";

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 * @param manager
	 * 	the model manager that contains the info about where the model
	 * 	environment and model weights are installed
	 */
	public EfficientViTSAMXL0() {
		this.isHeavy = false;
		this.fullName = "EfficientViT-SAM smallest version (XL0)";
		this.githubLink = "https://github.com/mit-han-lab/efficientvit";
		this.githubName = "mit-han-lab/efficientvit";
		this.paperName = "EfficientViT-SAM: Accelerated Segment Anything Model Without Performance Loss";
		this.paperLink = "https://arxiv.org/pdf/2402.05008.pdf";
		this.speedRank = 3;
		this.performanceRank = 3;
		this.size = 468.2;
		this.manager = EfficientViTSamEnvManager.create(EfficientViTSamEnvManager.DEFAULT_DIR, ID);
	}

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 */
	public EfficientViTSAMXL0(EfficientViTSamEnvManager manager) {
		this.isHeavy = false;
		this.fullName = "EfficientViT-SAM smallest version (XL0)";
		this.githubLink = "https://github.com/mit-han-lab/efficientvit";
		this.githubName = "mit-han-lab/efficientvit";
		this.paperName = "EfficientViT-SAM: Accelerated Segment Anything Model Without Performance Loss";
		this.paperLink = "https://arxiv.org/pdf/2402.05008.pdf";
		this.speedRank = 3;
		this.performanceRank = 3;
		this.size = 468.2;
		if (!manager.getModelType().equals(ID))
			throw new IllegalArgumentException("The model type should be: " + ID + " vs manager model type: " + manager.getModelType());
		this.manager = manager;
	}
	
	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return FULL_NAME;
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
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
			this.log.info( text );
		};
		if (this.samj == null)
			this.samj = EfficientViTSamJ.initializeSam(ID, manager, filteringLogger, false);
		try {
			this.samj.setImage(Cast.unchecked(image));;
		} catch (IOException | InterruptedException | RuntimeException e) {
			log.error(FULL_NAME + " experienced an error: " + e.getMessage());
			throw e;
		}
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}
}
