package schedule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogicalMessage {

	public Object message;
	public EventID creatorID;
	public VectorClock vc;
	
	private static final String LogicalMessagePattern = "(.*LogicalMessage\\()(.*)(,)(EventID\\(.*\\))(,)(Map\\(.*\\))(\\).*)";
	
	public LogicalMessage(Object message, EventID creatorID, VectorClock vc) {
		this.message = message;
		this.creatorID = creatorID;
		this.vc = vc;
	}
	
	public LogicalMessage(Object message, EventID creatorID) {
		this(message, creatorID, null);
	}

	public static LogicalMessage parse(String logicalMessageString) {
		Pattern pattern = Pattern.compile(LogicalMessagePattern);
		Matcher matcher = pattern.matcher(logicalMessageString);
		if (matcher.matches()){ // _, message, _, messageIDStr, _, vcMapStr, _
			EventID messageID = EventID.parse(matcher.group(4));
			VectorClock vc = VectorClock.parse(matcher.group(6));
			return new LogicalMessage(matcher.group(2), messageID, vc);
        }
		return null;
	}

	public static int getHashCode(String logicalMessageString, Integer[] traceHashCodes) {
		Pattern pattern = Pattern.compile(LogicalMessagePattern);
		Matcher matcher = pattern.matcher(logicalMessageString);
		if (matcher.matches()){ // _, message, _, creatorIDStr, _, vcMapStr, _
			EventID creatorID = EventID.parse(matcher.group(4));
			
			String messageType = matcher.group(2);
			boolean hasParameter = (messageType.indexOf("(") >= 0);
			boolean isObject = (messageType.indexOf("@") >= 0);
			if (hasParameter) 
				messageType = messageType.substring(0, messageType.indexOf("("));
			else if(isObject)
				messageType = messageType.substring(0, messageType.indexOf("@"));
			
			int parentHashCode = creatorID.creatorIndex;
			if(creatorID.creatorIndex >= 0)
				parentHashCode = traceHashCodes[creatorID.creatorIndex];
			
			String logicalMessageHashString = matcher.group(2);
			if (!(matcher.group(2).contains("ReceiveTimeout") || matcher.group(2).contains("Heartbeat"))) {
				String parentHashCodeString = "" + parentHashCode;
				int length = 0;
				for(Integer code : traceHashCodes)
					if(code == parentHashCode)
						length++;
				if(length > 1)
					parentHashCodeString += length;
	            logicalMessageHashString = messageType + parentHashCodeString + creatorID.seqNum;
			}
			
			//System.out.println(messageType + "+" + parentHashCode + "+" + creatorID.seqNum + ": ");
	        return logicalMessageHashString.hashCode();
		}
		return -1;
	}
}
