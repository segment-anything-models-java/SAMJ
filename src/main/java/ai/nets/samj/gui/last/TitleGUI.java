package ai.nets.samj.gui.last;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class TitleGUI extends JPanel {

    private static final long serialVersionUID = -1041152913379704899L;

    // No font-size here -> allows JLabel font resizing to work
    private static final String TITLE_HTML =
            "<html><div style='text-align:center;'>"
          + "<span style='color:black;'>SAM</span>"
          + "<span style='color:red;'>J</span>"
          + "</div></html>";

    private static final float MIN_FONT = 12f;
    private static final float MAX_FONT = 120f;

    // Tune this factor to make the title bigger/smaller relative to the panel
    private static final float FONT_SCALE = 0.75f;

    private final JLabel titleLabel;

    public TitleGUI() {
        setBackground(Color.LIGHT_GRAY);
        setOpaque(true);

        setLayout(new BorderLayout());

        titleLabel = new JLabel(TITLE_HTML);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setVerticalAlignment(SwingConstants.CENTER);
        titleLabel.setOpaque(false);

        add(titleLabel, BorderLayout.CENTER);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        Insets in = getInsets();
        int w = Math.max(1, getWidth()  - in.left - in.right);
        int h = Math.max(1, getHeight() - in.top  - in.bottom);

        // Scale based on the limiting dimension
        float target = Math.min(w, h) * FONT_SCALE;
        target = Math.max(MIN_FONT, Math.min(target, MAX_FONT));

        Font base = getFont();
        if (base == null) base = new Font("SansSerif", Font.BOLD, 12);

        Font current = titleLabel.getFont();
        if (current == null || Math.abs(current.getSize2D() - target) > 0.25f) {
            titleLabel.setFont(base.deriveFont(Font.BOLD, target));
        }
    }
}