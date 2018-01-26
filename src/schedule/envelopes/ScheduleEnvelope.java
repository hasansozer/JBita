package schedule.envelopes;

import schedule.LogicalMessage;

// case class in scala - immutable

public class ScheduleEnvelope {
	
	public String receiver;
	public LogicalMessage logicalMessage;
	public boolean cmh;

	public ScheduleEnvelope(String receiver, LogicalMessage logicalMessage, boolean cmh) {
		this.receiver = receiver;
		this.logicalMessage = logicalMessage;
		this.cmh = cmh;
	}
}
