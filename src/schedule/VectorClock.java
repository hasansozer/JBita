package schedule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VectorClock {
	
	private static final String VCMapPattern = "(Map\\()(.*)(\\).*)";
	private static final String VCPattern = "(.*)( -> )(\\d*)(.*)";

	public ConcurrentHashMap<String, Integer> vc = new ConcurrentHashMap<String, Integer>();
			
	@Override
	public VectorClock clone() {
		VectorClock cloneVC = new VectorClock();
		cloneVC.vc = new ConcurrentHashMap<String, Integer>(vc);
		return cloneVC;
	}

	public void increase(String actorPath) {
		if (vc.contains(actorPath)) {
		    int curClock = vc.get(actorPath);
		    vc.put(actorPath, curClock + 1);
		} else 
			vc.put(actorPath, 1);
	}
	
	public void put(String actorPath, int clockValue) {
		vc.put(actorPath, clockValue);
	}

	@Override
	public String toString() {
		String output = "Map(";
		
		Iterator<Entry<String, Integer>> it = vc.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
    		output += pair.getKey() + " -> " + pair.getValue() + ", ";
    	}
    	output = output.substring(0, output.length()-2);
    	output += ")";
		return output;
	}
	
	public void updateToMax(VectorClock otherVC) {
	    for (ConcurrentHashMap.Entry<String, Integer> entry : otherVC.vc.entrySet()) {
	      if (!vc.contains(entry.getKey()) || vc.get(entry.getKey()) < entry.getValue())
	        vc.put(entry.getKey(), entry.getValue());
	    }
	}

	public static VectorClock parse(String vcMapString) {

		VectorClock vcObject = new VectorClock();
		Pattern patternVCMAP = Pattern.compile(VCMapPattern);
		Pattern patternVC = Pattern.compile(VCPattern);
		Matcher matcherVCMAP = patternVCMAP.matcher(vcMapString);
		if (matcherVCMAP.matches()){
			// _, vcs, _
			String[] vcArrays = matcherVCMAP.group(2).split(",");
			for(String vc : vcArrays) {
				Matcher matcherVC = patternVC.matcher(vc);
				if(matcherVC.matches()) //actorPath, _, vcValue, _
					vcObject.put(matcherVC.group(1).trim(), Integer.parseInt(matcherVC.group(3)));
			}
        } 
		return vcObject;
	}
	
	public static boolean greaterThanEq(VectorClock v1, VectorClock v2) {
		
		if (v1.vc.isEmpty() && v2.vc.isEmpty()) 
			 return true;
		
		Iterator<Entry<String, Integer>> it = v2.vc.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
    		if(!v1.vc.containsKey(pair.getKey()) 
    			|| v1.vc.get(pair.getKey()) < pair.getValue())
    			return false;
    	}
		return true;
	}
	
	public static boolean lessThanEq(VectorClock v1, VectorClock v2) {
		
		Iterator<Entry<String, Integer>> it = v1.vc.entrySet().iterator();
    	while (it.hasNext()) {
    		Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
    		if(!v2.vc.containsKey(pair.getKey()) 
    			|| pair.getValue() > v2.vc.get(pair.getKey()))
    			return false;
    	}
	    return true;
	}
}
