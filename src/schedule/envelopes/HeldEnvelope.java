package schedule.envelopes;

import akka.actor.ActorRef;
import akka.dispatch.Envelope;
import schedule.LogicalMessage;

// case class in scala: immutable

public class HeldEnvelope {

	public LogicalMessage logicalMessage;
	public PromiseEnvelope promiseEnvelope;
	public Envelope envelope;
	public ActorRef receiver;
	public ActorRef sender;
	
	public HeldEnvelope(ActorRef receiver, Envelope envelope, PromiseEnvelope promiseEnvelope) {
		this.receiver = receiver;
		this.sender = promiseEnvelope.sender;
		this.envelope = envelope;
		this.logicalMessage = promiseEnvelope.message;
		if(envelope != null) {
			this.sender = envelope.sender();
			this.logicalMessage = (LogicalMessage) envelope.message();
		}
	}
	
	public HeldEnvelope(ActorRef receiver, Envelope envelope) {
		this(receiver, envelope, null);
	}
	
	public HeldEnvelope(ActorRef receiver, PromiseEnvelope promiseEnvelope) {
		this(receiver, null, promiseEnvelope);
	}
	
	@Override
	public String toString() {
		return logicalMessage.message.toString() + ", " +
			    logicalMessage.creatorID + ", " + sender + "," + receiver;
	}
}
