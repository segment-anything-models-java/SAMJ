package ai.nets.samj.gui.roimanager.commands;

import java.util.List;

import ai.nets.samj.annotation.Mask;




public interface Command {
	public void execute();
	
	public void undo();
		
	public List<Mask> getMasks();
}
