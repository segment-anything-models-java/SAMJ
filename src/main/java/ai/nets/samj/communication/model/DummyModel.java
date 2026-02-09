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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import ai.nets.samj.models.AbstractSamJ;
import ai.nets.samj.models.Sam2;
import ai.nets.samj.install.Sam2EnvManager;
import ai.nets.samj.ui.SAMJLogger;

/**
 * Instance to communicate with EfficientSAM
 * @author Carlos Garcia Lopez de Haro
 * @author Vladimir Ulman
 */
public class DummyModel extends SAMModel {
	/**
	 * Name of the model
	 */
	public static final String FULL_NAME = "Dummy";
	/**
	 * Axes order required for the input image by the model
	 */
	public static final String INPUT_IMAGE_AXES = "xyc";
		

	/**
	 * Create an instance of the model that loads the model and encodes an image
	 * @throws BuildException if there is any error retrieving the environment
	 */
	public DummyModel() {
		this.isHeavy = false;
		this.fullName = "Dummy";
		this.githubLink = "https://github.com/segment-anything-models-java/SAMJ-IJ/issues";
		this.paperLink = "https://arxiv.org/abs/2506.02783";
		this.githubName = "segment-anything-models-java/SAMJ-IJ";
		this.paperName = "Please reinstall SAMJ. Installation is incorrect";
		this.speedRank = Integer.MAX_VALUE;
		this.performanceRank = Integer.MAX_VALUE;
		//this.size = Math.round(10 * Sam2EnvManager.SAM2_1_BYTE_SIZES_MAP.get(ID) / ((double) ( 1024 * 1024))) / 10.0;
		this.size = Integer.MAX_VALUE;
		this.manager = null;
	}
	
	@Override
	public boolean isInstalled() {
		return false;
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
	public void loadModel(final SAMJLogger useThisLoggerForIt) throws InterruptedException, TaskException, BuildException {
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public String getInputImageAxes() {
		return INPUT_IMAGE_AXES;
	}
}
