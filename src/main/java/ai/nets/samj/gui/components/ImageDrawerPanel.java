/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 - 2026 SAMJ developers.
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
package ai.nets.samj.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import ai.nets.samj.gui.HTMLPane;

public class ImageDrawerPanel extends JPanel {

    private static final long serialVersionUID = -8070874774405110221L;
	private JLabel drawerTitle = new JLabel();
    private JButton install = new JButton("Install");
    private JButton uninstall = new JButton("Uninstall");
    
    private int hSize;
    
    private static final String MODEL_TITLE = "<html><div style='text-align: center; font-size: 15px;'>%s</html>";
	
	
	private ImageDrawerPanel(int hSize) {
		this.hSize = hSize;
		createDrawerPanel();
	}
	
	/**
	 * Creates a drawer panel with the requested preferred width.
	 *
	 * @param hSize preferred width in pixels
	 * @return a new {@link ImageDrawerPanel}
	 */
	public static ImageDrawerPanel create(int hSize) {
		return new ImageDrawerPanel(hSize);
	}

    private void createDrawerPanel() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        drawerTitle.setText(String.format(MODEL_TITLE, "&nbsp;"));
        this.add(drawerTitle, BorderLayout.NORTH);
        this.add(createInstallModelComponent(), BorderLayout.SOUTH);
        HTMLPane html = new HTMLPane("Arial", "#000", "#CCCCCC", 200, 200);
        html.append("Model description");
        html.append("Model description");
        html.append("Model description");
        html.append("");
        html.append("i", "Other information");
        html.append("i", "References");
        this.add(html, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(hSize, 0)); // Set preferred width
    }

    // Method to create the install model component
    private JPanel createInstallModelComponent() {
        JPanel thirdComponent = new JPanel();
        thirdComponent.setLayout(new GridBagLayout());
        thirdComponent.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        thirdComponent.setBorder(new LineBorder(Color.BLACK));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // Install button
        gbc.gridx = 0;
        thirdComponent.add(this.install, gbc);

        // Uninstall button
        gbc.gridx = 1;
        thirdComponent.add(this.uninstall, gbc);

        return thirdComponent;
    }
    
    /**
     * Updates the title shown at the top of the drawer.
     *
     * @param title title text to display
     */
    public void setTitle(String title) {
        drawerTitle.setText(String.format(MODEL_TITLE, title));
    }
    
    /**
     * Shows or hides the drawer panel.
     *
     * @param visible whether the drawer should be visible
     */
    @Override
    public void setVisible(boolean visible) {
    	super.setVisible(visible);
    }

}
