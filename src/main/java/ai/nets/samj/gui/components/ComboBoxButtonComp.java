package ai.nets.samj.gui.components;

import java.awt.Font;
import java.awt.FontMetrics;
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

    public ComboBoxButtonComp(JComboBox<T> modelCombobox) {
        // Populate the JComboBox with models
        this.cmbBox = modelCombobox;
        btn.setMargin(new Insets(2, 3, 2, 2));

        // Set layout manager to null for absolute positioning
        setLayout(null);

        // Add components to the panel
        add(modelCombobox);
        add(btn);

        // Add a ComponentListener to the button to adjust font size
        btn.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustButtonFont();
            }
        });
    }

    @Override
    public void doLayout() {
        int inset = 5; // Separation between components and edges
        int totalInsets = inset * 3; // Left, middle, and right insets

        int width = getWidth();
        int height = getHeight();

        int availableWidth = width - totalInsets;
        double ratioSum = RATIO_CBX_BTN + 1;

        // Calculate widths based on the ratio
        int comboWidth = (int) Math.round(availableWidth * RATIO_CBX_BTN / ratioSum);
        int btnWidth = availableWidth - comboWidth;

        int x = inset;
        int y = inset;
        int componentHeight = height - (2 * inset); // Account for top and bottom insets

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
