/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 - 2026 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.gui;

import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.gui.components.ComboBoxComp;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.ui.ConsumerInterface;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImageSelectionOnlyComboBox extends ComboBoxComp<ComboBoxItem> implements PopupMenuListener {

	private final ConsumerInterface consumer;
	private final ImageSelection.ImageSelectionListener listener;
	
	private ComboBoxItem selected;

	private static final long serialVersionUID = 2478618937640492286L;

	private ImageSelectionOnlyComboBox(ConsumerInterface consumer, ImageSelection.ImageSelectionListener listener) {
		super(new JComboBox<ComboBoxItem>());
		this.consumer = consumer;
		this.listener = listener;
		List<ComboBoxItem> listImages = this.consumer.getListOfOpenImages();
		for(ComboBoxItem item : listImages)
			this.cmbBox.addItem(item);
		cmbBox.addPopupMenuListener(this);
	}
	
	protected static ImageSelectionOnlyComboBox create(ConsumerInterface consumer, ImageSelection.ImageSelectionListener listener) {
		return new ImageSelectionOnlyComboBox(consumer, listener);
	}
	
	protected Object getSelectedObject() {
		if (this.cmbBox.getSelectedItem() == null)
			return null;
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getValue();
	}
	
	protected <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> getSelectedRai() {
		return ((ComboBoxItem) this.cmbBox.getSelectedItem()).getImageAsImgLib2();
	}
	
	protected void updateList() {
		try {
	        List<ComboBoxItem> openSeqs = consumer.getListOfOpenImages();
	        ComboBoxItem[] objects = new ComboBoxItem[openSeqs.size()];
	        for (int i = 0; i < objects.length; i ++) objects[i] = openSeqs.get(i);
	        DefaultComboBoxModel<ComboBoxItem> comboBoxModel = new DefaultComboBoxModel<ComboBoxItem>(objects);
	        if (selected != null && objects.length != 0)
	        	comboBoxModel.setSelectedItem(selected);
	        if (objects.length == 0)
	        	selected = null;
	        this.cmbBox.setModel(comboBoxModel);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Refreshes the list of open images before the popup becomes visible.
	 *
	 * @param e popup event
	 */
	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		updateList();
	}

	/**
	 * Updates the selected image and notifies listeners when it changes.
	 *
	 * @param e popup event
	 */
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

	/**
	 * Handles popup-cancel events.
	 *
	 * @param e popup event
	 */
	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		
	}

}
