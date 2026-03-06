package ai.nets.samj.gui.last;

import java.awt.CardLayout;
import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainGUI extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int MIN_DRAWER_SIZE = 450;
    
    protected boolean isModelDrawer = true;
    protected boolean isDrawerOpen = false;
    
    private boolean uiReady = false;
    private boolean pendingModelGuiRefresh = false;

    // keep compatibility with "Main extends MainGUI"
    protected javax.swing.JButton close;
    protected javax.swing.JButton help;
    protected TitleGUI titleGui;
    protected SelectionPanel selectionPanel;
    protected Center centerPanel;
    protected BottomPanel bottomPanel;

    protected final NoDrawerMainGUI content = new NoDrawerMainGUI();
    protected final DrawersPanel drawersPanel = new DrawersPanel();

    private int pinnedLeftW = -1, drawerW = 0;

    public MainGUI() {
        setLayout(null);
        add(content);
        add(drawersPanel);

        // proxy fields
        close = content.close; help = content.help;
        titleGui = content.titleGui; selectionPanel = content.selectionPanel;
        centerPanel = content.centerPanel; bottomPanel = content.bottomPanel;

        drawersPanel.setVisible(false);
    	selectionPanel.go.setEnabled(false);
    }

    @Override
    public void doLayout() {
        int h = getHeight();
        if (!isDrawerOpen()) {
            content.setBounds(0, 0, getWidth(), h);
            drawersPanel.setBounds(getWidth(), 0, 0, h);
            return;
        }
        int leftW = pinnedLeftW > 0 ? pinnedLeftW : getWidth();
        content.setBounds(0, 0, leftW, h);
        drawersPanel.setBounds(leftW, 0, getWidth() - leftW, h);
    }
    
    protected void setLoaded(boolean isInstalled) {
    	
    }

    private Window window() { return SwingUtilities.getWindowAncestor(this); }

    private void resizeWindowBy(int dx) {
        Window w = window();
        if (w == null || dx == 0) return;
        w.setSize(w.getWidth() + dx, w.getHeight());
        w.validate();
    }

    private int computeDrawerW(int leftW) { return Math.max(MIN_DRAWER_SIZE, leftW / 2); }

    protected void toggleModelDrawer() { 
    	setModelDrawerOpen(!isModelsDrawerOpen()); }
    protected void toggleImageDrawer() { setImageDrawerOpen(!isImagesDrawerOpen()); }

    public void setModelDrawerOpen(boolean open) {
        if (isModelsDrawerOpen() == open) 
        	return;
    	boolean wasOpened = isDrawerOpen();
        if (open) {
            isModelDrawer = true;
            ((CardLayout) drawersPanel.getLayout()).show(drawersPanel, DrawersPanel.MODEL_TAG);
        	if (wasOpened) {
                return;
        	}
        	isDrawerOpen = true;
            this.drawersPanel.setVisible(true);
            pinnedLeftW = getWidth() > 0 ? getWidth() : 600;
            drawerW = computeDrawerW(pinnedLeftW);
            SwingUtilities.invokeLater(() -> resizeWindowBy(drawerW));
        } else {
            this.drawersPanel.setVisible(false);
            isModelDrawer = false;
            isDrawerOpen = false;
	        final int shrink = drawerW;   // <<< capture BEFORE resetting
	        pinnedLeftW = -1;
	        drawerW = 0;
	        SwingUtilities.invokeLater(() -> resizeWindowBy(-shrink));
        }
        revalidate();
        repaint();
    }

    public void setImageDrawerOpen(boolean open) {
        if (isImagesDrawerOpen() == open) 
        	return;
    	boolean wasOpened = isDrawerOpen();
        if (open) {
        	isModelDrawer = !open;
            ((CardLayout) drawersPanel.getLayout()).show(drawersPanel, DrawersPanel.IMAGE_TAG);
        	if (wasOpened) {
                return;
        	}
        	isDrawerOpen = true;
            this.drawersPanel.setVisible(true);
            pinnedLeftW = getWidth() > 0 ? getWidth() : 600;
            drawerW = computeDrawerW(pinnedLeftW);
            SwingUtilities.invokeLater(() -> resizeWindowBy(drawerW));
        } else {
            this.drawersPanel.setVisible(false);
            isModelDrawer = false;
            isDrawerOpen = false;
	        final int shrink = drawerW;   // <<< capture BEFORE resetting
	        pinnedLeftW = -1;
	        drawerW = 0;
	        SwingUtilities.invokeLater(() -> resizeWindowBy(-shrink));
        }
        revalidate();
        repaint();
    }
	
	protected boolean isDrawerOpen() {
		return this.isDrawerOpen;
	}
	
	protected boolean isModelsDrawerOpen() {
		return isDrawerOpen && isModelDrawer;
	}
	
	protected boolean isImagesDrawerOpen() {
		return isDrawerOpen && !isModelDrawer;
	}
	
	protected void manageInstalled(boolean isInstalled) {
		boolean isThereImage = selectionPanel.cmbImages.getSelectedObject() != null;
		if (!isInstalled) {
			if (isImagesDrawerOpen() || !isDrawerOpen())
				setModelDrawerOpen(true);
			this.selectionPanel.go.setEnabled(false);
		} else {
			this.selectionPanel.go.setEnabled(isThereImage);
		}
		this.centerPanel.radioButton1.setEnabled(false);
		this.centerPanel.radioButton2.setEnabled(false);
		this.centerPanel.instantCard.chkInstant.setEnabled(false);
		this.centerPanel.instantCard.propagate3D.setEnabled(false);
		this.centerPanel.batchCard.btnBatchSAMize.setEnabled(false);
		this.centerPanel.batchCard.stopProgressBtn.setEnabled(false);
		this.centerPanel.batchCard.batchProgress.setEnabled(false);
		this.centerPanel.batchCard.propagate3D.setEnabled(false);
		this.bottomPanel.export.setEnabled(false);
		this.bottomPanel.returnLargest.setEnabled(false);
	}
	
	protected void setLoading() {
		manageLoaded(false);
		this.selectionPanel.go.setLoading();
	}
	
	protected void manageLoaded(boolean isLoaded) {
		this.selectionPanel.go.setEnabled(!isLoaded);
		this.centerPanel.radioButton1.setEnabled(isLoaded);
		this.centerPanel.radioButton2.setEnabled(isLoaded);
		this.centerPanel.instantCard.chkInstant.setEnabled(isLoaded);
		this.centerPanel.instantCard.propagate3D.setEnabled(isLoaded);
		this.centerPanel.batchCard.btnBatchSAMize.setEnabled(isLoaded);
		this.centerPanel.batchCard.stopProgressBtn.setEnabled(isLoaded);
		this.centerPanel.batchCard.batchProgress.setEnabled(isLoaded);
		this.centerPanel.batchCard.propagate3D.setEnabled(isLoaded);
		this.bottomPanel.export.setEnabled(isLoaded);
		this.bottomPanel.returnLargest.setEnabled(isLoaded);
	}
	
    public void changeGUI() {
    	if (!uiReady) {
    		pendingModelGuiRefresh = true;
    		return;
    	}
    	setLoading();

        new Thread(() -> {
            boolean installed = selectionPanel.cmbModels.getSelectedModel().isInstalled();
            SwingUtilities.invokeLater(() -> {
            	manageInstalled(installed);
            });
        }).start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (uiReady) return;
        uiReady = true;

        SwingUtilities.invokeLater(() -> {
            if (pendingModelGuiRefresh) {
                changeGUI();
                pendingModelGuiRefresh = false;
            }
        });
    }
}