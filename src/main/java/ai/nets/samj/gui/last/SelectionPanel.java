package ai.nets.samj.gui.last;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.ImageSelection.ImageSelectionListener;
import ai.nets.samj.gui.ImageSelectionCombo;
import ai.nets.samj.gui.LoadingButton;
import ai.nets.samj.gui.ModelSelection;
import ai.nets.samj.gui.ModelSelection.ModelSelectionListener;

public class SelectionPanel extends JPanel {

	protected ModelSelection cmbModels;
	
	protected ImageSelectionCombo cmbImages;

    protected LoadingButton go;
    
    private static final double BUTTON_HRATIO = 1.2;

    private static final long serialVersionUID = -1041152913379704899L;

	/**
	 * Name of the folder where the icon images for the dialog buttons are within the resources folder
	 */
	protected static final String RESOURCES_FOLDER = "icons_samj/";

	public SelectionPanel() {
		setLayout(null);
		this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        cmbImages = ImageSelectionCombo.create();
        cmbModels = ModelSelection.create();
        go = new LoadingButton("Go!", RESOURCES_FOLDER, "loading_animation_samj.gif", 20);
        add(cmbImages);
        add(cmbModels);
        add(go);
    }
	
	public void setModels(List<SAMModel> models) {
		cmbModels.setModels(models);
	}
	
	public void setModelListener(ModelSelectionListener listener) {
		cmbModels.setListener(listener);
	}
	
	public void setImageConsumer(ConsumerInterface consumer) {
		cmbImages.setConsumer(consumer);
	}
	
	public void setImageListener(ImageSelectionListener listener) {
		cmbImages.setListener(listener);
	}
	
	public LoadingButton getGoButton() {
		return this.go;
	}
	
	public ModelSelection getModelSelection() {
		return this.cmbModels;
	}
	
	public ImageSelectionCombo getImageSelection() {
		return this.cmbImages;
	}
    
	@Override
	public void doLayout() {
	    final int gapY = 2;     // vertical space BETWEEN components
	    final int padX = 2;     // horizontal padding on BOTH sides

	    final java.awt.Insets ins = getInsets();

	    // Inner available area (respect border insets)
	    int x = ins.left + padX;
	    int y = ins.top;

	    int w = Math.max(0, getWidth() - ins.left - ins.right - padX * 2);
	    int h = Math.max(0, getHeight() - ins.top - ins.bottom);

	    // Remove the two vertical gaps (3 rows => 2 gaps)
	    int contentH = Math.max(0, h - gapY * 2);

	    // Target ratio: base, base, 1.2*base  => total = 3.2*base
	    int baseH = (int) Math.floor(contentH / (BUTTON_HRATIO + 2));
	    baseH = Math.max(1, baseH);

	    int btnH = (int) Math.round(baseH * BUTTON_HRATIO);
	    btnH = Math.max(1, btnH);

	    // If rounding made us overflow, shrink button first, then base heights if needed
	    int used = baseH + baseH + btnH;
	    if (used > contentH) {
	        int over = used - contentH;

	        int shrinkBtn = Math.min(over, Math.max(0, btnH - 1));
	        btnH -= shrinkBtn;
	        over -= shrinkBtn;

	        if (over > 0) {
	            int shrinkBase = (over + 1) / 2;
	            baseH = Math.max(1, baseH - shrinkBase);
	            btnH = Math.max(1, contentH - 2 * baseH);
	        }
	    }

	    // Row 1: model selection
	    cmbModels.setBounds(x, y, w, baseH);
	    y += baseH + gapY;

	    // Row 2: image selection
	    cmbImages.setBounds(x, y, w, baseH);
	    y += baseH + gapY;

	    // Row 3: button (20% taller)
	    go.setBounds(x, y, w, btnH);
	}
}
