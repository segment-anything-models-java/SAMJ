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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
/**
 * TODO make it resizable
 * This class is a normal button whose font is resized when the component is resized
 * @author Carlos Garcia
 */
public class ResizableButton extends JButton {
	/**
	 * Serial version unique identifier
	 */
	private static final long serialVersionUID = -958367053852506146L;
	/**
	 * Label containing the text that the button displays when it is not pressed
	 */
	private JLabel textLabel;
	/**
	 * Label containing the loading animation to be displayed while the button is pressed
	 */
	private int fontSize;
	
	private int horizontalInset;
	private int verticalInset;

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
    public ResizableButton(String text, int fontSize, int horizontalInset, int verticalInset) {
        super(text);
        this.horizontalInset = horizontalInset;
        this.verticalInset = verticalInset;
        setFont(getFont().deriveFont((float) fontSize));
        setMargin(new Insets(verticalInset, horizontalInset, verticalInset, horizontalInset));
        // Add a ComponentListener to the button to adjust font size
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                //adjustButtonFont();
            }
        });
        //adjustButtonFont();
    }

    public void setFontSize(int fontSize) {
        setFont(getFont().deriveFont((float) fontSize));
    }

    @Override
    public Insets getInsets() {
        return new Insets(verticalInset, horizontalInset, verticalInset, horizontalInset);
    }

    @Override
    public Insets getInsets(Insets insets) {
        return new Insets(verticalInset, horizontalInset, verticalInset, horizontalInset);
    }

    // Method to adjust the font size based on button size
    private void adjustButtonFont() {
        int btnHeight = this.getHeight();
        int btnWidth = this.getWidth();

        if (btnHeight <= 0 || btnWidth <= 0) {
            return; // Cannot calculate font size with non-positive dimensions
        }

        // Get the button's insets
        Insets insets = this.getInsets();
        int availableWidth = btnWidth - insets.left - insets.right;

        // Start with a font size based on button height
        int fontSize = btnHeight - insets.top - insets.bottom;

        // Get the current font
        Font originalFont = this.getFont();
        Font font = originalFont.deriveFont((float) fontSize);

        FontMetrics fm = this.getFontMetrics(font);
        int textWidth = fm.stringWidth(this.getText());

        // Reduce font size until text fits
        while (textWidth > availableWidth && fontSize > 0) {
            fontSize--;
            font = originalFont.deriveFont((float) fontSize);
            fm = this.getFontMetrics(font);
            textWidth = fm.stringWidth(this.getText());
        }

        // Apply the new font
        this.setFont(font);

        // Center the text
        this.setHorizontalAlignment(JButton.CENTER);
        this.setVerticalAlignment(JButton.CENTER);
    }
}
