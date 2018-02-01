package test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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

import criteria.*;
import sut.BoundedBuffer.*;
import schedule.Scheduler;
import schedule.RandomScheduler;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import java.io.File;

@RunWith(Parameterized.class)
public class RandomTest {

	private int index = 0;
	private int timeoutInSecs = 30;
	
	private static final int TEST_COUNT = 4;
	private static final String TRACES_FOLDER = "./test-traces/";
	private static final String MSGS_FOLDER = "./test-msgs/";
	
	@Parameters  
    public static Collection<Object[]> generateParams() {  
         List<Object[]> params = new ArrayList<Object[]>();  
         for (int i = 1; i <= TEST_COUNT; i++) {  
              params.add(new Object[] {i});  
         }  
         return params;  
    }   
    
	public RandomTest(int param) {
		index = param;
	}  
	
	@Rule
    public ExternalResource externalResource = new ExternalResource() {
        protected void before() throws Throwable { 
        	System.out.println("Test Case #" + index + " started..."); 
        	Scheduler.reset();
        }
        protected void after() { 
        	System.out.println("Test Case #" + index + " ended..."); 
        	Scheduler.finish(TRACES_FOLDER + "trace-test" + index + ".txt");
        	Scheduler.logger.outputMessages(MSGS_FOLDER + "trace-test" + index + ".txt");
        	
        	// Coverage measurement
        	if(index == TEST_COUNT) {
        		File folder = new File(TRACES_FOLDER);
            	File[] listOfFiles = folder.listFiles();
            	ArrayList<String> traceFiles = new ArrayList<String>(); 
            	for(File file : listOfFiles)
            		traceFiles.add(file.getPath());
            	Criterion cov = new PRCriterion("PairOfReceives");
            	cov.measureCoverage(traceFiles);
        	}
        	
        }
    };
	
	@Test
	public void testBoundedBuffer() {
		
		final int bufferCapacity = 3;
		final int noOfPutMessages = 2;
		final int noOfGetMessages = 2;
		
		Timeout timeout = new Timeout(Duration.create(timeoutInSecs, "seconds"));
		ActorSystem system = ActorSystem.create("BoundedBuffer");
		
		Scheduler.setSystem(system);
		Scheduler.setTraceName("trace-dump" + index + ".txt");
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
