package ai.nets.samj.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PlusMinusButtonComp extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel;
    private final JButton plusButton;
    private final JButton minusButton;

    public PlusMinusButtonComp(String title, Consumer<String> action) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        titleLabel = new JLabel(title);
        plusButton = new JButton("+");
        minusButton = new JButton("-");

        plusButton.setFocusable(false);
        minusButton.setFocusable(false);

        plusButton.setMargin(new Insets(2, 8, 2, 8));
        minusButton.setMargin(new Insets(2, 8, 2, 8));

        plusButton.addActionListener(e -> action.accept("Points+"));
        minusButton.addActionListener(e -> action.accept("Points-"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        add(titleLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        add(plusButton, gbc);

        gbc.gridx = 2;
        add(minusButton, gbc);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        titleLabel.setEnabled(enabled);
        plusButton.setEnabled(enabled);
        minusButton.setEnabled(enabled);
    }
}