package schedule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.ActorAnalysis;

public class Event {
	
	public static final int ROOT_EVENT = -1;
	public static final int SCHEDULER_EVENT = -2;
	public static final int HTTP_EVENT = -3;
	
	public static Event rootEvent = new Event(ROOT_EVENT, "", 
			ActorAnalysis.getSystemGuardianActorPath(), 
			ActorAnalysis.getSystemGuardianActorPath(), 
			null, false);
	public static Event schedulerEvent = new Event(SCHEDULER_EVENT, "", 
			ActorAnalysis.getSystemGuardianActorPath(), 
			ActorAnalysis.getSystemGuardianActorPath(), 
			null, false);
	public static Event httpEvent = new Event(HTTP_EVENT, "", 
			ActorAnalysis.getSystemGuardianActorPath(), 
			ActorAnalysis.getSystemGuardianActorPath(), 
			null, false);
	
	public Set<String> create = null;
	public Set<String> stop = null;
	
	public int index = -1;
	public Object message = null;
	public String senderIDStr = "";
	public String receiverIDStr = "";
	public VectorClock vc = null;
	public boolean promiseResponse = false;
	public boolean cmh = false;
	
	public int createSequenceNum = 0;
	
	public ConcurrentHashMap<String,Integer> receiversSeqNumMap = new ConcurrentHashMap<String,Integer>();
	public int hashCodeInTrace = -1;
	
	private static final String EventPattern = "(\\d+)(: <)(LogicalActor\\(.*\\)|null)(,)(LogicalMessage\\(.*\\)\\))(,)(LogicalActor\\(.*\\)|null)(,)(Map\\(.*\\)|null)(,)(true|false)(,)(true|false)(>.*)";
	
	public Event(int index, 
				Object message, 
				String senderIDStr, 
				String receiverIDStr,
				VectorClock vc,
				boolean promiseResponse,
				boolean cmh) {
		
		create = Collections.synchronizedSet(new HashSet<String>());
		stop = Collections.synchronizedSet(new HashSet<String>());
		
		this.index = index;
		this.message = message;
		this.senderIDStr = senderIDStr;
		this.receiverIDStr = receiverIDStr;
		this.vc = vc;
		this.promiseResponse = promiseResponse;
		this.cmh = cmh;
	}
	
	public Event(int index, 
			Object message, 
			String senderIDStr, 
			String receiverIDStr,
			VectorClock vc,
			boolean promiseResponse) {
		this(index, message, senderIDStr, receiverIDStr, vc, promiseResponse, false);
	}

	public void increaseClock(String receiverPath) {
		
		if(index == SCHEDULER_EVENT) {
			if(receiverPath != null) {
				if(receiversSeqNumMap.contains(receiverPath))
					receiversSeqNumMap.replace(receiverPath, receiversSeqNumMap.get(receiverPath) + 1);
			} else
				receiversSeqNumMap.put(receiverPath, 1);		
		} else {
		
			if (receiverPath != null) {
				if (receiversSeqNumMap.contains(receiverPath)) {
					int currClock = receiversSeqNumMap.get(receiverPath) + 1;
					receiversSeqNumMap.replace(receiverPath, currClock);
					assert(receiversSeqNumMap.get(receiverPath) == currClock);
				} else
					receiversSeqNumMap.put(receiverPath, 1);
			} else /* If the path is null, then increases the creation sequence number. */
				createSequenceNum++;
			}
	}
	
	public EventID getID(String receiverPath) {
		if(receiverPath != null) {
			return new EventID(index, receiversSeqNumMap.get(receiverPath));
		} else if(index != SCHEDULER_EVENT) {
			return new EventID(index, createSequenceNum);
		} else
			return null;
	}
	
	public static Event parse(String eventStr) {
		Pattern pattern = Pattern.compile(EventPattern);
		Matcher matcher = pattern.matcher(eventStr);
		if (matcher.matches()){
			// indexStr, _, receiverStr, _, logicalMessageStr, _, senderStr, _, vcMapStr, _, promiseResponseStr, _, cmhStr, _
			LogicalMessage message = LogicalMessage.parse(matcher.group(5));
			VectorClock vc = VectorClock.parse(matcher.group(9));
			boolean promiseResponse = Boolean.parseBoolean(matcher.group(11));
			boolean cmh = Boolean.parseBoolean(matcher.group(13));
			return new Event(Integer.parseInt(matcher.group(1)), message, matcher.group(7), matcher.group(3), vc, promiseResponse, cmh);
        }
        
		System.out.println("**** ERROR! Could not parse event: " + eventStr);
        return null;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
	    Event clonedEvent = new Event(index, message, senderIDStr, receiverIDStr, vc, promiseResponse);
	    clonedEvent.hashCodeInTrace = hashCodeInTrace;
	    clonedEvent.cmh = cmh;
	    return clonedEvent;
	}

	@Override
	public String toString() {
	    LogicalMessage newMessage = (LogicalMessage) message;
	    newMessage.message = newMessage.message.toString().replace("\n", "").replace(",", ";");
	    return (index + ": <" + receiverIDStr + "," + newMessage + "," + senderIDStr + "," + vc + "," + promiseResponse + "," + cmh + ">");
	}

	public static int getHashCode(String eventStr, Integer[] traceHashCodes) {
		Pattern pattern = Pattern.compile(EventPattern);
		Matcher matcher = pattern.matcher(eventStr);
		if (matcher.matches()){
			// indexStr, _, receiverStr, _, logicalMessageStr, _, senderStr, _, vcMapStr, _, promiseResponseStr, _, cmhStr, _
			String eventHashString = ""
					+ "" + LogicalMessage.getHashCode(matcher.group(5), traceHashCodes) 
					+ "" + LogicalActor.getHashCode(matcher.group(7), traceHashCodes) 
					+ "" + LogicalActor.getHashCode(matcher.group(3), traceHashCodes);
			//System.out.println("" + eventHashString.hashCode());
			return eventHashString.hashCode();
		} 
        
		System.out.println("**** ERROR! Could not parse event: " + eventStr + "to obtain hash code");
        return -1;
	}
}
