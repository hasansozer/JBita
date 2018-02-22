package test;

import static org.junit.Assert.*;

import org.junit.Test;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.dispatch.Await;
import akka.dispatch.Future;
import akka.pattern.Patterns;
import akka.util.Duration;
import akka.util.Timeout;
import schedule.RandomScheduler;
import schedule.Scheduler;
import sut.BoundedBuffer.Buffer;
import sut.BoundedBuffer.Consume;
import sut.BoundedBuffer.Consumer;
import sut.BoundedBuffer.Produce;
import sut.BoundedBuffer.Producer;

public class TestBoundedBuffer extends ActorSysGenTest {

	private int timeoutInSecs = 30;
	
	public TestBoundedBuffer(int param) {
		super(param);
	}  
	
	@Test
	public void testBoundedBuffer() {
		
		final int bufferCapacity = 3;
		final int noOfPutMessages = 2;
		final int noOfGetMessages = 2;
		
		Timeout timeout = new Timeout(Duration.create(timeoutInSecs, "seconds"));
		ActorSystem system = ActorSystem.create("BoundedBuffer");
		
		Scheduler.setSystem(system);
		Scheduler.setSchedule(schedules.get(index));
		RandomScheduler.setSystem(system);
	
		ActorRef buffer = system.actorOf(new Props(new UntypedActorFactory() {
		      public UntypedActor create() {
		        return new Buffer(bufferCapacity);
		      }
		    }), "buffer");
		
		ActorRef producer = system.actorOf(new Props(new UntypedActorFactory() {
		      public UntypedActor create() {
		        return new Producer(buffer);
		      }
		    }), "producer");
		
		ActorRef consumer = system.actorOf(new Props(new UntypedActorFactory() {
		      public UntypedActor create() {
		        return new Consumer(buffer);
		      }
		    }), "consumer");
	
		for (int i = 0; i < noOfPutMessages; i++) {
			producer.tell(new Produce());
		}
		
		for (int i = 0; i < noOfGetMessages; i++) {
			Future<Object> future = Patterns.ask(consumer, new Consume(), timeout);	
			try {
				String str = (String) Await.result(future, timeout.duration());
				System.out.println(str);
			} catch (Exception e) {
				fail("timeout waiting for consumer...");
			}
		}
	}
}
