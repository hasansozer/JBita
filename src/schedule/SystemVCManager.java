package schedule;

import java.util.concurrent.ConcurrentHashMap;
import util.ActorAnalysis;

public class SystemVCManager {

	private ConcurrentHashMap<String, VectorClock> vectorClocks = new ConcurrentHashMap<String, VectorClock>();
			
	public void resetClocks() {
		vectorClocks.clear();
	}
	
	public VectorClock updateVectorClockForCreate(String parentPath, String childPath) {
		VectorClock returnVC = null;

		if (!vectorClocks.containsKey(childPath)) {
			String parent = getDeadLetterIfSystem(parentPath);
			if (vectorClocks.containsKey(parent)) {
				VectorClock vc = vectorClocks.get(parent);
	    	    vc.increase(parent);
	    	    returnVC = vc.clone();
	    	} else {
	    		VectorClock newVC = new VectorClock();
	    	    newVC.increase(parent);
	    	    vectorClocks.put(parent, newVC);
	    	    returnVC = newVC.clone();
	    	}
	    	
			VectorClock childVC = vectorClocks.get(parent).clone();
			vectorClocks.put(childPath, childVC);
		}
	    return returnVC;
	}

	private String getDeadLetterIfSystem(String actorPath) {
		if (ActorAnalysis.isSystemCreatedActor(actorPath) 
			|| ActorAnalysis.isTemp(actorPath))
			return ActorAnalysis.getDeadLetterActorPath();
	    else
	    	return actorPath;
	}

	public VectorClock updateVectorClockForReceive(String httpActorPath, VectorClock newVC) {
		return newVC.clone();
	}

	public VectorClock updateVectorClockForSend(String senderActorPath) {
	    String sender = getDeadLetterIfSystem(senderActorPath);
	    if (vectorClocks.containsKey(sender)) {
	    	VectorClock vc = vectorClocks.get(sender);
	    	vc.increase(sender);
	    	return vc.clone();
	    } else {
	    	VectorClock newVC = new VectorClock();
	    	newVC.increase(sender);
	    	vectorClocks.put(sender, newVC);
	    	return newVC.clone();
	    }
	}
}
