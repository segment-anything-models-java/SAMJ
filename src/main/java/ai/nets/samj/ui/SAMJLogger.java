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
package ai.nets.samj.ui;

/**
 * Interface that lets the consumer software implement their own logging.
 * @author Carlos Garcia
 */
public interface SAMJLogger {

	/**
	 * Method that displays info about what SamJ is doing
	 * @param text
	 * 	the information from the plugin
	 */
	void info(String text);
	/**
	 * Method that displays warnings about what SamJ is doing
	 * @param text
	 * 	the information from the plugin
	 */
	void warn(String text);
	/**
	 * Method that displays infromation about the errors that happened during the program execution
	 * @param text
	 * 	the error string
	 */
	void error(String text);
}
