package schedule.envelopes;

import schedule.LogicalMessage;
import akka.actor.ActorRef;

//case class in scala: immutable

public class PromiseEnvelope {
	
	public LogicalMessage message;
	public ActorRef sender;

	public PromiseEnvelope(LogicalMessage message, ActorRef sender) {
		this.message = message;
		this.sender = sender;
	}
}
