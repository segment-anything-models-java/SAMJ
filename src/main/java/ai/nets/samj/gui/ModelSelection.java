package ai.nets.samj.gui;

import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import ai.nets.samj.annotation.Mask;
import ai.nets.samj.communication.model.SAMModel;

public class ModelSelection extends ComboBoxButtonComp<String> implements PopupMenuListener {
	
	private SAMModel selected;
	
	private final ModelSelectionListener listener;
	
	private final List<SAMModel> models;


	private static final long serialVersionUID = 2478618937640492286L;

	private ModelSelection(List<SAMModel> models, ModelSelectionListener listener) {
		super(new JComboBox<String>());
		this.listener = listener;
		this.models = models;
		for (SAMModel model : models) {
			this.cmbBox.addItem(model.getName());
		}
		cmbBox.addPopupMenuListener(this);
		selected = models.get(cmbBox.getSelectedIndex());
	}
	
	protected static ModelSelection create(List<SAMModel> models, ModelSelectionListener listener) {
		return new ModelSelection(models, listener);
	}
	
	protected SAMModel getSelectedModel() {
		return selected;
	}
	
	protected JButton getButton() {
		return this.btn;
	}
	
	protected <T extends RealType<T> & NativeType<T>> void loadModel(RandomAccessibleInterval<T> rai) throws IOException, RuntimeException, InterruptedException {
		selected.setImage(rai, null);
	}
	
	protected void unLoadModel() {
		selected.closeProcess();
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
		try {
			SAMModel nSelectedModel = models.get(cmbBox.getSelectedIndex());
			if (nSelectedModel != selected) {
				unLoadModel();
				listener.changeGUI();
				selected = nSelectedModel;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		listener.changeDrawerPanel();
	}

	@Override
	/**
	 * Nothing
	 */
	public void popupMenuCanceled(PopupMenuEvent e) {
	}
	
	public interface ModelSelectionListener {

	    void changeDrawerPanel();
	    
	    void changeGUI();
	}
}
