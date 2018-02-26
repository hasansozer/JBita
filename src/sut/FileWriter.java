package sut;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.Await;
import akka.dispatch.Future;
import akka.pattern.Patterns;
import akka.util.Duration;
import akka.util.Timeout;

public class FileWriter {
	
	public static class Execute {}
	public static class CheckForError {}
	
	static class ActionDone {}
	
	static class Flush {}
	
	static class Flushed {}
	
	static class Error {}
	
	static class GetUnlflushedContent {}
			
	static class Write {
		private final String element;
		
		public Write(String element) {
			this.element = element;
		}
		
		public String getElement() {
			return element;
		}
	}

	public static class Writer extends UntypedActor {

		ArrayList<String> results = new ArrayList<String>();
		
		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof Write) {
				results.add(((Write)message).getElement());
			} else if(message instanceof Flush) {
				System.out.println(results);
				results.clear();
				getSender().tell(new Flushed(), getSelf());
			} else if(message instanceof GetUnlflushedContent) {
				getSender().tell(new String(""+results), getSelf());			
			} else {
				unhandled(message);
			}
		}
	}
	
	public static class Action extends UntypedActor {

		String name;
		ActorRef terminator;
		ActorRef writer;
		
		public Action (String name, ActorRef terminator, ActorRef writer) {
			this.name = name;
			this.terminator = terminator;
			this.writer = writer;
		}
		
		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof Execute) {
				writer.tell(new Write(name), getSelf());
				terminator.tell(new ActionDone(), getSelf());
			} else {
				unhandled(message);
			}			
		}
	}
	
	public static class Terminator extends UntypedActor {

		public boolean error;
		public boolean flushed;
		
		private int actionCount;
		private ActorRef writer;
		
		public Terminator(int actionCount, ActorRef writer) {
			flushed = false;
			error = false;
			this.actionCount = actionCount;
			this.writer = writer;
		}
		
		@Override
		public void onReceive(Object message) throws Exception {
			if(message instanceof ActionDone) {
				actionCount--;
				if(actionCount == 0)
					writer.tell(new Flush(), getSelf());
			} else if(message instanceof Flushed) {
				flushed = true;
			} else if(message instanceof Error) {
				error = true;
			} else if(message instanceof CheckForError) {
				//getSender().tell(new Result(flushed && error), getSelf());
				String result = "" + (flushed && error);
				
				Timeout timeout = new Timeout(Duration.create(20, "seconds"));
				Future<Object> future = Patterns.ask(writer, new GetUnlflushedContent(), timeout);
				try {
					String e = (String) Await.result(future, timeout.duration());				
					getSender().tell(result+e, getSelf());
				} catch (Exception e) {
					System.out.println("Action timeout waiting for terminator...");
				}
			} else {
				unhandled(message);
			}	
		}
	}
}
