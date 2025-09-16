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

import java.util.ArrayList;

/**
 * A static list of SAM networks available in this installation.
 */
public class SAMModels extends ArrayList<SAMModel> {

	private static final long serialVersionUID = -6037502816438646853L;

	/**
	 * A list where each of the components is a {@link SAMModel}
	 */
	public  SAMModels() {
		super();
		add(new SAM2Tiny());
		add(new SAM2Small());
		add(new SAM2Large());
		add(new EfficientTAMTiny());
		add(new EfficientTAMSmall());
	}
}
