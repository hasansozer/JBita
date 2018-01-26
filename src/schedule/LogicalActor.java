package schedule;

import java.util.regex.*;

public class LogicalActor {
	
	public static LogicalActor DeadLetterID = new LogicalActor("EntryPoint");
	public static LogicalActor GatlingHTTPHandlerID = new LogicalActor("GatlingHttpHandler");
	public static LogicalActor SchedulerID = new LogicalActor("Scheduler");

	public String objectType = "";
	public EventID creatorID = null;
	
	private static final String LogicalActorPattern = "(.*LogicalActor\\()(EventID\\(.*\\))(,)(.*)(\\))";
	
	public LogicalActor(EventID creatorID, String actorObjectClass) {
		objectType = actorObjectClass;
		this.creatorID = creatorID;
	}
	
	public LogicalActor(String actorObjectClass) {
		objectType = actorObjectClass;
		creatorID = new EventID(-1,-1);
	}
	
	public LogicalActor(EventID creatorID) {
		objectType = "";
		this.creatorID = creatorID;
	}
	
	public EventID getCreatorID() {
		return creatorID;
	}
	
	private static String logicalActorPattern = "(.*LogicalActor\\()(EventID\\(.*\\))(,)(.*)(\\))";

	public static LogicalActor parse(String actorIDStr) {
		
		// _, eventIDStr, _, objectType, _
	    // EventID.parse(eventIDStr), objectType
		Pattern pattern = Pattern.compile(logicalActorPattern);
        Matcher matcher = pattern.matcher(actorIDStr);
        if (matcher.matches()){
            return new LogicalActor(EventID.parse(matcher.group(2)), matcher.group(4));
        }
        return null;
	}

	public static int getHashCode(String ActorIDStr, Integer[] traceHashCodes) {
		Pattern pattern = Pattern.compile(logicalActorPattern);
		Matcher matcher = pattern.matcher(ActorIDStr);
		if (matcher.matches()){
			// _, eventIDStr, _, objectType, _
			EventID creatorID = EventID.parse(matcher.group(2));
			int parentHashCode = creatorID.creatorIndex;
			if (creatorID.creatorIndex >= 0)
				parentHashCode = traceHashCodes[creatorID.creatorIndex]; 
	        return (matcher.group(4) + parentHashCode + "" + creatorID.seqNum).hashCode();
		} 
        return -1;
	}
}
