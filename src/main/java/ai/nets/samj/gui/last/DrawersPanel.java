package ai.nets.samj.gui.last;

import java.awt.CardLayout;

import javax.swing.JPanel;

import ai.nets.samj.gui.components.ImageDrawerPanel;
import ai.nets.samj.gui.components.ModelDrawerPanel;

public class DrawersPanel extends JPanel {
	
    private static final long serialVersionUID = 4451334593534890679L;
    
	protected ModelDrawerPanel modelDrawerPanel;
	
    protected ImageDrawerPanel imageDrawerPanel;
    
    protected JPanel cardPanel;
    
    protected JPanel cardPanel1_2;
    
    protected JPanel cardPanel2_2;
    
    protected static final String MODEL_TAG = "MODEL_DRAWER";
    
    protected static final String IMAGE_TAG = "ROI_DRAWER";

	/**
	 * Name of the folder where the icon images for the dialog buttons are within the resources folder
	 */
	protected static final String RESOURCES_FOLDER = "icons_samj/";

	public DrawersPanel() {
		setLayout(new CardLayout());
        modelDrawerPanel = ModelDrawerPanel.create();
        imageDrawerPanel = ImageDrawerPanel.create();
        add(modelDrawerPanel, MODEL_TAG);
        add(imageDrawerPanel, IMAGE_TAG);
        ((CardLayout) this.getLayout()).show(this, MODEL_TAG);
        
    }
}
