package ai.nets.samj.gui.roimanager.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.gui.roimanager.RoiManager;


public class DeleteRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
  
	public DeleteRoiCommand(RoiManager roiManager, List<Mask> polys) {
		this.roiManager = roiManager;
		this.polys = polys.stream().collect(Collectors.toList());
	}
	
	public void execute() {
		for (Mask rr2 : polys) {
	    	for (int n = this.roiManager.getROIsNumber() - 1; n >= 0; n --) {
	    		Mask rr = roiManager.getRoisAsArray()[n];
    			if (!Arrays.equals(rr.getContour().xpoints, rr2.getContour().xpoints))
    				continue;
    			if (!Arrays.equals(rr.getContour().ypoints, rr2.getContour().ypoints))
    				continue;
	    		roiManager.delete(n);
	    		break;		    		
	    	}
		}
		roiManager.updateShowAll();
	}
	
	public void undo() {
		for (Mask m : polys) {
			roiManager.addRoi(m);;
		}
		roiManager.updateShowAll();
	}
	
	@Override
	public List<Mask> getMasks(){
		return polys;
	}
}
