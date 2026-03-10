package ai.nets.samj.gui.last;

import java.awt.CardLayout;

public class Center extends CenterPanel {
	
	protected boolean isInstant = true;

	private static final long serialVersionUID = 7936657510534471002L;

	protected Center() {
        radioButton1.addActionListener(e -> {
            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, MANUAL_STR);
            isInstant = true;
        	//this.chkInstant.setEnabled(this.isValidPrompt);
        });

        radioButton2.addActionListener(e -> {
        	CardLayout cl = (CardLayout) (cardPanel.getLayout());
        	cl.show(cardPanel, PRESET_STR);
        	isInstant = false;
        	//this.chkInstant.setSelected(false);
        	//setInstantPromptsEnabled(false);
        });
	}
	
	public boolean isInstantShowing() {
		return this.isInstant;
	}
	
	public boolean isPromptValid() {
		return true;
	}
	
	public void setValidPrompt(boolean isValid) {
		
	}
}
