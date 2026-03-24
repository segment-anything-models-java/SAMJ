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
package ai.nets.samj.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * A JLabel with custom insets that ensures the icon touches all edges of the border.
 * 
 * @author Carlos Garcia
 */
public class CustomInsetsJLabel extends JLabel {
    private static final long serialVersionUID = 177134806911886339L;

    private int top;
    private int left;
    private int bottom;
    private int right;

    /**
     * Creates a label whose icon respects the supplied custom insets.
     *
     * @param icon icon to display
     * @param top top inset in pixels
     * @param left left inset in pixels
     * @param bottom bottom inset in pixels
     * @param right right inset in pixels
     */
    public CustomInsetsJLabel(Icon icon, int top, int left, int bottom, int right) {
        super(icon);
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
    }

    /**
     * Returns the custom insets used to position the icon.
     *
     * @return the configured insets
     */
    @Override
    public Insets getInsets() {
        return new Insets(top, left, bottom, right);
    }

    /**
     * Computes the preferred size including icon, insets, and border.
     *
     * @return the preferred component size
     */
    @Override
    public Dimension getPreferredSize() {
        // Calculate the preferred size based on icon size, insets, and border
        int width = 0;
        int height = 0;

        if (getIcon() != null) {
            width = getIcon().getIconWidth();
            height = getIcon().getIconHeight();
        }

        Insets insets = getInsets();
        if (insets != null) {
            width += insets.left + insets.right;
            height += insets.top + insets.bottom;
        }

        Border border = getBorder();
        if (border != null) {
            Insets borderInsets = border.getBorderInsets(this);
            width += borderInsets.left + borderInsets.right;
            height += borderInsets.top + borderInsets.bottom;
        }

        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Ensure the icon is painted precisely
        int x = 0;
        int y = 0;

        Border border = getBorder();
        if (border != null) {
            Insets borderInsets = border.getBorderInsets(this);
            x += borderInsets.left;
            y += borderInsets.top;
        }

        Insets insets = getInsets();
        if (insets != null) {
            x += insets.left;
            y += insets.top;
        }

        if (getIcon() != null) {
            getIcon().paintIcon(this, g, x, y);
        }
    }
    
    /**
     * Launches a small demo frame for this component.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Icon icon = UIManager.getIcon("OptionPane.questionIcon");
            if (icon == null) {
                icon = UIManager.getIcon("OptionPane.errorIcon");
            }

            CustomInsetsJLabel label = new CustomInsetsJLabel(icon, 4, 2, 0, 0);
            label.setBorder(LineBorder.createBlackLineBorder());
            label.setToolTipText("Test Tooltip");

            JFrame frame = new JFrame("Test CustomInsetsJLabel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());
            frame.add(label);
            frame.pack();
            frame.setVisible(true);
        });
    }
}
