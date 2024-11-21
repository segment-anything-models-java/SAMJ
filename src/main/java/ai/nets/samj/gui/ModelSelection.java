package ai.nets.samj.gui;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import io.bioimage.modelrunner.model.Model;

public class ModelSelection extends ComboBoxButtonComp<String> implements PopupMenuListener {
	
	private Model selected;
	
	private final List<Model> models;
    private final ModelSelectionListener listener;


	private static final long serialVersionUID = 2478618937640492286L;

	private ModelSelection(List<Model> models, ModelSelectionListener listener) {
		super(new JComboBox<String>());
		this.listener = listener;
		this.models = models;
		for (Model model : models) {
			this.cmbBox.addItem(model.getModelName());
		}
		cmbBox.addPopupMenuListener(this);
		selected = models.get(cmbBox.getSelectedIndex());
	}
	
	protected ModelSelection create(List<Model> models, ModelSelectionListener listener) {
		return new ModelSelection(models, listener);
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
		Model nSelectedModel = models.get(cmbBox.getSelectedIndex());
		if (nSelectedModel != selected) {
			listener.modelChanged(nSelectedModel);
			selected = nSelectedModel;
		}
	}

	@Override
	/**
	 * Nothing
	 */
	public void popupMenuCanceled(PopupMenuEvent e) {
	}
	
	public interface ModelSelectionListener {
	    void modelChanged(Model model);
	}
}
