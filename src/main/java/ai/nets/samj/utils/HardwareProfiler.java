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
package ai.nets.samj.utils;

import java.util.List;

import ai.nets.samj.communication.model.SAMModel;

/**
 * Utility class to detect system hardware capabilities and recommend an optimal SAM model
 * based on CPU performance. All SAMJ models run on CPU, so CPU core count is the primary
 * metric for selecting an appropriate model.
 */
public class HardwareProfiler {

	/**
	 * Recommend a SAM model from the provided list based on system CPU capabilities.
	 * Models are selected according to the following heuristic:
	 * <ul>
	 *   <li>1-2 cores: EfficientViTSAM-L2 (lightest/fastest)</li>
	 *   <li>3-4 cores: EfficientViTSAM-L2 or SAM2 Tiny</li>
	 *   <li>5-8 cores: SAM2 Tiny</li>
	 *   <li>9+ cores: SAM2 Small or SAM2 Large</li>
	 * </ul>
	 * 
	 * @param models list of available SAM models to choose from
	 * @return the recommended model from the list, or the first model if no suitable match is found
	 */
	public static SAMModel recommendModel(List<SAMModel> models) {
		if (models == null || models.isEmpty()) {
			return null;
		}

		int numCores = Runtime.getRuntime().availableProcessors();

		if (numCores <= 2) {
			return findModelByName(models, "EfficientViTSAM-L2");
		} else if (numCores <= 4) {
			SAMModel efficientViT = findModelByName(models, "EfficientViTSAM-L2");
			if (efficientViT != null) {
				return efficientViT;
			}
			return findModelByName(models, "SAM2 Tiny");
		} else if (numCores <= 8) {
			return findModelByName(models, "SAM2 Tiny");
		} else {
			SAMModel sam2Large = findModelByName(models, "SAM2 Large");
			if (sam2Large != null) {
				return sam2Large;
			}
			SAMModel sam2Small = findModelByName(models, "SAM2 Small");
			if (sam2Small != null) {
				return sam2Small;
			}
		}

		return models.get(0);
	}

	/**
	 * Find a model by its display name
	 * @param models list of available models
	 * @param modelName the name of the model to find
	 * @return the model with the matching name, or null if not found
	 */
	private static SAMModel findModelByName(List<SAMModel> models, String modelName) {
		for (SAMModel model : models) {
			if (model.getName().equals(modelName)) {
				return model;
			}
		}
		return null;
	}
}
