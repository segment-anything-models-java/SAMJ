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

import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JButton;
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
	
	private int horizontalInset;
	private int verticalInset;

	/**
	 * Constructor. Creates a button that has an icon inside. The icon changes when pressed.
	 * @param text
	 * 	the text inside the button
	 * @param fontSize
	 * 	the size of the font that we want in the button
	 * @param horizontalInset
	 * 	horizontal distance from the letter to the sides
	 * @param verticalInset
	 * 	vertical distance from the buttons to the sizes
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

    /**
     * 
     * @param fontSize
     * 	the font size of the button
     */
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
}
