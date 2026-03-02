package ai.nets.samj.gui.last;

import java.awt.Window;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainGUI extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int MIN_DRAWER_SIZE = 450;

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
        drawersPanel.setModelsOpen(false);
        drawersPanel.setImagesOpen(false);
    	selectionPanel.go.setEnabled(false);

        setTwoThirdsEnabled(false);
    }

    protected void setTwoThirdsEnabled(boolean enabled) {}

    @Override public void doLayout() {
        int h = getHeight();
        if (!drawersPanel.isOpen()) {
            content.setBounds(0, 0, getWidth(), h);
            drawersPanel.setBounds(getWidth(), 0, 0, h);
            return;
        }
        int leftW = pinnedLeftW > 0 ? pinnedLeftW : getWidth();
        content.setBounds(0, 0, leftW, h);
        drawersPanel.setBounds(leftW, 0, Math.max(0, getWidth() - leftW), h);
    }

    private Window window() { return SwingUtilities.getWindowAncestor(this); }

    private void resizeWindowBy(int dx) {
        Window w = window();
        if (w == null || dx == 0) return;
        w.setSize(w.getWidth() + dx, w.getHeight());
        w.validate();
    }

    private int computeDrawerW(int leftW) { return Math.max(MIN_DRAWER_SIZE, leftW / 2); }

    protected void toggleModelDrawer() { setModelDrawerOpen(!drawersPanel.isModelsOpen()); }
    protected void toggleImageDrawer() { setImageDrawerOpen(!drawersPanel.isImagesOpen()); }

    public void setModelDrawerOpen(boolean open) {
        if (drawersPanel.isModelsOpen() == open) 
        	return;
    	boolean wasOpened = drawersPanel.isOpen();
    	drawersPanel.setModelsOpen(open);
        if (open) {
        	if (wasOpened)
        		return;
            pinnedLeftW = getWidth() > 0 ? getWidth() : 600;
            drawerW = computeDrawerW(pinnedLeftW);
            SwingUtilities.invokeLater(() -> resizeWindowBy(drawerW));
        } else {
	        final int shrink = drawerW;   // <<< capture BEFORE resetting
	        pinnedLeftW = -1;
	        drawerW = 0;
	        SwingUtilities.invokeLater(() -> resizeWindowBy(-shrink));
        }
        revalidate();
        repaint();
    }

    public void setImageDrawerOpen(boolean open) {
        if (drawersPanel.isImagesOpen() == open) 
        	return;
    	boolean wasOpened = drawersPanel.isOpen();
    	drawersPanel.setImagesOpen(open);
        if (open) {
        	if (wasOpened)
        		return;
            pinnedLeftW = getWidth() > 0 ? getWidth() : 600;
            drawerW = computeDrawerW(pinnedLeftW);
            SwingUtilities.invokeLater(() -> resizeWindowBy(drawerW));
        } else {
	        final int shrink = drawerW;   // <<< capture BEFORE resetting
	        pinnedLeftW = -1;
	        drawerW = 0;
	        SwingUtilities.invokeLater(() -> resizeWindowBy(-shrink));
        }
        revalidate();
        repaint();
    }
}