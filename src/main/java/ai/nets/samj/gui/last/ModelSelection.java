package ai.nets.samj.gui.last;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import ai.nets.samj.communication.model.DummyModel;
import ai.nets.samj.communication.model.SAMModel;

public class ModelSelection extends ComboBoxButtonComp<SAMModel> implements ItemListener {
	
	private SAMModel selected;

	private SAMModel displayed;
	
	private ModelSelectionListener listener;
	
	private List<SAMModel> models;


	private static final long serialVersionUID = 2478618937640492286L;

	private ModelSelection(List<SAMModel> models, ModelSelectionListener listener) {
		super(new JComboBox<SAMModel>());
		this.listener = listener;
		this.models = models;
		if (models == null)
			models = new ArrayList<>(Collections.singletonList(new DummyModel()));
		for (SAMModel model : models) {
			this.cmbBox.addItem(model);
		}
		cmbBox.addItemListener(this);
		selected = models.get(cmbBox.getSelectedIndex());
		
		installHoverListener();
	}
	
	public void setModels(List<SAMModel> models) {
		this.models = models;
		this.cmbBox.removeAllItems();
		for (SAMModel model : models) {
			this.cmbBox.addItem(model);
		}
	}
	
	public void setListener(ModelSelectionListener listener) {
		this.listener = listener;
	}
	
	public static ModelSelection create(List<SAMModel> models, ModelSelectionListener listener) {
		return new ModelSelection(models, listener);
	}
	
	public static ModelSelection create() {
		return new ModelSelection(null, null);
	}
	
	protected SAMModel getSelectedModel() {
		return selected;
	}
	
	protected SAMModel getDisplayedModel() {
		return displayed;
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
	
	protected <T extends RealType<T> & NativeType<T>> void loadModel(RandomAccessibleInterval<T> rai, int slice, int frame,
			int nSlices, int nFrames, boolean propagate) throws InterruptedException, BuildException, TaskException, IOException {
		if (!selected.isLoaded())
			selected.loadModel(null);
		selected.setImage(rai, slice, frame, nSlices, nFrames, propagate);
	}
	
	protected void unLoadModel() {
		selected.closeProcess();
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			SAMModel nSelectedModel = models.get(cmbBox.getSelectedIndex());
			try {
				// checks if indeed a different model is selected (from what was selected before)
				if (nSelectedModel != selected) {
					unLoadModel();
					selected = nSelectedModel;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			if (nSelectedModel != displayed) {
				listener.changeDrawerPanel(nSelectedModel);
				displayed = nSelectedModel;
			}
		}
	}


	public interface ModelSelectionListener {

	    void changeDrawerPanel(SAMModel selected);
	}
	
	private void installHoverListener() {
	    SwingUtilities.invokeLater(() -> {
	        try {
	            BasicComboPopup popup = (BasicComboPopup)
	                    ((BasicComboBoxUI) this.cmbBox.getUI()).getAccessibleChild(this.cmbBox, 0);

	            @SuppressWarnings("rawtypes")
	            JList list = popup.getList();

	            list.addMouseMotionListener(new MouseMotionAdapter() {
	                @Override
	                public void mouseMoved(MouseEvent e) {
	                    int index = list.locationToIndex(e.getPoint());
	                    if (index >= 0) {
	                        // This is the item currently under the mouse
	                        SAMModel hovered = (SAMModel) list.getModel().getElementAt(index);

	            			if (hovered != displayed) {
	            				listener.changeDrawerPanel(hovered);
	            				displayed = hovered;
	            			}
	                    }
	                }
	            });
	        } catch (ClassCastException ex) {
	            // Not Basic LAF / custom UI delegate; hover hook not available this way
	            ex.printStackTrace();
	        }
	    });
	}
}
