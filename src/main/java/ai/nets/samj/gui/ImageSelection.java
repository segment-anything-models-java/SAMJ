package ai.nets.samj.gui;

import java.util.List;
import java.util.stream.IntStream;

import javax.swing.DefaultComboBoxModel;
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

	private final ConsumerInterface consumer;
	private final ImageSelectionListener listener;
	
	private ComboBoxItem selected;

	private static final long serialVersionUID = 2478618937640492286L;

	private ImageSelection(ConsumerInterface consumer, ImageSelectionListener listener) {
		super(new JComboBox<ComboBoxItem>());
		this.consumer = consumer;
		this.listener = listener;
		List<ComboBoxItem> listImages = this.consumer.getListOfOpenImages();
		for(ComboBoxItem item : listImages)
			this.cmbBox.addItem(item);
		cmbBox.addPopupMenuListener(this);
	}
	
	protected static ImageSelection create(ConsumerInterface consumer, ImageSelectionListener listener) {
		return new ImageSelection(consumer, listener);
	}
	
	protected Object getSelectedObject() {
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getValue();
	}
	
	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getSelectedRai() {
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getImageAsImgLib2();
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		try {
			Object item = this.cmbBox.getSelectedItem();
	        List<ComboBoxItem> openSeqs = consumer.getListOfOpenImages();
	        ComboBoxItem[] objects = new ComboBoxItem[openSeqs.size()];
	        for (int i = 0; i < objects.length; i ++) objects[i] = openSeqs.get(i);
	        DefaultComboBoxModel<ComboBoxItem> comboBoxModel = new DefaultComboBoxModel<ComboBoxItem>(objects);
	        this.cmbBox.setModel(comboBoxModel);
	        if (item != null && objects.length != 0) 
	        	this.cmbBox.setSelectedIndex(
	        			IntStream.range(0, objects.length).filter(i -> objects[i].getId() == ((ComboBoxItem) item).getId()).findFirst().orElse(0)
	        			);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		try {
			ComboBoxItem item = (ComboBoxItem) this.cmbBox.getSelectedItem();
			if (selected != item) {
				listener.modelActionsOnImageChanged();
				listener.imageActionsOnImageChanged();
			}
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
