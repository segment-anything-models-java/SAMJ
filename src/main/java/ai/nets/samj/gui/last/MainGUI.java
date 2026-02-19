package ai.nets.samj.gui.last;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainGUI extends JPanel {
    

    private static final long serialVersionUID = 5067309009579235529L;
    
	protected JButton close = new JButton("Close");
    protected JButton help = new JButton("Help");
    protected TitleGUI titleGui;
    protected SelectionPanel selectionPanel;
    protected Center centerPanel;
    protected DrawersPanel drawersPanel;
    protected BottomPanel bottomPanel;

    
    // Tune these
    private static final double TITLE_H_PCT      = 0.10;
    private static final double SELECTION_H_PCT  = 0.25;
    private static final double CENTER_H_PCT     = 0.45;
    private static final double BOTTOM_H_PCT     = 0.15;
   // The last row is whatever height is left (typically ~0.10)
    private static final int MIN_DRAWER_SIZE = 450;

   private static final int BUTTONS_GAP_PX = 4;

    public MainGUI() {
        setLayout(null);

        titleGui = new TitleGUI();
        selectionPanel = new SelectionPanel();
        centerPanel = new Center();
        drawersPanel = new DrawersPanel();
        bottomPanel = new BottomPanel();

        add(titleGui);
        add(selectionPanel);
        add(centerPanel);
        add(drawersPanel);
        add(bottomPanel);
        add(close);
        add(help);

        drawersPanel.setVisible(true);

        this.setTwoThirdsEnabled(false);
    }

    @Override
    public void doLayout() {
        final int gap = BUTTONS_GAP_PX;
        int wTit = getWidth();
        int wDrawer = 0;
        if (drawersPanel.isOpen()) {
        	wDrawer = Math.max(MIN_DRAWER_SIZE, wTit / 2);
        	wTit -= wDrawer;
        }
        final int w = Math.max(0, wTit - gap * 2);
        final int h = getHeight();
        
        final int hTitle     = (int) Math.round(h * TITLE_H_PCT);
        final int hSelection = Math.max(0, - gap * 2 + (int) Math.round(h * SELECTION_H_PCT));
        final int hCenter    = Math.max(0, - gap * 2 + (int) Math.round(h * CENTER_H_PCT));
        final int hBottom    = Math.max(0, - gap * 2 + (int) Math.round(h * BOTTOM_H_PCT));

        int y = 0;
        
        drawersPanel.setBounds(wTit, 0, wDrawer, getHeight());

        // Row 1
        titleGui.setBounds(0, y, wTit, hTitle);
        y += hTitle + gap;

        // Row 2
        selectionPanel.setBounds(gap, y, w, hSelection);
        y += hSelection + gap;

        // Row 3
        centerPanel.setBounds(gap, y, w, hCenter);
        //drawersPanel.setBounds(0, y, w, hCenter); // overlay same row
        y += hCenter + gap;

        // Row 4
        bottomPanel.setBounds(gap, y, w, hBottom);
        y += hBottom + gap;

        // Row 5 (remaining)
        int hButtons = h - y - gap * 2;
        if (hButtons < 0) hButtons = 0;

        int W = w / 2;

        close.setBounds(gap, y, W, hButtons);
        help.setBounds(gap + W, y, W, hButtons);
    }

    protected void setTwoThirdsEnabled(boolean enabled) {
    }

    protected void toggleModelDrawer() {
    	setModelDrawerOpen(!drawersPanel.isModelsOpen());
    }

    protected void toggleImageDrawer() {
    	setImageDrawerOpen(!drawersPanel.isImagesOpen());
    }
    
    public void prepareDrawer(boolean open) {
    	if (open && !drawersPanel.isOpen()) {
    		int extraWidth = Math.max(MIN_DRAWER_SIZE, getWidth());
    		this.setSize(getWidth() + extraWidth, getHeight());
    	} else if (!open && drawersPanel.isOpen()) {
    		int extraWidth = Math.max(MIN_DRAWER_SIZE, getWidth() / 2);
    		this.setSize(getWidth() - extraWidth, getHeight());
    	}
    }
    
    public void setModelDrawerOpen(boolean open) {
    	if (drawersPanel.isModelsOpen() && open)
    		return;
    	else if (!drawersPanel.isModelsOpen() && !open)
    		return;
    	prepareDrawer(open);
    	drawersPanel.setModelsOpen(open);

        // trigger a new layout pass
        revalidate();
        repaint();
    }
    
    public void setImageDrawerOpen(boolean open) {
    	if (drawersPanel.isImagesOpen() && open)
    		return;
    	else if (!drawersPanel.isImagesOpen() && !open)
    		return;
    	prepareDrawer(open);
    	drawersPanel.setImagesOpen(open);

        // trigger a new layout pass
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.JFrame frame = new javax.swing.JFrame("MainGUI");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

            MainGUI gui = new MainGUI();
            frame.setContentPane(gui);

            // Pick one:
            frame.setSize(250, 400);          // fixed size for quick testing
            // frame.pack();                  // use if your components have preferred sizes

            frame.setLocationRelativeTo(null); // center on screen
            frame.setVisible(true);
        });
    }
}
