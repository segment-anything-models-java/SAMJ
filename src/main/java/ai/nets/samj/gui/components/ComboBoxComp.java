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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public class ComboBoxComp<T> extends JPanel {

    private static final long serialVersionUID = 2478618937640492286L;

    protected final JComboBox<T> cmbBox;

    /**
     * Creates the component around the supplied combo box.
     *
     * @param modelCombobox combo box to embed
     */
    public ComboBoxComp(JComboBox<T> modelCombobox) {
        this.cmbBox = modelCombobox;

        // Use GridBagLayout instead of null layout
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0); // Adjust insets as needed
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;

        // Add the JComboBox with weightx corresponding to RATIO_CBX_BTN
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(cmbBox, gbc);
    }

    /**
     * Lays out the combo box inside the available horizontal space.
     */
    @Override
    public void doLayout() {
        int inset = 2; // Separation between components and edges
        int totalInset = inset * 2; // Separation between components and edges

        int width = getWidth();
        int height = getHeight();

        int availableWidth = width - totalInset;

        // Calculate widths based on the ratio

        int x = inset;
        int y = 0;
        int componentHeight = height; // Account for top and bottom insets

        // Set bounds for the JComboBox
        cmbBox.setBounds(x, y, availableWidth, componentHeight);
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
            frame.add(new ComboBoxComp(null));
            frame.setSize(400, 100); // Adjust the size as needed
            frame.setVisible(true);
        });
    }
}
