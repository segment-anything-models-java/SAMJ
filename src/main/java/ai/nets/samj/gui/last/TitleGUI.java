package ai.nets.samj.gui.last;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class TitleGUI extends JPanel {
	
	private static JLabel titleLabel;

    private static final long serialVersionUID = -1041152913379704899L;
    
    private static final String TITLE_TEXT = ""
    		+ "<html><div style='text-align: center; font-size: 15px;'>"
            + "<span style='color: black;'>SAM</span>" + "<span style='color: red;'>J</span>";

    String text = "<html><div style='text-align: center; font-size: 15px;'>"
            + "<span style='color: black;'>SAM</span>" + "<span style='color: red;'>J</span>";

	public TitleGUI() {
		setBackground(Color.LIGHT_GRAY);
		this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		titleLabel = new JLabel(TITLE_TEXT, SwingConstants.CENTER);
    }
    
    @Override
    public void doLayout() {
        int rawW = getWidth();
        int rawH = getHeight();
        int inset = Math.min(rawW, 2);
        inset = Math.min(rawH, 2);

        int w = Math.max(1, getWidth() - inset * 2);
        int h = Math.max(1, getHeight() - inset * 2);

        titleLabel.setBounds(inset, inset, w, h);

        float sizeFromWidth  = w / 10f;     // tweak
        float sizeFromHeight = h * 0.65f;   // tweak
        float newSize = Math.min(sizeFromWidth, sizeFromHeight);

        newSize = Math.max(10f, Math.min(newSize, 96f));

        Font base = getFont();
        if (base == null) base = new Font("SansSerif", Font.BOLD, 12);

        Font current = titleLabel.getFont();
        if (current == null || Math.abs(current.getSize2D() - newSize) > 0.25f) {
            titleLabel.setFont(base.deriveFont(Font.BOLD, newSize));
        }
    }
}
