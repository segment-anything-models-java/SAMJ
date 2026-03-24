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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class ComboBoxButtonComp<T> extends JPanel {

    private static final long serialVersionUID = 2478618937640492286L;

    protected final JComboBox<T> cmbBox;
    protected JButton btn = new JButton("▶");
    private static final double RATIO_CBX_BTN = 10.0;

    /**
     * Creates the composite control around the supplied combo box.
     *
     * @param modelCombobox combo box to embed
     */
    public ComboBoxButtonComp(JComboBox<T> modelCombobox) {
        this.cmbBox = modelCombobox;
        btn.setMargin(new Insets(2, 3, 2, 2));

        // Use GridBagLayout instead of null layout
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0); // Adjust insets as needed
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;

        // Add the JComboBox with weightx corresponding to RATIO_CBX_BTN
        gbc.gridx = 0;
        gbc.weightx = RATIO_CBX_BTN;
        gbc.weighty = 1;
        add(cmbBox, gbc);

        // Add the JButton with weightx of 1
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(btn, gbc);

        // Add a ComponentListener to the button to adjust font size
        btn.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustButtonFont();
            }
        });
    }

    /**
     * Lays out the combo box and the side button according to the configured size ratio.
     */
    @Override
    public void doLayout() {
        int inset = 2; // Separation between components and edges
        int totalInsets = inset * 3; // Left, middle, and right insets

        int width = getWidth();
        int height = getHeight();

        int availableWidth = width - totalInsets;
        double ratioSum = RATIO_CBX_BTN + 1;

        // Calculate widths based on the ratio
        int comboWidth = (int) Math.round(availableWidth * RATIO_CBX_BTN / ratioSum);
        int btnWidth = availableWidth - comboWidth;

        int x = inset;
        int y = 0;
        int componentHeight = height; // Account for top and bottom insets

        // Set bounds for the JComboBox
        cmbBox.setBounds(x, y, comboWidth, componentHeight);

        x += comboWidth + inset; // Move x position for the JButton

        // Set bounds for the JButton
        btn.setBounds(x, y, btnWidth, componentHeight);

        // Adjust font size after layout
        adjustButtonFont();
    }

    // Method to adjust the font size based on button size
    private void adjustButtonFont() {
        int btnHeight = btn.getHeight();
        int btnWidth = btn.getWidth();

        if (btnHeight <= 0 || btnWidth <= 0) {
            return; // Cannot calculate font size with non-positive dimensions
        }

        // Get the button's insets
        Insets insets = btn.getInsets();
        int availableWidth = btnWidth - insets.left - insets.right;

        // Start with a font size based on button height
        int fontSize = btnHeight - insets.top - insets.bottom;// - 4; // Subtract padding

        // Get the current font
        Font originalFont = btn.getFont();
        Font font = originalFont.deriveFont((float) fontSize);

        FontMetrics fm = btn.getFontMetrics(font);
        int textWidth = fm.stringWidth(btn.getText());

        // Reduce font size until text fits
        while (textWidth > availableWidth && fontSize > 0) {
            fontSize--;
            font = originalFont.deriveFont((float) fontSize);
            fm = btn.getFontMetrics(font);
            textWidth = fm.stringWidth(btn.getText());
        }

        // Apply the new font
        btn.setFont(font);

        // Center the text
        btn.setHorizontalAlignment(JButton.CENTER);
        btn.setVerticalAlignment(JButton.CENTER);
    }

    /**
     * Launches a small demo frame for this component.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Model Selection");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(new ComboBoxButtonComp(null));
            frame.setSize(400, 100); // Adjust the size as needed
            frame.setVisible(true);
        });
    }
}
