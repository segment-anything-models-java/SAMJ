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
package ai.nets.samj.gui.icons;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
/**
 * This class is a normal button that while pressed displays a loading animation
 * @author Carlos Garcia
 * @author Daniel Sage
 */
public class LoadingButton extends JButton {
	/**
	 * Serial version unique identifier
	 */
	private static final long serialVersionUID = -286903277589716471L;
	/**
	 * Icon for when the button is pressed
	 */
	private ImageIcon gifIcon;
	/**
	 * Label containing the text that the button displays when it is not pressed
	 */
	private JLabel textLabel;
	/**
	 * String that is displayed inside the button
	 */
	private String text;
	/**
	 * Label containing the loading animation to be displayed while the button is pressed
	 */
	private JLabel gifLabel;
	/**
	 * HTML used to format the button label text. First the color needs to be specified, then the 
	 * text that wants to be in the button
	 */
	private static final String BTN_TEXT_HTML = "<html><font color='%s'>%s</font></html>";
	/**
	 * Color code for the HTML String of the button for when the button is enabled
	 */
	private static final String ENABLED_COLOR = "black";
	/**
	 * Color code for the HTML String of the button for when the button is disabled
	 */
	private static final String DISABLED_COLOR = "gray";

	/**
	 * Constructor. Creates a button that has an icon inside. The icon changes when pressed.
	 * @param text
	 * 	the text inside the button
	 * @param filePath
	 * 	the path to the file that contains the image that is going to be used
	 * @param filename
	 * 	the name of the file that is going to be used
	 * @param animationSize
	 * 	size of the side of the squared animation inside the button
	 */
	public LoadingButton(String text, String filePath, String filename, double animationSize) {
		super();
		this.text = text;
        textLabel = new JLabel(String.format(BTN_TEXT_HTML, DISABLED_COLOR, text));
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(textLabel);
        gifIcon = getIcon(filePath + "/" + filename, (int) animationSize);
        // Create JLabel to display GIF animation
        gifLabel = new JLabel(gifIcon);
        gifLabel.setVisible(false); // Initially hide GIF animation
        add(gifLabel);
	}
	
	@Override
	public void setEnabled(boolean isEnabled) {
		textLabel.setText(String.format(BTN_TEXT_HTML, isEnabled ? ENABLED_COLOR : DISABLED_COLOR, text));
		super.setEnabled(isEnabled);
	}
	
	private ImageIcon getIcon(String path, int smallestSide) {
		while (path.indexOf("//") != -1) path = path.replace("//", "/");
		URL url = LoadingButton.class.getClassLoader().getResource(path);
		if (url == null) {
			File f = findJarFile(LoadingButton.class);
			if (f.getName().endsWith(".jar")) {
				try (URLClassLoader clsloader = new URLClassLoader(new URL[]{f.toURI().toURL()})){
					url = clsloader.getResource(path);
				} catch (IOException e) {
				}
			}
		}
		if (url != null) {
			ImageIcon icon = new ImageIcon(url);
			int width = icon.getIconWidth();
			int height = icon.getIconHeight();
			int min = Math.min(width, height);
			double scale = (double) smallestSide / (double) min;
			icon.setImage(icon.getImage().getScaledInstance((int) (width * scale), (int) (height * scale), Image.SCALE_DEFAULT));
			return icon;
		}
		return null;
	}
	
	private static File findJarFile(Class<?> clazz) {
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        if (protectionDomain != null) {
            CodeSource codeSource = protectionDomain.getCodeSource();
            if (codeSource != null) {
                URL location = codeSource.getLocation();
                if (location != null) {
                    try {
                        return new File(URI.create(location.toURI().getSchemeSpecificPart()).getPath());
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
	
	/**
	 * Set the button as pressed or not pressed, changing the image displayed
	 * @param isPressed
	 * 	whether the button is pressed or not
	 */
	public void setPressed(boolean isPressed) {
		super.setEnabled(!isPressed);
        gifLabel.setVisible(isPressed);
		this.setSelected(isPressed);
	}
}
