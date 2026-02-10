package ai.nets.samj.gui.roimanager.commands;

import java.util.List;

import org.proteovir.utils.Mask;



public interface Command {
	public void execute();
	
	public void undo();
		
	public List<Mask> getMasks();
}
