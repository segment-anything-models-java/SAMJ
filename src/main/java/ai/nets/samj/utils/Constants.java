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

import java.io.InputStream;
import java.util.Properties;


/**
 * Constants for the SAMJ plugin, such as the version and others
 * @author Carlos Garcia Lopez de Haro
 */
public class Constants {
	
	public static final String SAMJ_VERSION = getVersion();
	
	public static final String JAR_NAME = getNAME();
	
    private static String getVersion() {
        try (InputStream input = Constants.class.getResourceAsStream("/.samj_properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("version");
        } catch (Exception | Error ex) {
            return "unknown";
        }
    }
	
    private static String getNAME() {
        try (InputStream input = Constants.class.getResourceAsStream("/.samj_properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop.getProperty("name");
        } catch (Exception | Error ex) {
            return "unknown";
        }
    }

}
