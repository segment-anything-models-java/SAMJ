package ai.nets.samj.gui.last;

import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.gui.components.ComboBoxButtonComp;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.ConsumerInterface;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImageSelection extends ComboBoxButtonComp<ComboBoxItem> implements PopupMenuListener {

	private ConsumerInterface consumer;
	private ImageSelection.ImageSelectionListener listener;
	
	private ComboBoxItem selected;

	private static final long serialVersionUID = 2478618937640492286L;

	private ImageSelection(ConsumerInterface consumer, ImageSelection.ImageSelectionListener listener) {
		super(new JComboBox<ComboBoxItem>());
		this.consumer = consumer;
		this.listener = listener;
		List<ComboBoxItem> listImages = new ArrayList<>(); //this.consumer.getListOfOpenImages();
		for(ComboBoxItem item : listImages)
			this.cmbBox.addItem(item);
		cmbBox.addPopupMenuListener(this);
	}
	
	public void setConsumer(ConsumerInterface consumer) {
		this.consumer = consumer;
	}
	
	public void setListener(ImageSelection.ImageSelectionListener listener) {
		this.listener = listener;
	}

	protected JButton getButton() {
		return this.btn;
	}
	
	public static ImageSelection create(ConsumerInterface consumer, ImageSelection.ImageSelectionListener listener) {
		return new ImageSelection(consumer, listener);
	}
	
	public static ImageSelection create() {
		return new ImageSelection(null, null);
	}
	
	protected Object getSelectedObject() {
		if (this.cmbBox.getSelectedItem() == null)
			return null;
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getValue();
	}
	
	protected int getSelectedNFrames() {
		if (this.cmbBox.getSelectedItem() == null)
			return 0;
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getNFrames();
	}
	
	protected int getSelectedNSlices() {
		if (this.cmbBox.getSelectedItem() == null)
			return 0;
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getNSlices();
	}
	
	protected int getSelectedCurrentFrame() {
		if (this.cmbBox.getSelectedItem() == null)
			return 0;
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getCurrentFrame();
	}
	
	protected int getSelectedCurrentSlice() {
		if (this.cmbBox.getSelectedItem() == null)
			return 0;
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getCurrentSlice();
	}
	
	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getSelectedRai() {
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getImageAsImgLib2();
	}
	
	protected void updateList() {
		try {
			Object selectedVal = null;
			if (selected != null)
				selectedVal = selected.getValue();
	        List<ComboBoxItem> openSeqs = consumer.getListOfOpenImages();
	        ComboBoxItem[] objects = new ComboBoxItem[openSeqs.size()];
	        for (int i = 0; i < objects.length; i ++) objects[i] = openSeqs.get(i);
	        DefaultComboBoxModel<ComboBoxItem> comboBoxModel = new DefaultComboBoxModel<ComboBoxItem>(objects);
	        if (selectedVal == null && objects.length != 0)
	        	selected = objects[0];
	        else if (selectedVal == null)
	        	selected = null;
	        if (selected != null && objects.length != 0)
	        	comboBoxModel.setSelectedItem(selected);
	        if (objects.length == 0)
	        	selected = null;
	        this.cmbBox.setModel(comboBoxModel);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		updateList();
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		try {
			ComboBoxItem item = (ComboBoxItem) this.cmbBox.getSelectedItem();
			if ((selected == null && item != null) || (selected != null && item == null)
					|| (selected != null && item != null && !selected.getId().equals(item.getId()))) {
				listener.modelActionsOnImageChanged();
				listener.imageActionsOnImageChanged();
			} else if (selected == null && item == null) {
				selected = null;
				listener.modelActionsOnImageChanged();
				listener.imageActionsOnImageChanged();
			}
			selected = item;
		} catch (Exception ex) {
			ex.printStackTrace();
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
