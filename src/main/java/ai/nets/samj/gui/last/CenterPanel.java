package ai.nets.samj.gui.last;


import java.awt.CardLayout;
import java.awt.Color;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.LineBorder;

public class CenterPanel extends JPanel {


    private static final long serialVersionUID = -4364257664138829685L;
    protected JPanel cardPanel;
    protected JRadioButton radioButton1;
    protected JRadioButton radioButton2;
    protected CenterPanelSwitch instantCard;
    protected CenterPanelBatch batchCard;


    protected static String MANUAL_STR = "Manual";
    protected static String PRESET_STR = "Preset prompts";

    private static final int    PAD_X              = 2;      // horizontal inset from border
    private static final int    PAD_Y              = 2;      // vertical inset from border (top/bottom)
    private static final int    GAP_Y              = 2;      // gap between row 1 and row 2
    private static final double RADIO_TEXT_WRATIO  = 0.95;   // radio uses up to 95% of its column width
    private static final int    MIN_FONT_SIZE      = 6;
    private static final String ELLIPSIS_TEXT      = "...";
    
	public CenterPanel() {
		setLayout(null);
        setBorder(new LineBorder(Color.BLACK));
        
        radioButton1 = new JRadioButton(MANUAL_STR, true);
        radioButton2 = new JRadioButton(PRESET_STR);

        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(radioButton1);
        radioGroup.add(radioButton2);
        
        cardPanel = new JPanel(new CardLayout());
        instantCard = new CenterPanelSwitch();
        batchCard = new CenterPanelBatch();
        cardPanel.add(instantCard, MANUAL_STR);
        cardPanel.add(batchCard, PRESET_STR);

        
        add(radioButton1);
        add(radioButton2);
        add(cardPanel);
    }

	@Override
	public void doLayout() {
	    final java.awt.Insets ins = getInsets();

	    int innerW = Math.max(0, getWidth()  - ins.left - ins.right  - PAD_X * 2);
	    int innerH = Math.max(0, getHeight() - ins.top  - ins.bottom - PAD_Y * 2);

	    int x0 = ins.left + PAD_X;
	    int y0 = ins.top  + PAD_Y;

	    // Two rows with a 2px gap between them
	    int contentH = Math.max(0, innerH - GAP_Y);

	    // Give the radio row a reasonable height based on total height (and keep >= 1)
	    int row1H = Math.max(1, (int) Math.round(contentH * 0.18));
	    int row2H = Math.max(1, contentH - row1H);

	    // If radio row is too small to render nicely, steal a bit from the card panel
	    row1H = Math.max(16, row1H);
	    if (row1H + 1 > contentH) row1H = Math.max(1, contentH - 1);
	    row2H = Math.max(1, contentH - row1H);

	    int y1 = y0;
	    int y2 = y1 + row1H + GAP_Y;

	    // ----- Row 1: two equal columns for radio buttons -----
	    int colW = Math.max(1, innerW / 2);
	    int col1X = x0;
	    int col2X = x0 + colW;
	    int col2W = Math.max(1, innerW - colW);

	    // Center the radio buttons inside their columns and cap them at 95% width
	    radioButton1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    radioButton2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

	    int r1MaxW = Math.max(1, (int) Math.round(colW  * RADIO_TEXT_WRATIO));
	    int r2MaxW = Math.max(1, (int) Math.round(col2W * RADIO_TEXT_WRATIO));

	    // Helper logic inlined: shrink font to fit; if still not fit -> "..."
	    java.awt.Font base1 = radioButton1.getFont();
	    java.awt.Font base2 = radioButton2.getFont();

	    int startSize = Math.max(MIN_FONT_SIZE, (int) Math.floor(row1H * 0.65));

	    // --- radioButton1 fit ---
	    boolean fit1 = false;
	    String full1 = MANUAL_STR;
	    for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	        radioButton1.setFont(base1.deriveFont((float) sz));
	        radioButton1.setText(full1);
	        java.awt.Dimension pref = radioButton1.getPreferredSize();
	        if (pref.width <= r1MaxW && pref.height <= row1H) { fit1 = true; break; }
	    }
	    if (!fit1) {
	        for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	            radioButton1.setFont(base1.deriveFont((float) sz));
	            radioButton1.setText(ELLIPSIS_TEXT);
	            java.awt.Dimension pref = radioButton1.getPreferredSize();
	            if (pref.width <= r1MaxW && pref.height <= row1H) { fit1 = true; break; }
	        }
	        if (!fit1) { radioButton1.setFont(base1.deriveFont((float) MIN_FONT_SIZE)); radioButton1.setText(ELLIPSIS_TEXT); }
	    }

	    // --- radioButton2 fit ---
	    boolean fit2 = false;
	    String full2 = PRESET_STR;
	    for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	        radioButton2.setFont(base2.deriveFont((float) sz));
	        radioButton2.setText(full2);
	        java.awt.Dimension pref = radioButton2.getPreferredSize();
	        if (pref.width <= r2MaxW && pref.height <= row1H) { fit2 = true; break; }
	    }
	    if (!fit2) {
	        for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	            radioButton2.setFont(base2.deriveFont((float) sz));
	            radioButton2.setText(ELLIPSIS_TEXT);
	            java.awt.Dimension pref = radioButton2.getPreferredSize();
	            if (pref.width <= r2MaxW && pref.height <= row1H) { fit2 = true; break; }
	        }
	        if (!fit2) { radioButton2.setFont(base2.deriveFont((float) MIN_FONT_SIZE)); radioButton2.setText(ELLIPSIS_TEXT); }
	    }

	    // Set bounds (centered inside each column, max 95% width)
	    int r1W = Math.max(1, Math.min(r1MaxW, radioButton1.getPreferredSize().width));
	    int r2W = Math.max(1, Math.min(r2MaxW, radioButton2.getPreferredSize().width));

	    int r1X = col1X + Math.max(0, (colW  - r1W) / 2);
	    int r2X = col2X + Math.max(0, (col2W - r2W) / 2);

	    radioButton1.setBounds(r1X, y1, r1W, row1H);
	    radioButton2.setBounds(r2X, y1, r2W, row1H);

	    // ----- Row 2: card panel takes all remaining space -----
	    cardPanel.setBounds(x0, y2, Math.max(1, innerW), Math.max(1, row2H));
	}
}
