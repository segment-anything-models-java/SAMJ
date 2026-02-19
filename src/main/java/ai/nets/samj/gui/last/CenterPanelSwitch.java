package ai.nets.samj.gui.last;


import java.awt.CardLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ai.nets.samj.gui.JSwitchButton;

public class CenterPanelSwitch extends JPanel {


    private static final long serialVersionUID = -4364257664138829685L;
	protected JCheckBox propagate3D;
    protected JSwitchButton chkInstant;
    protected JLabel warningLabel;
    protected JPanel cardPanel;

    protected static final String WARNING_MESSAGE = ""
    		+ "<html><span style='color:orange; font-size:%spx;'>&#9888; Only rect and points!</span></html>";
    protected static final String WARNING_MESSAGE_ELLIPSIS = ""
    		+ "<html><span style='color:orange; font-size:12px;'>&#9888; ...</span></html>";

    protected static final double SWITCH_HRATIO = 0.575;
    protected static final double MESSAGE_HRATIO = 0.15;
    protected static final double SWITCH_WRATIO = 0.9;
    protected static final double CHCK_WRATIO = 0.95;
    
    protected static final int MIN_FONT_SIZE = 8;
    protected static final int BIG_GAP_Y = 5;
    protected static final int SMALL_GAP_Y = 2;
    protected static final int GAP_X = 2;

    protected static final String NO_MSG = "NO MESSAGE";
    protected static final String YES_MSG = "YES MESSAGE";
    
	public CenterPanelSwitch() {
		setLayout(null);
	    propagate3D = new JCheckBox("Propagate in 3D/time", false);
	    propagate3D.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    propagate3D.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
	    chkInstant = new JSwitchButton("LIVE", "OFF");
	    cardPanel = new JPanel(new CardLayout());
	    warningLabel = new JLabel(String.format(WARNING_MESSAGE, 12));
	    this.warningLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    this.warningLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);

        cardPanel.add(new JPanel() {private static final long serialVersionUID = 1L; { setOpaque(false); }}, NO_MSG);
        cardPanel.add(warningLabel, YES_MSG);
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, YES_MSG);
	    add(chkInstant);
	    add(cardPanel);
	    add(propagate3D);
    }
    
	@Override
	public void doLayout() {

	    int contentH = Math.max(0, getHeight() - BIG_GAP_Y - 3 * SMALL_GAP_Y);


	    int switchH = Math.max(1, (int) Math.round(contentH * SWITCH_HRATIO));
	    int msgH = Math.max(1, (int) Math.round(contentH * MESSAGE_HRATIO));
	    int checkH  = Math.max(1, contentH - msgH - switchH);

	    // --- Switch button (90% width, centered) ---
	    int swW = Math.max(1, (int) Math.min(getWidth() - GAP_X * 2, Math.round(getWidth() * SWITCH_WRATIO)));
	    int swX = Math.max(0, (getWidth() - swW) / 2);
	    int swY = BIG_GAP_Y;

	    chkInstant.setBounds(swX, swY, swW, switchH);

	    // --- WARNING (needed width up to 95%, centered) ---
	    int msgMaxW = Math.max(1, (int) Math.min(getWidth() - GAP_X * 2, Math.round(getWidth() * CHCK_WRATIO)));
	    int msgY = swY + switchH + SMALL_GAP_Y;

	    int startSize = Math.max(MIN_FONT_SIZE, (int) Math.floor(msgH * 0.75));
	    boolean fitted = false;

	    for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	        warningLabel.setText(String.format(WARNING_MESSAGE, sz));
	        java.awt.Dimension pref = warningLabel.getPreferredSize();
	        if (pref.width <= msgMaxW && pref.height <= msgH) {
	            fitted = true;
	            break;
	        }
	    }
	    if (!fitted) {
	        warningLabel.setText(WARNING_MESSAGE_ELLIPSIS);
	    }

	    java.awt.Dimension msgPref = warningLabel.getPreferredSize();
	    int msgW = Math.max(1, Math.min(msgMaxW, msgPref.width));
	    int msgX = Math.max(0, (getWidth() - msgW) / 2);

	    // IMPORTANT: size/position the CARD PANEL (CardLayout will size the label)
	    cardPanel.setBounds(msgX, msgY, msgW, msgH);
	    
	    	

	    // --- Checkbox (needed width up to 90%, centered) ---
	    int cbMaxW = Math.max(1, (int) Math.min(getWidth() - GAP_X * 2, Math.round(getWidth() * CHCK_WRATIO)));
	    int cbY = msgY + msgH + SMALL_GAP_Y;

	    final String fullText = "Propagate in 3D/time";
	    final String ellipsisText = "....";

	    java.awt.Font base = propagate3D.getFont();
	    startSize = Math.max(MIN_FONT_SIZE, (int) Math.floor(checkH * 0.75));
	    fitted = false;

	    // Try full text, shrink font down to 6 until it fits in BOTH width and height
	    for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	        java.awt.Font f = base.deriveFont((float) sz);
	        propagate3D.setFont(f);
	        propagate3D.setText(fullText);

	        java.awt.Dimension pref = propagate3D.getPreferredSize();
	        if (pref.width <= cbMaxW && pref.height <= checkH) {
	            fitted = true;
	            break;
	        }
	    }

	    // If still doesn't fit, use "...." (and shrink if needed)
	    if (!fitted) {
	        for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	            java.awt.Font f = base.deriveFont((float) sz);
	            propagate3D.setFont(f);
	            propagate3D.setText(ellipsisText);

	            java.awt.Dimension pref = propagate3D.getPreferredSize();
	            if (pref.width <= cbMaxW && pref.height <= checkH) {
	                fitted = true;
	                break;
	            }
	        }
	        if (!fitted) {
	            // last resort: keep "...." and min font
	            propagate3D.setFont(base.deriveFont(MIN_FONT_SIZE));
	            propagate3D.setText(ellipsisText);
	        }
	    }

	    // Width = "all that is needed" but capped at 90% of inner width, centered
	    java.awt.Dimension cbPref = propagate3D.getPreferredSize();
	    int cbW = Math.max(1, Math.min(cbMaxW, cbPref.width));
	    int cbX = Math.max(0, (getWidth() - cbW) / 2);

	    propagate3D.setBounds(cbX, cbY, cbW, checkH);
	}
}
