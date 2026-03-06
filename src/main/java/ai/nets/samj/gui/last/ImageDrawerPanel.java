package ai.nets.samj.gui.last;


import java.awt.Color;
import java.awt.Polygon;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.gui.roimanager.RoiManager;
import ai.nets.samj.gui.roimanager.RoiManagerConsumer;

public class ImageDrawerPanel extends JPanel {

    private static final long serialVersionUID = -8070874774405110221L;
	private JLabel drawerTitle = new JLabel();
	private RoiManager roiManager;
        
    private static final String MODEL_TITLE = ""
    		+ "<html>"
    		+ "<span style=\"color: blue;\">ROI Manager</span>"
    		+ "</html>";;
    
    private static double TITLE_RATIO = 0.15;
	
	
	private ImageDrawerPanel(RoiManagerConsumer consumer) {
		setFocusable(true);
		setBorder(BorderFactory.createLineBorder(Color.black));
		
		drawerTitle = new JLabel(MODEL_TITLE);
		drawerTitle.setHorizontalAlignment(JLabel.CENTER);
		drawerTitle.setVerticalAlignment(JLabel.CENTER);
		
		roiManager = new RoiManager(consumer);
		roiManager.block(true);
        
		add(drawerTitle);
		add(roiManager);
    }
	
	@Override
	public void doLayout() {
	    int rawW = getWidth();
	    int rawH = getHeight();
	    int inset = 2;

	    int titleHeight = (int) ((rawH - 3 * inset) * TITLE_RATIO);

	    drawerTitle.setBounds(inset, inset, rawW - 2 * inset, titleHeight);
	    roiManager.setBounds(inset, titleHeight + inset * 2,
	            rawW - 2 * inset,
	            (int) ((rawH - 3 * inset) * (1 - TITLE_RATIO)));

	    int fontSize = (int) (titleHeight * 0.6); // 60% of title height usually looks good
	    fontSize = Math.max(fontSize, 10);        // avoid too small fonts

	    drawerTitle.setFont(drawerTitle.getFont().deriveFont((float) fontSize));
	}
	
	public static ImageDrawerPanel create() {
	    RoiManagerConsumer dummy = new RoiManagerConsumer() {
	        @Override public void setSelected(Mask mm) {}
	        @Override public void exportMask() {}
	        @Override public void setRois(List<Mask> rois) {}
	        @Override public void setRois(List<Mask> rois, int index) {}
	        @Override public void deleteAllRois() {}
	        @Override public void setModifyRoiCallback(BiConsumer<Integer, Polygon> modifyRoiCallback) {}
	        @Override public void setImage(Object image) {}
	        @Override public void setSelectedCallback(Consumer<Integer> selectedCallback) {}
	    };

	    return new ImageDrawerPanel(dummy);
	}
	
	public static ImageDrawerPanel create(RoiManagerConsumer consumer) {
		return new ImageDrawerPanel(consumer);
	}
    
    @Override
    public void setVisible(boolean visible) {
    	super.setVisible(visible);
    }

}
