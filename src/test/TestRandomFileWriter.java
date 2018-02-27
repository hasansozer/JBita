package test;

import static org.junit.Assert.fail;

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
import sut.BoundedBuffer.Consume;
import sut.FileWriter.*;

public class TestRandomFileWriter extends ActorSysTest {

	public TestRandomFileWriter(int param) {
		super(param);
	}

	@Test
	public void test() {
		Timeout timeout = new Timeout(Duration.create(TIMEOUT_SEC, "seconds"));
		ActorSystem system = ActorSystem.create("FileWriter");
		
		Scheduler.setSystem(system);
		RandomScheduler.setSystem(system);
		
		ActorRef writer = system.actorOf(new Props(new UntypedActorFactory() {
		      public UntypedActor create() {
		        return new Writer();
		      }
		    }), "writer");
		
		ActorRef terminator = system.actorOf(new Props(new UntypedActorFactory() {
		      public UntypedActor create() {
		        return new Terminator(1, writer);
		      }
		    }), "terminator");

		ActorRef action = system.actorOf(new Props(new UntypedActorFactory() {
		      public UntypedActor create() {
		        return new Action("n", terminator, writer);
		      }
		    }), "action");
		
		Patterns.ask(action, new Execute(), timeout);
		Future<Object> future = Patterns.ask(terminator, new CheckForError(), timeout);	
		try {
			String str = (String) Await.result(future, timeout.duration());
			System.out.println(str);
			assert(str.length() == 0);
		} catch (Exception e) {
			fail("timeout waiting for terminator...");
		}
	}
}
