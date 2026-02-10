package ai.nets.samj.gui.roimanager.commands;

import java.util.Arrays;
import java.util.List;

import org.proteovir.roimanager.RoiManager;
import org.proteovir.utils.Mask;


public class AddRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
  
	public AddRoiCommand(RoiManager roiManager, List<Mask> polys) {
		this.roiManager = roiManager;
		this.polys = polys;
	}
	
	public List<Mask> getMasks(){
		return polys;
	}
  
	@Override
	public void execute() {
		for (Mask m : polys)
			this.roiManager.addRoi(m);
	}
  
	@Override
	public void undo() {
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
		this.roiManager.updateShowAll();
	}
}