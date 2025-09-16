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
import ai.nets.samj.models.EfficientSamJ;
import ai.nets.samj.install.EfficientSamEnvManager;
import ai.nets.samj.install.Sam2EnvManager;
import ai.nets.samj.ui.SAMJLogger;

/**
 * Instance to communicate with EfficientSAM
 * @author Carlos Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class EfficientSAM extends SAMModel {
	/**
	 * Name of the model
	 */
	public static final String FULL_NAME = "EfficientSAM";
	/**
	 * Axes order required for the input image by the model
	 */
	public static final String INPUT_IMAGE_AXES = "xyc";
	


	/**
	 * Create an instance of the model that loads the model and encodes an image
	 */
	public EfficientSAM() {
		this.isHeavy = true;
		this.fullName = "EfficientSAM: Leveraged Masked Image Pretraining for Efficient Segment Anything";
		this.githubLink = "https://github.com/yformer/EfficientSAM";
		this.paperLink = "https://arxiv.org/pdf/2312.00863.pdf";
		this.githubName = "yformer/EfficientSAM";
		this.paperName = "EfficientSAM: Leveraged Masked Image Pretraining for Efficient Segment";
		this.speedRank = 5;
		this.performanceRank = 2;
		this.size = 105.7;
		this.manager = EfficientSamEnvManager.create();
	}

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 * @param manager
	 * 	the model manager that contains the info about where the model
	 * 	environment and model weights are installed
	 */
	public EfficientSAM(EfficientSamEnvManager manager) {
		this.isHeavy = true;
		this.fullName = "EfficientSAM: Leveraged Masked Image Pretraining for Efficient Segment Anything";
		this.githubLink = "https://github.com/yformer/EfficientSAM";
		this.paperLink = "https://arxiv.org/pdf/2312.00863.pdf";
		this.githubName = "yformer/EfficientSAM";
		this.paperName = "EfficientSAM: Leveraged Masked Image Pretraining for Efficient Segment";
		this.speedRank = 5;
		this.performanceRank = 2;
		this.size = 105.7;
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
	public void loadModel(final SAMJLogger useThisLoggerForIt) throws IOException, InterruptedException, RuntimeException {
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
			samj = EfficientSamJ.initializeSam(manager, filteringLogger, false);
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}
}
