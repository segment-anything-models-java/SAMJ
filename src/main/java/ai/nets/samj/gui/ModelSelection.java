package ai.nets.samj.gui;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import ai.nets.samj.communication.model.SAMModel;

public class ModelSelection extends ComboBoxButtonComp<String> implements ItemListener {
	
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
		cmbBox.addItemListener(this);
		selected = models.get(cmbBox.getSelectedIndex());
	}
	
	protected static ModelSelection create(List<SAMModel> models, ModelSelectionListener listener) {
		return new ModelSelection(models, listener);
	}
	
	protected SAMModel getSelectedModel() {
		return selected;
	}

	protected boolean isModelInstalled(String modelName) {
		for (SAMModel m : this.models) {
			if (m.getName().equals(modelName)) return m.isInstalled();
		}
		return false;
	}

	protected SAMModel getModelByName(String modelName) {
		for (SAMModel m : this.models) {
			if (m.getName().equals(modelName)) return m;
		}
		return null;
	}

	protected JButton getButton() {
		return this.btn;
	}
	
	protected <T extends RealType<T> & NativeType<T>> void loadModel(RandomAccessibleInterval<T> rai) throws IOException, RuntimeException, InterruptedException {
		selected.setImage(rai);
	}
	
	protected void unLoadModel() {
		selected.closeProcess();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			try {
				SAMModel nSelectedModel = models.get(cmbBox.getSelectedIndex());
				// checks if indeed a different model is selected (from what was selected before)
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
	}


	public interface ModelSelectionListener {

	    void changeDrawerPanel();
	    
	    void changeGUI();
	}
}
