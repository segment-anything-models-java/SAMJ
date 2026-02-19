package ai.nets.samj.gui.last;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;

public class HelpBadge extends JComponent {
    private static final long serialVersionUID = 1L;

    private static final int PREF = 20;      // default badge size
    private static final int PAD  = 2;       // inner padding
    private static final float STROKE = 1.2f;
    

	 // inside HelpBadge
	 private final int oldInitial = ToolTipManager.sharedInstance().getInitialDelay();
	 private final int oldReshow  = ToolTipManager.sharedInstance().getReshowDelay();
	 private final int oldDismiss = ToolTipManager.sharedInstance().getDismissDelay();

    private boolean hover = false;

    public HelpBadge(String tooltip) {
        setToolTipText(tooltip);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
        });
        setToolTipText(tooltip);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                ToolTipManager.sharedInstance().setInitialDelay(10);
                ToolTipManager.sharedInstance().setReshowDelay(0);
                ToolTipManager.sharedInstance().setDismissDelay(10000);
            }
            @Override public void mouseExited(MouseEvent e) {
                ToolTipManager.sharedInstance().setInitialDelay(oldInitial);
                ToolTipManager.sharedInstance().setReshowDelay(oldReshow);
                ToolTipManager.sharedInstance().setDismissDelay(oldDismiss);
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PREF, PREF);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int s = Math.min(w, h);
            int x = (w - s) / 2;
            int y = (h - s) / 2;

            // Colors (tries to respect LAF a bit, with solid fallbacks)
            Color base = UIManagerColor("Actions.Blue", new Color(0x2F80ED)); // nice blue
            Color fill = hover ? base.brighter() : base;
            Color stroke = fill.darker();

            // Soft shadow
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(x + 1, y + 2, s - 2, s - 2);

            // Circle fill
            g2.setColor(fill);
            g2.fillOval(x + PAD, y + PAD, s - 2 * PAD, s - 2 * PAD);

            // Circle stroke
            g2.setStroke(new BasicStroke(STROKE));
            g2.setColor(stroke);
            g2.drawOval(x + PAD, y + PAD, s - 2 * PAD, s - 2 * PAD);

            // Draw "?" (center using glyph visual bounds)
            float fontSize = (s - 2 * PAD) * 0.78f;
            Font baseFont = getFont();
            if (baseFont == null) baseFont = new Font("SansSerif", Font.BOLD, 12);
            Font f = baseFont.deriveFont(Font.BOLD, fontSize);
            g2.setFont(f);

            String q = "?";
            java.awt.font.FontRenderContext frc = g2.getFontRenderContext();
            java.awt.font.GlyphVector gv = f.createGlyphVector(frc, q);
            java.awt.geom.Rectangle2D vb = gv.getVisualBounds(); // visual (what you see), not advance

            // center within the inner circle area
            int innerX = x + PAD;
            int innerY = y + PAD;
            int innerS = s - 2 * PAD;

            double tx = innerX + (innerS - vb.getWidth()) / 2.0 - vb.getX();
            double ty = innerY + (innerS - vb.getHeight()) / 2.0 - vb.getY();

            g2.setColor(Color.WHITE);
            g2.drawGlyphVector(gv, (float) tx, (float) ty);

        } finally {
            g2.dispose();
        }
    }

    private static Color UIManagerColor(String key, Color fallback) {
        Color c = javax.swing.UIManager.getColor(key);
        return (c != null) ? c : fallback;
    }
}