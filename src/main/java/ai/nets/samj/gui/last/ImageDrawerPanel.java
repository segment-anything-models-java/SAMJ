package ai.nets.samj.gui.last;


import java.awt.Polygon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.gui.roimanager.RoiManagerConsumer;
import ai.nets.samj.gui.roimanager.commands.AddRoiCommand;
import ai.nets.samj.gui.roimanager.commands.Command;
import ai.nets.samj.gui.roimanager.commands.DeleteRoiCommand;

public class ImageDrawerPanel extends ImageDrawerPanelGui implements KeyListener {

    /**
	 * Counter of the ROIs created
	 */
	private int promptsCreatedCnt = 0;
	
	private static final long serialVersionUID = -6984455225757235982L;

	private ImageDrawerPanel(RoiManagerConsumer consumer) {
		super(consumer);
		roiManager.addCommandCallback((cmd) -> {
			redoAnnotatedMask.clear();
			annotatedMask.add(cmd);
		});
    }
	
	protected static ImageDrawerPanel create() {
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
	
	void addToRoiManager(final List<Mask> polys, final String promptShape, String modelName) {
		if (this.roiManager.getROIsNumber() == 0 && annotatedMask.size() != 0 
				&& !(annotatedMask.peek() instanceof DeleteRoiCommand)) {
			annotatedMask.clear();
			roiManager.deleteAll();
		}
			
		this.redoAnnotatedMask.clear();
		promptsCreatedCnt++;
		int resNo = 1;
		List<Mask> masks = new ArrayList<Mask>();
		for (Mask m : polys) {
			m.setName(promptsCreatedCnt + "." + (resNo ++) + "_" + promptShape + "_" + modelName);
			masks.add(m);
		}
		Command command = new AddRoiCommand(roiManager, masks);
		command.execute();
		this.annotatedMask.push(command);
	}

	@Override
	public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z && this.annotatedMask.size() != 0 && !redoPressed) {
        	redoPressed = true;
        	Command undo = annotatedMask.pop();
        	undo.undo();
        	redoAnnotatedMask.push(undo);
        } else if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Y && this.redoAnnotatedMask.size() != 0 && !undoPressed) {
        	undoPressed = true;
        	Command redo = redoAnnotatedMask.pop();
        	redo.execute();
        	annotatedMask.push(redo);
        }
        e.consume();
	}

	@Override
	public void keyReleased(KeyEvent e) {
	    if (e.getKeyCode() == KeyEvent.VK_Z) {
	        redoPressed = false;
			e.consume();
	    }
	    if (e.getKeyCode() == KeyEvent.VK_Y) {
	        undoPressed = false;
			e.consume();
	    }
	}

	@Override
	public void keyTyped(KeyEvent e) {}

}
