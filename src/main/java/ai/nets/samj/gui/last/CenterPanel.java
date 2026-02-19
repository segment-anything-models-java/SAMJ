package ai.nets.samj.gui.last;


import java.awt.CardLayout;
import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class CenterPanel extends JPanel {


    private static final long serialVersionUID = -4364257664138829685L;
    protected JPanel cardPanel;
    protected JRadioButton radioButton1;
    protected JRadioButton radioButton2;
    protected CenterPanelSwitch instantCard;
    protected CenterPanelBatch batchCard;
	protected Font radioBaseFont;


    protected static String MANUAL_STR = "Manual";
    protected static String PRESET_STR = "Preset prompts";

    private static final double MANUAL_WRATIO = 0.35;
    private static final double RADIO_HRATIO = 0.18;
    private static final int    PAD_X              = 2;      // horizontal inset from border
    private static final int    PAD_Y              = 2;      // vertical inset from border (top/bottom)
    private static final double RADIO_TEXT_WRATIO  = 0.98;   // radio uses up to 95% of its column width
    private static final int    MIN_FONT_SIZE      = 8;
    private static final String ELLIPSIS_TEXT      = "...";
    
	public CenterPanel() {
		setLayout(null);
        //setBorder(new LineBorder(Color.BLACK));
        
        radioButton1 = new JRadioButton(MANUAL_STR, true);
        radioButton2 = new JRadioButton(PRESET_STR);
        radioBaseFont = radioButton1.getFont();
        radioButton1.setFont(radioBaseFont.deriveFont(java.awt.Font.PLAIN, radioBaseFont.getSize2D()));
        radioButton2.setFont(radioBaseFont.deriveFont(java.awt.Font.PLAIN, radioBaseFont.getSize2D()));
        radioBaseFont = radioButton1.getFont();

        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(radioButton1);
        radioGroup.add(radioButton2);
        
        cardPanel = new JPanel(new CardLayout());
        //cardPanel.setBorder(LineBorder.createBlackLineBorder());
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

	    int innerW = Math.max(0, getWidth() - PAD_X * 2);
	    int innerH = Math.max(0, getHeight() - PAD_Y * 2);

	    int x0 = PAD_X;
	    int y0 = PAD_Y;

	    // Two rows with a gap between them
	    int contentH = Math.max(0, innerH - PAD_Y);

	    // Row split
	    int row1H = Math.max(1, (int) Math.round(contentH * RADIO_HRATIO));
	    row1H = Math.max(16, row1H);
	    if (row1H + 1 > contentH) row1H = Math.max(1, contentH - 1);
	    int row2H = Math.max(1, contentH - row1H);

	    int y1 = y0;
	    int y2 = y1 + row1H + PAD_Y;

	    // ----- Row 1: two equal columns for radio buttons -----
	    int colW  = (int) Math.max(1, innerW * MANUAL_WRATIO);
	    int col1X = x0;
	    int col2X = x0 + colW;
	    int col2W = Math.max(1, innerW - colW);

	    radioButton1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
	    radioButton2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

	    int r1MaxW = Math.max(1, (int) Math.round(colW  * RADIO_TEXT_WRATIO));
	    int r2MaxW = Math.max(1, (int) Math.round(col2W * RADIO_TEXT_WRATIO));

	    // Use ONE font size for BOTH radios (prevents different fonts)
	    int startSize = Math.max(MIN_FONT_SIZE, (int) Math.floor(row1H * 0.75));

	    int chosenSize = MIN_FONT_SIZE;
	    boolean fullFits = false;

	    // Find largest font size that fits BOTH full texts
	    for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	        java.awt.Font f = radioBaseFont.deriveFont((float) sz);

	        radioButton1.setFont(f);
	        radioButton2.setFont(f);
	        radioButton1.setText(MANUAL_STR);
	        radioButton2.setText(PRESET_STR);

	        java.awt.Dimension p1 = radioButton1.getPreferredSize();
	        java.awt.Dimension p2 = radioButton2.getPreferredSize();

	        if (p1.width <= r1MaxW && p1.height <= row1H &&
	            p2.width <= r2MaxW && p2.height <= row1H) {
	            chosenSize = sz;
	            fullFits = true;
	            break;
	        }
	        if (p1.height <= row1H && p2.height <= row1H
	        		&& p2.getWidth() > r2MaxW) {
	        		while (p2.getWidth() > r2MaxW && radioButton2.getText().length() > ELLIPSIS_TEXT.length()) {
	        			String newString = radioButton2.getText();
	        			newString = newString.substring(0, newString.length() - 1 - ELLIPSIS_TEXT.length()) + ELLIPSIS_TEXT;
	        	        radioButton2.setText(newString);
	        	        p2 = radioButton2.getPreferredSize();
	        		}
		            chosenSize = sz;
		            fullFits = true;
		            break;
            }
	        if (p1.height <= row1H && p2.height <= row1H
	        		&& p1.getWidth() > r1MaxW) {
	        		while (p1.getWidth() > r1MaxW && radioButton1.getText().length() > ELLIPSIS_TEXT.length()) {
	        			String newString = radioButton1.getText();
	        			newString = newString.substring(0, newString.length() - 1 - ELLIPSIS_TEXT.length()) + ELLIPSIS_TEXT;
	        	        radioButton1.setText(newString);
	        	        p1 = radioButton1.getPreferredSize();
	        		}
		            chosenSize = sz;
		            fullFits = true;
		            break;
            }
	    }

	    // If full text doesn't fit, use ellipsis for BOTH (still same font size)
	    if (!fullFits) {
	        for (int sz = startSize; sz >= MIN_FONT_SIZE; sz--) {
	            java.awt.Font f = radioBaseFont.deriveFont((float) sz);

	            radioButton1.setFont(f);
	            radioButton2.setFont(f);
	            radioButton1.setText(ELLIPSIS_TEXT);
	            radioButton2.setText(ELLIPSIS_TEXT);

	            java.awt.Dimension p1 = radioButton1.getPreferredSize();
	            java.awt.Dimension p2 = radioButton2.getPreferredSize();

	            if (p1.width <= r1MaxW && p1.height <= row1H &&
	                p2.width <= r2MaxW && p2.height <= row1H) {
	                chosenSize = sz;
	                break;
	            }
	        }
	    }

	    // Ensure final font is applied (in case loops never ran)
	    java.awt.Font finalFont = radioBaseFont.deriveFont((float) chosenSize);
	    radioButton1.setFont(finalFont);
	    radioButton2.setFont(finalFont);

	    // Bounds (centered inside each column, capped at 95% width)
	    int r1W = Math.max(1, Math.min(r1MaxW, radioButton1.getPreferredSize().width));
	    int r2W = Math.max(1, Math.min(r2MaxW, radioButton2.getPreferredSize().width));

	    int r1X = col1X + Math.max(0, (colW  - r1W) / 2);
	    int r2X = col2X + Math.max(0, (col2W - r2W) / 2);

	    radioButton1.setBounds(r1X, y1, r1W, row1H);
	    radioButton2.setBounds(r2X, y1, r2W, row1H);

	    // ----- Row 2: card panel takes remaining space -----
	    cardPanel.setBounds(x0, y2, Math.max(1, innerW), Math.max(1, row2H));
	}
}
