package ai.nets.samj.gui.last;


import javax.swing.JCheckBox;
import javax.swing.JPanel;

import ai.nets.samj.gui.JSwitchButton;

public class CenterPanelSwitch extends JPanel {


    private static final long serialVersionUID = -4364257664138829685L;
	protected JCheckBox propagate3D;
    protected JSwitchButton chkInstant;

    protected final double SWITCH_HRATIO = 0.8;
    protected final double SWITCH_WRATIO = 0.9;
    protected final double CHCK_WRATIO = 0.95;
    
	public CenterPanelSwitch() {
		setLayout(null);
	    propagate3D = new JCheckBox("Propagate in 3D/time", false);
	    chkInstant = new JSwitchButton("LIVE", "OFF");
    }
    
	@Override
	public void doLayout() {
	    final int gapY = 2;     // vertical space BETWEEN components
	    final int padX = 2;     // horizontal padding on BOTH sides

	    final java.awt.Insets ins = getInsets();

	    int innerW = Math.max(0, getWidth() - ins.left - ins.right - padX * 2);
	    int innerH = Math.max(0, getHeight() - ins.top - ins.bottom);

	    // Two rows => one gap
	    int contentH = Math.max(0, innerH - gapY);

	    // 80% / 20% split
	    int switchH = Math.max(1, (int) Math.round(contentH * SWITCH_HRATIO));
	    int checkH  = Math.max(1, contentH - switchH);

	    // Center the whole group vertically
	    int groupH = switchH + gapY + checkH;
	    int y0 = ins.top + Math.max(0, (innerH - groupH) / 2);

	    // --- Switch button (90% width, centered) ---
	    int swW = Math.max(1, (int) Math.round(innerW * SWITCH_WRATIO));
	    int swX = ins.left + padX + Math.max(0, (innerW - swW) / 2);
	    int swY = y0;

	    chkInstant.setBounds(swX, swY, swW, switchH);

	    // --- Checkbox (needed width up to 90%, centered) ---
	    int cbMaxW = Math.max(1, (int) Math.round(innerW * CHCK_WRATIO));
	    int cbY = swY + switchH + gapY;

	    // Make it look centered even if width is tight
	    propagate3D.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

	    final String fullText = "Propagate in 3D/time";
	    final String ellipsisText = "....";

	    java.awt.Font base = propagate3D.getFont();
	    int startSize = Math.max(6, (int) Math.floor(checkH * 0.75));
	    boolean fitted = false;

	    // Try full text, shrink font down to 6 until it fits in BOTH width and height
	    for (int sz = startSize; sz >= 6; sz--) {
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
	        for (int sz = startSize; sz >= 6; sz--) {
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
	            propagate3D.setFont(base.deriveFont(6f));
	            propagate3D.setText(ellipsisText);
	        }
	    }

	    // Width = "all that is needed" but capped at 90% of inner width, centered
	    java.awt.Dimension cbPref = propagate3D.getPreferredSize();
	    int cbW = Math.max(1, Math.min(cbMaxW, cbPref.width));
	    int cbX = ins.left + padX + Math.max(0, (innerW - cbW) / 2);

	    propagate3D.setBounds(cbX, cbY, cbW, checkH);
	}
}
