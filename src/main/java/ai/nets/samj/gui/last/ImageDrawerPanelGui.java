package ai.nets.samj.gui.last;


import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Stack;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import ai.nets.samj.gui.roimanager.RoiManager;
import ai.nets.samj.gui.roimanager.RoiManagerConsumer;
import ai.nets.samj.gui.roimanager.commands.Command;

public class ImageDrawerPanelGui extends JPanel {

    private static final long serialVersionUID = -8070874774405110221L;
	private JLabel drawerTitle = new JLabel();
	protected RoiManager roiManager;
	/**
	 * Tracks if Ctrl+Z has already been handled
	 */
	protected boolean undoPressed = false;
	/**
	 * Tracks if Ctrl+Y has already been handled
	 */
	protected boolean redoPressed = false;
	/**
	* List of the annotated masks on an image
    */
	protected Stack<Command> annotatedMask = new Stack<Command>();
	/**
    * List that keeps track of the annotated masks
    */
	protected Stack<Command> redoAnnotatedMask = new Stack<Command>();
        
    private static final String MODEL_TITLE = ""
    		+ "<html>"
    		+ "<span style=\"color: blue;\">ROI Manager</span>"
    		+ "</html>";;
    
    private static double TITLE_RATIO = 0.15;
	
	
	protected ImageDrawerPanelGui(RoiManagerConsumer consumer) {
		setFocusable(true);
		setBorder(BorderFactory.createLineBorder(Color.black));
		
		drawerTitle = new JLabel(MODEL_TITLE);
		drawerTitle.setHorizontalAlignment(JLabel.CENTER);
		drawerTitle.setVerticalAlignment(JLabel.CENTER);
		
		roiManager = new RoiManager(consumer);
		roiManager.block(false);
        
		add(drawerTitle);
		add(roiManager);
		setActionMapForUndoRedo();
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
    
    @Override
    public void setVisible(boolean visible) {
    	super.setVisible(visible);
    }

	private void setActionMapForUndoRedo() {
    	InputMap  im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    	ActionMap am = getActionMap();

    	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
    	am.put("undo", new AbstractAction() {
    	    private static final long serialVersionUID = 8471444501293017255L;

			@Override
    	    public void actionPerformed(ActionEvent e) {
    	        // almost the same logic as your keyPressed:
    	        if (!redoPressed && annotatedMask.size() > 0) {
    	            redoPressed = true;
    	            Command undo = annotatedMask.pop();
    	            undo.undo();
    	            redoAnnotatedMask.push(undo);
    	        }
    	    }
    	});

    	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
    	am.put("redo", new AbstractAction() {
    	    private static final long serialVersionUID = 5207212866704104118L;

			@Override
    	    public void actionPerformed(ActionEvent e) {
    	        if (!undoPressed && redoAnnotatedMask.size() > 0) {
    	            undoPressed = true;
    	            Command redo = redoAnnotatedMask.pop();
    	            redo.execute();
    	            annotatedMask.push(redo);
    	        }
    	    }
    	});
    	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, true), "undoReleased");
    	am.put("undoReleased", new AbstractAction() {
    	    private static final long serialVersionUID = 2184878126792218374L;
			@Override public void actionPerformed(ActionEvent e) {
    	        redoPressed = false;
    	    }
    	});
    	im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK, true), "redoReleased");
    	am.put("redoReleased", new AbstractAction() {
    	    private static final long serialVersionUID = 1006214991556606511L;
			@Override public void actionPerformed(ActionEvent e) {
    	        undoPressed = false;
    	    }
    	});
	}

}
