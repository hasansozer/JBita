package criteria;

import schedule.Trace;
import java.util.HashMap;
import java.util.ArrayList;

public abstract class Criterion {
	  
	protected String name;
	protected HashMap<Integer, ArrayList<String>> actorFiles;
	protected HashMap<Integer, String> actorHashToName;
		
	public Criterion(String name) {
		this.name = name;
		actorFiles = new HashMap<Integer, ArrayList<String>>();
		actorHashToName = new HashMap<Integer, String>();
	}
	
	public abstract boolean doesSatisfy(Trace t, int i, int j);
	public abstract int measureCoverage(ArrayList<String> traceFiles);
	public abstract boolean satisfy(Trace t, int i, int j);
}
