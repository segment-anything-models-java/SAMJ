package ai.nets.samj.gui.roimanager.commands;

import java.awt.Polygon;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.proteovir.roimanager.RoiManager;
import org.proteovir.utils.Mask;


public class ModifyRoiCommand implements Command {
	private RoiManager roiManager;
	private final List<Mask> polys;
	private HashMap<String, HashMap<String, Polygon>> modsMap = new HashMap<>();
	
	private static final String OLD_KEY = "oldPolygon";
	private static final String NEW_KEY = "newPolygon";
  
	public ModifyRoiCommand(RoiManager roiManager, List<Mask> polys) {
		this.roiManager = roiManager;
		this.polys = polys.stream().collect(Collectors.toList());
	}
	
	public void setOldContour(String id, Polygon oldContour) {
		if (modsMap.get(id) == null) {
			HashMap<String, Polygon> idMap = new HashMap<String, Polygon>();
			idMap.put(OLD_KEY, null);
			idMap.put(NEW_KEY, null);
			modsMap.put(id, idMap);
		} else if (modsMap.get(id).get(OLD_KEY) != null) {
			return;
		}
		modsMap.get(id).put(OLD_KEY, oldContour);
	}
	
	public void setNewContour(String id, Polygon newContour) {
		if (modsMap.get(id) == null) {
			HashMap<String, Polygon> idMap = new HashMap<String, Polygon>();
			idMap.put(OLD_KEY, null);
			idMap.put(NEW_KEY, null);
			modsMap.put(id, idMap);
		}
		modsMap.get(id).put(NEW_KEY, newContour);
	}
	
	public List<Mask> getMasks(){
		return polys;
	}
  
	@Override
	public void execute() {
		int n = polys.size();
		Mask modMask = null;
		for (int i = n - 1; i >= 0; i --) {
			Mask m = polys.get(i);
			if (modsMap.get(m.getUUID()) == null)
				continue;
			if (modsMap.get(m.getUUID()).get(NEW_KEY) == null)
				roiManager.delete(i);
			else {
				roiManager.getRoisAsArray()[i].setContour(modsMap.get(m.getUUID()).get(NEW_KEY));
				modMask = roiManager.getRoisAsArray()[i];
			}
		}
		this.roiManager.updateShowAll();
		if (modsMap.keySet().size() == 1)
			this.roiManager.select(modMask);
	}
  
	@Override
	public void undo() {
		this.roiManager.deleteAll();
		Mask modMask = null;
		for (Mask m : polys) {
			if (modsMap.get(m.getUUID()) != null) {
				m.setContour(modsMap.get(m.getUUID()).get(OLD_KEY));
				modMask = m;
			}
			this.roiManager.addRoi(m);
		}
		if (modsMap.keySet().size() == 1)
			this.roiManager.select(modMask);
	}
}