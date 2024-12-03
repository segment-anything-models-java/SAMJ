package ai.nets.samj.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JPanel;


public class ComboBoxComp<T> extends JPanel {

    private static final long serialVersionUID = 2478618937640492286L;

    protected final JComboBox<T> cmbBox;

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
