package ai.nets.samj.gui.last;


import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import ai.nets.samj.gui.JSwitchButton;

public class CenterPanel extends JPanel {


    private static final long serialVersionUID = -4364257664138829685L;
	protected JCheckBox propagate3D;
    protected JSwitchButton chkInstant;
    protected JRadioButton radioButton1;
    protected JRadioButton radioButton2;


    protected static String MANUAL_STR = "Manual";
    protected static String PRESET_STR = "Preset prompts";
    
	public CenterPanel() {
		setLayout(null);
		setBorder(BorderFactory.createEtchedBorder());
	    propagate3D = new JCheckBox("Propagate in 3D/time", false);
	    chkInstant = new JSwitchButton("LIVE", "OFF");
        radioButton1 = new JRadioButton(MANUAL_STR, true);
        radioButton2 = new JRadioButton(PRESET_STR);

        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(radioButton1);
        radioGroup.add(radioButton2);
    }
    
	@Override
	public void doLayout() {
	    final int gapY = 2;     // vertical space BETWEEN components
	    final int padX = 2;     // horizontal padding on BOTH sides

	}
}
