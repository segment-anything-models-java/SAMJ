package ai.nets.samj.gui.last;


import java.awt.CardLayout;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import ai.nets.samj.utils.Constants;

public class CenterPanelBatch extends JPanel {


	private static final long serialVersionUID = 7358709850496059408L;
	protected JCheckBox propagate3D;
    protected JButton btnBatchSAMize;
    protected JProgressBar batchProgress;
    protected JButton stopProgressBtn;
    protected HelpBadge questionMark;
    protected JPanel cardPanel;
    protected JLabel warningLabel;

    protected static final String WARNING_MESSAGE = ""
    		+ "<html><font style='color:orange; font-size:%spx;'>&#9888; No prompt was provided!</font></html>";
    protected static final String WARNING_MESSAGE_ELLIPSIS = ""
    		+ "<html><span style='color:orange; font-size:12px;'>&#9888; ...</span></html>";

    
    private static final int X_INSET = 2;
    private static final int SMALL_Y_INSET = 2;
    private static final int BIG_Y_INSET = 5;
    private static final int BUTTON_PROGRESS_Y_INSET = 2;

	private static final double BUTTON_PROGRESS_HRATIO = 0.575;
	private static final double BUTTON_OVER_PROGRESS_HRATIO = 1.75;
	private static final double BUTTON_OVER_HELP_HRATIO = 1.2;
	private static final double WARNING_HRATIO = 0.15;
	
	private static final double WRATIO = 0.95;

    protected static final double CHCK_WRATIO = 0.95;
	private static final int    MIN_FONT_SIZE       = 8;
	private static final String CHECK_TEXT          = "Propagate in 3D/time";
	private static final String ELLIPSIS_TEXT       = "....";

    protected static final String NO_MSG = "NO MESSAGE";
    protected static final String YES_MSG = "YES MESSAGE";
    
	public CenterPanelBatch() {
		setLayout(null);
		
	    btnBatchSAMize = new JButton("Batch SAMize");
        Icon questionIcon = UIManager.getIcon("OptionPane.questionIcon");
        if (questionIcon == null) {
            questionIcon = UIManager.getIcon("OptionPane.informationIcon");
        }
	    questionMark = new HelpBadge(Constants.HELP_MSG);

	    batchProgress = new JProgressBar();
	    stopProgressBtn = new JButton("■");
	    stopProgressBtn.setMargin(new java.awt.Insets(1, 1, 1, 1)); // small, symmetric insets
	    stopProgressBtn.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    stopProgressBtn.setVerticalAlignment(javax.swing.SwingConstants.CENTER);

	    cardPanel = new JPanel(new CardLayout());
	    warningLabel = new JLabel(String.format(WARNING_MESSAGE, 12));
	    this.warningLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    this.warningLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);

        cardPanel.add(new JPanel() {private static final long serialVersionUID = 1L; { setOpaque(false); }}, NO_MSG);
        cardPanel.add(warningLabel, YES_MSG);
        ((CardLayout) cardPanel.getLayout()).show(cardPanel, NO_MSG);
        
	    propagate3D = new JCheckBox("Propagate in 3D/time", false);
	    propagate3D.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    propagate3D.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
	    
	    add(btnBatchSAMize);
	    add(questionMark);
	    add(batchProgress);
	    add(stopProgressBtn);
	    add(cardPanel);
	    add(propagate3D);
    }

	@Override
	public void doLayout() {
	    int contentH = Math.max(0, getHeight() - BIG_Y_INSET - 3 * SMALL_Y_INSET);
		int buttonProgressH = (int) Math.round(Math.max(0, BUTTON_PROGRESS_HRATIO * contentH));
		int buttonH = (int) Math.round(Math.max(0, (buttonProgressH - BUTTON_PROGRESS_Y_INSET) * BUTTON_OVER_PROGRESS_HRATIO / (BUTTON_OVER_PROGRESS_HRATIO + 1)));
		int progressH = (int) Math.round(Math.max(0, (buttonProgressH - BUTTON_PROGRESS_Y_INSET) / (BUTTON_OVER_PROGRESS_HRATIO + 1)));
		int warningH = (int) Math.round(Math.max(0, WARNING_HRATIO * contentH));
		int checkH = Math.round(Math.max(0, contentH - buttonProgressH - warningH));
		
		int commonW = (int) Math.round(Math.max(0, Math.min(getWidth() - X_INSET * 2, getWidth() * WRATIO)));

	    // ---------- Row 1: [Batch button] + [question icon label on the right] ----------
		int y = BIG_Y_INSET;
		int x = Math.max(0, (getWidth() - commonW) / 2);
		int questionH = (int) (buttonH / BUTTON_OVER_HELP_HRATIO);
	    btnBatchSAMize.setBounds(x, y, Math.max(0, commonW - questionH), buttonH);
	    questionMark.setBounds(x + Math.max(0, commonW - questionH), y + (buttonH - questionH) / 2, questionH, questionH);

	    // ---------- Row 2: [progress bar] + [stop button immediately to its right] ----------
	    // Stop button must be square; keep it as large as the row height if possible.
	    y += BUTTON_PROGRESS_Y_INSET + buttonH;
	    batchProgress.setBounds(x, y, Math.max(0, commonW - progressH), progressH);
	    stopProgressBtn.setBounds(x + Math.max(0, commonW - progressH), y, progressH, progressH);
	    
	    // Row 3: warning message
	    int msgMaxW = Math.max(1, (int) Math.min(getWidth() - X_INSET * 2, Math.round(getWidth() * CHCK_WRATIO)));
	    int msgY = y + SMALL_Y_INSET + progressH;

	    int startSize = Math.max(MIN_FONT_SIZE, (int) Math.floor(warningH * 0.75));
	    boolean fitted = false;

	    for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	        warningLabel.setText(String.format(WARNING_MESSAGE, sz));
	        java.awt.Dimension pref = warningLabel.getPreferredSize();
	        if (pref.width <= msgMaxW && pref.height <= warningH) {
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
	    cardPanel.setBounds(msgX, msgY, msgW, warningH);

	    
    	

	    // --- Checkbox (needed width up to 90%, centered) ---
	    int cbMaxW = Math.max(1, (int) Math.min(getWidth() - X_INSET * 2, Math.round(getWidth() * CHCK_WRATIO)));
	    int cbY = msgY + SMALL_Y_INSET + warningH;

	    final String fullText = CHECK_TEXT;
	    final String ellipsisText = ELLIPSIS_TEXT;

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
