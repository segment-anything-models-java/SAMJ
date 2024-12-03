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

import ai.nets.samj.install.Sam2EnvManager;
import ai.nets.samj.models.AbstractSamJ;
import ai.nets.samj.models.Sam2;
import ai.nets.samj.ui.SAMJLogger;

/**
 * Instance to communicate with EfficientSAM
 * @author Carlos Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class SAM2Large extends SAMModel {

	/**
	 * Name of the model
	 */
	public static final String FULL_NAME = "SAM2.1 Large";
	/**
	 * Axes order required for the input image by the model
	 */
	public static final String INPUT_IMAGE_AXES = "xyc";	
	

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 */
	public SAM2Large() {
		this.isHeavy = true;
		this.fullName = "SAM-2: Segment Anything Model 2 (Large)";
		this.githubLink = "https://github.com/facebookresearch/segment-anything-2";
		this.paperLink = "https://ai.meta.com/research/publications/sam-2-segment-anything-in-images-and-videos/";
		this.githubName = "facebookresearch/segment-anything-2";
		this.paperName = "SAM 2: Segment Anything in Images and Videos";
		this.speedRank = 4;
		this.performanceRank = 1;
		this.size = 898.1;
		this.manager = Sam2EnvManager.create(Sam2EnvManager.DEFAULT_DIR, "large");
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
			samj = Sam2.initializeSam("large", manager, filteringLogger, false);
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
