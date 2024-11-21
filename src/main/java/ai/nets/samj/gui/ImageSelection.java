package ai.nets.samj.gui;

import javax.swing.JComboBox;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import ai.nets.samj.gui.components.ComboBoxItem;

public class ImageSelection extends ComboBoxButtonComp<ComboBoxItem> {
	

	private static final long serialVersionUID = 2478618937640492286L;

	public ImageSelection() {
		super(new JComboBox<ComboBoxItem>());
		this.cmbBox.setModel(null);
		
	}

}
