package ai.nets.samj.gui;

import java.util.List;
import java.util.stream.IntStream;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.UtilityMethods;

public class ImageSelection extends ComboBoxButtonComp<ComboBoxItem> implements PopupMenuListener {

	private final UtilityMethods consumerUtils;
	private final ImageSelectionListener listener;
	
	private ComboBoxItem selected;

	private static final long serialVersionUID = 2478618937640492286L;

	private ImageSelection(UtilityMethods consumerUtils, ImageSelectionListener listener) {
		super(new JComboBox<ComboBoxItem>());
		this.consumerUtils = consumerUtils;
		this.listener = listener;
		List<ComboBoxItem> listImages = this.consumerUtils.getListOfOpenImages();
		for(ComboBoxItem item : listImages)
			this.cmbBox.addItem(item);
	}
	
	protected ImageSelection create(UtilityMethods consumerUtils, ImageSelectionListener listener) {
		return new ImageSelection(consumerUtils, listener);
	}
	
	protected Object getSelectedObject() {
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getValue();
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		Object item = this.cmbBox.getSelectedItem();
        List<ComboBoxItem> openSeqs = consumerUtils.getListOfOpenImages();
        ComboBoxItem[] objects = new ComboBoxItem[openSeqs.size()];
        for (int i = 0; i < objects.length; i ++) objects[i] = openSeqs.get(i);
        DefaultComboBoxModel<ComboBoxItem> comboBoxModel = new DefaultComboBoxModel<ComboBoxItem>(objects);
        this.cmbBox.setModel(comboBoxModel);
        if (item != null && objects.length != 0) 
        	this.cmbBox.setSelectedIndex(
        			IntStream.range(0, objects.length).filter(i -> objects[i].getId() == ((ComboBoxItem) item).getId()).findFirst().orElse(0)
        			);
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		ComboBoxItem item = (ComboBoxItem) this.cmbBox.getSelectedItem();
		if (selected != item) {
			listener.modelActionsOnImageChanged();
			listener.imageActionsOnImageChanged();
		}
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		
	}
	
	public interface ImageSelectionListener {
		
	    void modelActionsOnImageChanged();
	    
	    void imageActionsOnImageChanged();
	}

}
