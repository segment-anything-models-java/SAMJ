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

    public CustomInsetsJLabel(Icon icon, int top, int left, int bottom, int right) {
        super(icon);
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        setHorizontalAlignment(CENTER);
        setVerticalAlignment(CENTER);
    }

    @Override
    public Insets getInsets() {
        return new Insets(top, left, bottom, right);
    }

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
