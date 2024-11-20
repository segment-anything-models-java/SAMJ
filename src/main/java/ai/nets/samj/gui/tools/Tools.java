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
package ai.nets.samj.gui.tools;

import java.awt.Desktop;
import java.net.URL;

/**
 * Class that contains helper tools for SAMJ
 * @author Daniel Sage
 * @author Carlos Garcia
 */
public class Tools {

	/**
	 * Method that opens a website where there is some documentation about SAMJ
	 * @return true if the method was able to access the website or false otherwise
	 */
	static public boolean help() {
		String url = "https://github.com/segment-anything-models-java/SAMJ-IJ"; 
		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(new URL(url).toURI());
				return true;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}
}
