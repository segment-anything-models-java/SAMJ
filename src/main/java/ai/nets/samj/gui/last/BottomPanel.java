package ai.nets.samj.gui.last;


import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import io.bioimage.modelrunner.system.PlatformDetection;

public class BottomPanel extends JPanel {
    
	private static final long serialVersionUID = -6798700877370764562L;

    protected JCheckBox returnLargest = new JCheckBox("Only return largest ROI", true);

    protected JCheckBox acceleratorEnabled;

    protected JButton export = new JButton("Export...");

	private static final int    PAD_X = 2;          // horizontal inset from border
	private static final int    PAD_Y = 2;          // vertical inset from border
	private static final int    GAP_Y = 2;          // gap between rows
	private static final double BTN_H_RATIO = 1.2;  // button row is 20% taller

	private static final String CUDA_ACCELERATOR = "CUDA";
	private static final String MPS_ACCELERATOR = "MPS acceleration";
    
	public BottomPanel() {
		setLayout(null);
        setBorder(new LineBorder(Color.BLACK));
        
        if (PlatformDetection.getArch().equals(PlatformDetection.ARCH_ARM64))
        	acceleratorEnabled = new JCheckBox(MPS_ACCELERATOR, false);
        else
        	acceleratorEnabled = new JCheckBox(CUDA_ACCELERATOR, false);
        acceleratorEnabled.setEnabled(false);
        
        add(returnLargest);
        add(acceleratorEnabled);
        add(export);
    }

	@Override
	public void doLayout() {
	    final java.awt.Insets ins = getInsets();

	    int innerW = Math.max(0, getWidth()  - ins.left - ins.right  - PAD_X * 2);
	    int innerH = Math.max(0, getHeight() - ins.top  - ins.bottom - PAD_Y * 2);

	    int x = ins.left + PAD_X;
	    int y = ins.top  + PAD_Y;

	    // two rows => one gap
	    int contentH = Math.max(0, innerH - GAP_Y * 3);

	    // heights: base and 1.2*base => total 2.2*base
	    int baseH = Math.max(1, (int) Math.floor(contentH / (2.0 + BTN_H_RATIO)));
	    int btnH  = Math.max(1, contentH - 2 * baseH); // absorb rounding, ~20% taller

	    // row 1: checkbox
	    returnLargest.setBounds(x, y, Math.max(1, innerW), baseH);

	    // row 2: export button (20% taller), same width
	    y += baseH + GAP_Y;
	    acceleratorEnabled.setBounds(x, y, Math.max(1, innerW), baseH);
	    y += baseH + GAP_Y;
	    export.setBounds(x, y, Math.max(1, innerW), btnH);
	}
}
