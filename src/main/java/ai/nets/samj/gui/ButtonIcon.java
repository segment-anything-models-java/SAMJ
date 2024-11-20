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
package ai.nets.samj.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
/**
 * ButtonIcon class represents a custom JButton with icons for normal and pressed states.
 * @author Carlos Garcia
 * @author Daniel Sage
 */
public class ButtonIcon extends JButton {
	
	/**
	 * Serial version unique identifier
	 */
	private static final long serialVersionUID = 7396676607184967666L;
	/**
	 * Prefix used in the file name of the images that represent that the button is pressed.
	 */
	private static final String PRESSED_PREFIX = "pressed_";
	/**
	 * Icon for when the button is not pressed
	 */
	private ImageIcon normalIcon;
	/**
	 * Icon for when the button is pressed
	 */
	private ImageIcon pressedIcon;
	/**
	 * String that is displayed inside the button
	 */
	private String text;
	/**
	 * Original color of the buttons
	 */
	private Color color;
	/**
	 * HTML used to format the button label text. First the color needs to be specified, then the 
	 * text that wants to be in the button
	 */
	private static final String BTN_TEXT_HTML = "<html><font color='%s'>%s</font></html>";
	/**
	 * Color code for the HTML String of the button for when the button is not pressed
	 */
	private static final String NOT_PRESSED_COLOR = "black";
	/**
	 * Color code for the HTML String of the button for when the button is pressed
	 */
	private static final String PRESSED_COLOR = "white";

	/**
	 * Constructor. Creates a button that has an icon inside. The icon changes when pressed.
	 * @param text
	 * 	the text inside the button
	 * @param filePath
	 * 	the path to the file that contains the image that is going to be used
	 * @param filename
	 * 	the name of the file that is going to be used
	 */
	public ButtonIcon(String text, String filePath, String filename) {
		super();
		this.text = text;
		try {
			normalIcon = getIcon(filePath + "/" + filename);
			pressedIcon = getIcon(filePath + "/" + PRESSED_PREFIX + filename);
			if (normalIcon != null) {
				setIcon(normalIcon);
				setBorder(BorderFactory.createEtchedBorder());
				setOpaque(false);
				setContentAreaFilled(false);
				setPreferredSize(new Dimension(24, 24));
				setVerticalTextPosition(SwingConstants.BOTTOM);
				setHorizontalTextPosition(SwingConstants.CENTER);
				setText(String.format(BTN_TEXT_HTML, NOT_PRESSED_COLOR, text));
			}
			if (pressedIcon != null) {
				this.setPressedIcon(pressedIcon);
			}
		} 
		catch (Exception ex) {
			setText(text);
		}
		color = this.getBackground();
	}
	
	private ImageIcon getIcon(String path) {
		while (path.indexOf("//") != -1) path = path.replace("//", "/");
		URL url = ButtonIcon.class.getClassLoader().getResource(path);
		if (url == null) {
			File f = findJarFile(ButtonIcon.class);
			if (f.getName().endsWith(".jar")) {
				try (URLClassLoader clsloader = new URLClassLoader(new URL[]{f.toURI().toURL()})){
					url = clsloader.getResource(path);
				} catch (IOException e) {
				}
			}
		}
		if (url != null) {
			ImageIcon img = new ImageIcon(url) ;  
			Image image = img.getImage();
			Image scaled = image.getScaledInstance(18, 18, Image.SCALE_SMOOTH);
			return new ImageIcon(scaled);
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
		if (isPressed) {
			this.setIcon(pressedIcon);
			this.setBackground(Color.BLACK);
			this.setOpaque(true);
			this.setContentAreaFilled(true);
		} else {
			this.setIcon(normalIcon);
			this.setBackground(color);
			this.setOpaque(false);
		}

		setText(String.format(BTN_TEXT_HTML, isPressed ? PRESSED_COLOR : NOT_PRESSED_COLOR, text));
		
		this.setSelected(isPressed);
		this.validate();
		this.repaint();
	}
}
