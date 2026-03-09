package ai.nets.samj.gui.components;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class PlusMinusButtonComp extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titleLabel;
    private final JLabel plusLabel;
    private final JLabel minusLabel;

    public PlusMinusButtonComp(String title, Consumer<String> action) {
        setLayout(new BorderLayout(6, 0));
        setOpaque(false);

        titleLabel = new JLabel(title);
        plusLabel = createClickableLabel("+", "Points+");
        minusLabel = createClickableLabel("-", "Points-");

        JPanel rightPanel = new JPanel(new GridLayout(1, 2, 4, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(plusLabel);
        rightPanel.add(minusLabel);

        add(titleLabel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JLabel src = (JLabel) e.getSource();
                action.accept(src.getName());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                ((JLabel) e.getSource()).setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(getForeground()),
                                BorderFactory.createEmptyBorder(1, 6, 1, 6)
                        )
                );
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ((JLabel) e.getSource()).setBorder(
                        BorderFactory.createEmptyBorder(2, 7, 2, 7)
                );
            }
        };

        plusLabel.addMouseListener(listener);
        minusLabel.addMouseListener(listener);
    }

    private JLabel createClickableLabel(String text, String actionCommand) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setName(actionCommand);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        return lbl;
    }

    public void setEnabledButtons(boolean enabled) {
        plusLabel.setEnabled(enabled);
        minusLabel.setEnabled(enabled);
        plusLabel.setCursor(enabled
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        minusLabel.setCursor(enabled
                ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        titleLabel.setEnabled(enabled);
        setEnabledButtons(enabled);
    }

    public Insets getInsets() {
        return new Insets(2, 2, 2, 2);
    }
}