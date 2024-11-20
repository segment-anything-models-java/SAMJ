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

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 * Class that contains helper methods to browse and get specific files in the {@link JFileChooser}
 * graphical user interface
 * @author Daniel Sage
 */
public class Files {

	/**
	 * 
	 * @return a String with the full path of the directory from where the code is being executed
	 */
	public static String getWorkingDirectory() {
		return System.getProperty("user.dir");
	}

	/**
	 * 
	 * @return the path to the home directory of the computer
	 */
	public static String getHomeDirectory() {
		return FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath() + File.separator;	
	}
	
	/**
	 * 
	 * @return String path to the desktop directory
	 */
	public static String getDesktopDirectory() {
		return getHomeDirectory() + "Desktop" + File.separator;
	}
	
	/**
	 * Method that returns a {@link File} if the path provided is a File or false otherwise
	 * @param path
	 * 	String path to the file of interest
	 * @return a {@link File} object for the file of interest or null if it does not exist
	 */
	public static File browseFile(String path) {
		JFileChooser fc = new JFileChooser(); 
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		File dir = new File(path);
		if (dir.exists())
			fc.setCurrentDirectory(dir);
		
		int ret = fc.showOpenDialog(null); 
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = new File(fc.getSelectedFile().getAbsolutePath());
			if (file.exists())
				return file;
		}
		return null;
	}
	
	/**
	 * Method that returns a {@link File} if the path provided is a directory or false otherwise
	 * @param path
	 * 	String path to the directory of interest
	 * @return a {@link File} object for the directory of interest or null if it does not exist
	 */
	public static File browseDirectory(String path) {
		JFileChooser fc = new JFileChooser(); 
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		File dir = new File(path);
		if (dir.exists())
			fc.setCurrentDirectory(dir);

		int ret = fc.showOpenDialog(null); 
		if (ret == JFileChooser.APPROVE_OPTION) {
			File file = new File(fc.getSelectedFile().getAbsolutePath());
			if (file.exists())
				return file;
		}
		return null;
	}
	/**
	 * Delete all the contents of  a directory
	 * @param folder
	 * 	the File which is going to be deleted
	 */
	public static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if (files == null) {
	    	folder.delete();
	    	return;
	    }
        for(File f: files) {
            if(f.isDirectory()) deleteFolder(f);
            else f.delete();
        }
	    folder.delete();
	}
}
