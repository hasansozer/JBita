package schedule;

public class ScheduleGenerator {

	private boolean haveOtherReorderingConstraints(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);
		return (haveFIFOConstraint(i, j, trace) 
				|| eventI.promiseResponse || eventJ.promiseResponse);
	}

	private boolean haveFIFOConstraint(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);

		String receiverI = eventI.receiverIDStr;
		String receiverJ = eventJ.receiverIDStr;
		String senderI = eventI.senderIDStr;
		String senderJ = eventJ.senderIDStr;

		return ((senderI.equals(senderJ) && receiverI.equals(receiverJ)));
	}	
	
	private boolean isCausallyRelated(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);

		LogicalMessage logicalMessageJ = (LogicalMessage)eventJ.message;
		int creatorIndex = logicalMessageJ.creatorID.creatorIndex;

		if (creatorIndex == i)
			return true;
		else if (i == j 
			|| VectorClock.greaterThanEq(logicalMessageJ.vc, eventI.vc)) 
			return true;

		return false;
	}
	
	private boolean canBeReordered(int i, int j, Trace t) {
		return !isCausallyRelated(i, j, t) 
				&& !haveOtherReorderingConstraints(i, j, t);
	}
}
