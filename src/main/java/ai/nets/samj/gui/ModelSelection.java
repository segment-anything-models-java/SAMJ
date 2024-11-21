package ai.nets.samj.gui;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import io.bioimage.modelrunner.model.Model;

public class ModelSelection extends ComboBoxButtonComp<String> {


	private static final long serialVersionUID = 2478618937640492286L;

	public ModelSelection(List<Model> models) {
		super(new JComboBox<String>());
		this.cmbBox.setli(null);
		
	}
	
	

}
