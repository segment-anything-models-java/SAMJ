package ai.nets.samj.gui;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import ai.nets.samj.communication.model.SAMModel;

public class ModelSelection extends ComboBoxButtonComp<String> implements PopupMenuListener {
	
	private SAMModel selected;
	
	private final List<SAMModel> models;


	private static final long serialVersionUID = 2478618937640492286L;

	private ModelSelection(List<SAMModel> models) {
		super(new JComboBox<String>());
		this.models = models;
		for (SAMModel model : models) {
			this.cmbBox.addItem(model.getName());
		}
		cmbBox.addPopupMenuListener(this);
		selected = models.get(cmbBox.getSelectedIndex());
	}
	
	protected static ModelSelection create(List<SAMModel> models) {
		return new ModelSelection(models);
	}
	
	protected JButton getButton() {
		return this.btn;
	}
	
	protected void loadModel() {
		
	}
	
	protected void unLooadModel() {
		
	}

	@Override
	/**
	 * Nothing
	 */
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	}

	@Override
	/**
	 * Check if the image selected has been changed once the combobox pop up is closed
	 */
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		SAMModel nSelectedModel = models.get(cmbBox.getSelectedIndex());
		if (nSelectedModel != selected) {
			unLooadModel();
			selected = nSelectedModel;
			loadModel();
		}
	}

	@Override
	/**
	 * Nothing
	 */
	public void popupMenuCanceled(PopupMenuEvent e) {
	}
}
