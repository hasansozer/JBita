package schedule;

import akka.routing.RoutedActorRef;
import akka.actor.ScalaActorRef;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import akka.util.Duration;
import akka.util.FiniteDuration;

import util.*;

public class RandomScheduler {
	
	private static ActorSystem theSystem;
	private static ConcurrentHashMap<Pair<String, String>, Long> senderReceiverToLastScheduledTime = new ConcurrentHashMap<Pair<String, String>, Long>();
	private static Random random = new Random(System.currentTimeMillis());
	private static int maxDelay = 100; // milliseconds
	private static long tickDuration = -1L; // milliseconds, read from config when theSystem is set
	private static long minDistance = 0L; // initialized when tickDuration is set
	
	public static synchronized void setSystem(ActorSystem system) {
	    reset();
	    theSystem = system;
	    tickDuration = theSystem.settings().config().getMilliseconds("akka.scheduler.tick-duration");
	    minDistance = (new Float((tickDuration * 1000000 * 1.2))).longValue(); // nanoseconds
	}

	private static synchronized void reset() {
		theSystem = null;
		senderReceiverToLastScheduledTime.clear();
		random = new Random(System.currentTimeMillis());		
	}
	
	public static void setMaxDelay(int delay) {
		maxDelay = delay;
	}

	public static synchronized ActorSystem getSystem() {
	    return theSystem;
	}
	
	public static synchronized FiniteDuration delay(ScalaActorRef receiver, Object message, ActorRef sender) {
		
		if(message == null)
			System.out.println(receiver + " " + sender);
		if(theSystem != null && message != null
			&& !(message instanceof LogicalMessage) //!already-delayed
			&& !ActorAnalysis.isSystemCreatedActor(((ActorRef)receiver).path().toString()) //!system-related
			&& !(receiver instanceof RoutedActorRef) //!router
			&& !Messages.isCreationMessage(message)
			&& !Messages.isStopMessage(message)			
			&& (sender == null || !ActorAnalysis.isSystemCreatedActor(sender.path().toString()))
			) {
			assert(tickDuration > 0);
			String realSenderPath = Scheduler.getActorPath(sender, null);
			String receiverPath = ((ActorRef)receiver).path().toString();
			long now = System.nanoTime();
			long delay = computeDelay(new Pair<String,String>(realSenderPath, receiverPath), now);
			return Duration.create(delay, TimeUnit.NANOSECONDS);
		} else
			return null;
	}
	
	private static long computeDelay(Pair<String,String> senderReceiverPair, long now) {
		long lastScheduledTime = now - tickDuration;
		if(senderReceiverToLastScheduledTime.get(senderReceiverPair) != null)
			lastScheduledTime = senderReceiverToLastScheduledTime.get(senderReceiverPair);
		long newDelay = random.nextInt(maxDelay) * 1000000;
		long completeDelay = (lastScheduledTime + minDistance) - now;
		if ((now + newDelay) > (lastScheduledTime + minDistance)) 
			completeDelay = newDelay;
		long newScheduledTime = now + completeDelay;

		assert(lastScheduledTime < newScheduledTime);
		if(lastScheduledTime >= newScheduledTime)
			System.out.println(String.format("last time= %s is less than new time = %s", lastScheduledTime, newScheduledTime));

		senderReceiverToLastScheduledTime.put(senderReceiverPair, newScheduledTime);
		return completeDelay;
	}
}
