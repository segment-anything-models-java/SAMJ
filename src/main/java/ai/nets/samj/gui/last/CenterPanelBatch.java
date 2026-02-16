package ai.nets.samj.gui.last;


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
    protected JLabel questionMark;

    
	// Put these near the top of the class (easy to tweak)
	private static final double VCONTENT_HRATIO      = 0.80;  // rows+gaps occupy 80% of inner height
	private static final double ROW_WRATIO          = 0.95;  // each row uses 95% of inner width
	private static final double ROW_GAP_HRATIO      = 0.10;  // gap size as % of (rows+gaps) height

	private static final double ROW1_BTN_X_RATIO    = 0.025; // button starts at 2.5% of row width
	private static final double ROW1_BTN_WRATIO     = 0.80;  // button is 80% of row width
	private static final double ROW1_GAP_WRATIO     = 0.01;  // small gap between button and icon label

	private static final double CHECK_MAX_WRATIO    = 0.90;  // checkbox uses needed width up to 90% of row width
	private static final int    MIN_FONT_SIZE       = 6;
	private static final String CHECK_TEXT          = "Propagate in 3D/time";
	private static final String ELLIPSIS_TEXT       = "....";
    
	public CenterPanelBatch() {
		setLayout(null);
		
	    btnBatchSAMize = new JButton("Batch SAMize");
        Icon questionIcon = UIManager.getIcon("OptionPane.questionIcon");
        if (questionIcon == null) {
            questionIcon = UIManager.getIcon("OptionPane.informationIcon");
        }
        questionMark = new JLabel(questionIcon);
        questionMark.setToolTipText(Constants.HELP_MSG);

	    batchProgress = new JProgressBar();
	    stopProgressBtn = new JButton("■");
	    propagate3D = new JCheckBox("Propagate in 3D/time", false);
	    
	    add(btnBatchSAMize);
	    add(questionMark);
	    add(batchProgress);
	    add(stopProgressBtn);
	    add(propagate3D);
    }

	@Override
	public void doLayout() {
	    final int padX = 2;

	    final java.awt.Insets ins = getInsets();
	    int innerW = Math.max(0, getWidth()  - ins.left - ins.right - padX * 2);
	    int innerH = Math.max(0, getHeight() - ins.top  - ins.bottom);

	    // Group height (rows + gaps) is 80% of inner height, centered vertically
	    int groupH = Math.max(0, (int) Math.round(innerH * VCONTENT_HRATIO));
	    int groupY = ins.top + Math.max(0, (innerH - groupH) / 2);

	    // Row width is 95% of inner width, centered horizontally
	    int rowW = Math.max(1, (int) Math.round(innerW * ROW_WRATIO));
	    int rowX = ins.left + padX + Math.max(0, (innerW - rowW) / 2);

	    // Larger gaps between rows, but keep them sensible
	    int gapY = (int) Math.round(groupH * ROW_GAP_HRATIO);
	    gapY = Math.max(4, gapY); // "more space" feel
	    int rowsAreaH = Math.max(0, groupH - 2 * gapY);

	    // Height ratio: 2 : 1 : 1  (batch row twice as tall)
	    int unitH  = Math.max(1, rowsAreaH / 4);
	    int row1H  = Math.max(1, unitH * 2);
	    int row2H  = Math.max(1, unitH);
	    int row3H  = Math.max(1, rowsAreaH - row1H - row2H); // absorb rounding

	    int y1 = groupY;
	    int y2 = y1 + row1H + gapY;
	    int y3 = y2 + row2H + gapY;

	    // ---------- Row 1: [Batch button] + [question icon label on the right] ----------
	    int btnX = rowX + (int) Math.round(rowW * ROW1_BTN_X_RATIO);
	    int btnW = (int) Math.round(rowW * ROW1_BTN_WRATIO);
	    btnW = Math.max(1, Math.min(btnW, rowX + rowW - btnX));

	    int gapX = Math.max(1, (int) Math.round(rowW * ROW1_GAP_WRATIO));

	    // Icon label must be square, no insets, centered icon
	    questionMark.setBorder(null);
	    questionMark.setOpaque(false);
	    questionMark.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    questionMark.setVerticalAlignment(javax.swing.SwingConstants.CENTER);

	    // Place label at the far right; keep it square
	    int labelSizeMax = Math.min(row1H, rowW);
	    int labelX = rowX + rowW - labelSizeMax;
	    int labelY = y1 + Math.max(0, (row1H - labelSizeMax) / 2);

	    // Ensure button doesn't collide with label; shrink button if needed
	    int maxBtnW = Math.max(1, labelX - gapX - btnX);
	    btnW = Math.min(btnW, maxBtnW);

	    btnBatchSAMize.setBounds(btnX, y1, btnW, row1H);
	    questionMark.setBounds(labelX, labelY, labelSizeMax, labelSizeMax);

	    // ---------- Row 2: [progress bar] + [stop button immediately to its right] ----------
	    // Stop button must be square; keep it as large as the row height if possible.
	    stopProgressBtn.setMargin(new java.awt.Insets(1, 1, 1, 1)); // small, symmetric insets
	    stopProgressBtn.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    stopProgressBtn.setVerticalAlignment(javax.swing.SwingConstants.CENTER);

	    int stopSize = Math.min(row2H, rowW);
	    stopSize = Math.max(1, stopSize);

	    int progW = Math.max(1, rowW - stopSize);
	    batchProgress.setBounds(rowX, y2, progW, row2H);

	    // Square stop button; if row is taller than it is wide, center vertically
	    int stopX = rowX + progW;                // no gap between progress and stop
	    int stopY = y2 + Math.max(0, (row2H - stopSize) / 2);
	    stopProgressBtn.setBounds(stopX, stopY, stopSize, stopSize);

	    // ---------- Row 3: checkbox "as before" (centered, auto font, min 6, fallback "....") ----------
	    propagate3D.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

	    int cbMaxW = Math.max(1, (int) Math.round(rowW * CHECK_MAX_WRATIO));

	    java.awt.Font base = propagate3D.getFont();
	    int startSize = Math.max(MIN_FONT_SIZE, (int) Math.floor(row3H * 0.75));

	    boolean fitted = false;

	    // Try full text, shrink font down to MIN_FONT_SIZE until it fits width+height
	    for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	        java.awt.Font f = base.deriveFont((float) sz);
	        propagate3D.setFont(f);
	        propagate3D.setText(CHECK_TEXT);

	        java.awt.Dimension pref = propagate3D.getPreferredSize();
	        if (pref.width <= cbMaxW && pref.height <= row3H) {
	            fitted = true;
	            break;
	        }
	    }

	    // If still doesn't fit, use "...."
	    if (!fitted) {
	        for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	            java.awt.Font f = base.deriveFont((float) sz);
	            propagate3D.setFont(f);
	            propagate3D.setText(ELLIPSIS_TEXT);

	            java.awt.Dimension pref = propagate3D.getPreferredSize();
	            if (pref.width <= cbMaxW && pref.height <= row3H) {
	                fitted = true;
	                break;
	            }
	        }
	        if (!fitted) {
	            propagate3D.setFont(base.deriveFont((float) MIN_FONT_SIZE));
	            propagate3D.setText(ELLIPSIS_TEXT);
	        }
	    }

	    java.awt.Dimension cbPref = propagate3D.getPreferredSize();
	    int cbW = Math.max(1, Math.min(cbMaxW, cbPref.width));
	    int cbX = rowX + Math.max(0, (rowW - cbW) / 2);

	    propagate3D.setBounds(cbX, y3, cbW, row3H);
	}
}
