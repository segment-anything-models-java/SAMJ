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

    public ComboBoxButtonComp(JComboBox<T> modelCombobox) {
        this.cmbBox = modelCombobox;
        btn.setMargin(new Insets(2, 3, 2, 2));

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = RATIO_CBX_BTN;
        gbc.weighty = 1;
        add(cmbBox, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        add(btn, gbc);

        // Toggle direction on each click
        btn.addActionListener(e -> {
            toggleLabel();
            adjustButtonFont();
        });

        btn.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustButtonFont();
            }
        });
    }

    @Override
    public void doLayout() {
        int inset = 2;
        int totalInsets = inset * 3;

        int width = getWidth();
        int height = getHeight();

        int availableWidth = width - totalInsets;
        double ratioSum = RATIO_CBX_BTN + 1;

        int comboWidth = (int) Math.round(availableWidth * RATIO_CBX_BTN / ratioSum);
        int btnWidth = availableWidth - comboWidth;

        int x = inset;
        int y = 0;
        int componentHeight = height;

        cmbBox.setBounds(x, y, comboWidth, componentHeight);

        x += comboWidth + inset;

        btn.setBounds(x, y, btnWidth, componentHeight);

        adjustButtonFont();
    }

    @Override
    public void setEnabled(boolean isEnabled) {
    	this.cmbBox.setEnabled(isEnabled);
    	this.btn.setEnabled(isEnabled);
    	super.setEnabled(isEnabled);
    }

    private void adjustButtonFont() {
        int btnHeight = btn.getHeight();
        int btnWidth = btn.getWidth();

        if (btnHeight <= 0 || btnWidth <= 0) {
            return;
        }

        Insets insets = btn.getInsets();
        int availableWidth = btnWidth - insets.left - insets.right;

        int fontSize = btnHeight - insets.top - insets.bottom;

        Font originalFont = btn.getFont();
        Font font = originalFont.deriveFont((float) fontSize);

        FontMetrics fm = btn.getFontMetrics(font);
        int textWidth = fm.stringWidth(btn.getText());

        while (textWidth > availableWidth && fontSize > 0) {
            fontSize--;
            font = originalFont.deriveFont((float) fontSize);
            fm = btn.getFontMetrics(font);
            textWidth = fm.stringWidth(btn.getText());
        }

        btn.setFont(font);
        btn.setHorizontalAlignment(JButton.CENTER);
        btn.setVerticalAlignment(JButton.CENTER);
    }
    
    public void toggleLabel() {
    	btn.setText("▶".equals(btn.getText()) ? "◀" : "▶");
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("Model Selection");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(new ComboBoxButtonComp<>(new JComboBox<>()));
            frame.setSize(400, 100);
            frame.setVisible(true);
        });
    }
}