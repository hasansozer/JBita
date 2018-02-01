package schedule;

import java.util.regex.*;

public class EventID {

	public int creatorIndex = -1;
	public int seqNum = -1;
	
	public EventID(int creatorIndex, int seqNum) {
		this.creatorIndex = creatorIndex;
		this.seqNum = seqNum;
	}
	
	public boolean isEqualTo(EventID otherID) {
		return (this.creatorIndex == otherID.creatorIndex && this.seqNum == otherID.seqNum); 
	}

	@Override
	public int hashCode() {
		return ("" + creatorIndex + "" + seqNum).hashCode();
	}

	private static String eventIDPattern = "(EventID\\()(-?\\d*)(,)(-?\\d*)(\\)*)";
			
	@Override
	public String toString() {		
		return "EventID(" + creatorIndex + "," + seqNum + ")";
	}

	public static EventID parse(String eventIDStr) {
		// _, index, _, seq, _
	    // EventID.parse(eventIDStr), objectType
		Pattern pattern = Pattern.compile(eventIDPattern);
        Matcher matcher = pattern.matcher(eventIDStr);
        if (matcher.matches()){
            return new EventID(Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(4)));
        }
        return null;
	}
}
 