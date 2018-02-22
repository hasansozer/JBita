package sut;

import java.util.ArrayList;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class FileWriter {
	
	static class Execute {}
	
	static class ActionDone {}
	
	static class Flush {}
	
	static class Flushed {}
	
	static class Error {}
	
	static class CheckForError {}
	
	static class Write {
		private final String element;
		
		public Write(String element) {
			this.element = element;
		}
		
		public String getElement() {
			return element;
		}
	}
	
	static class Result {
		private final boolean element;
		
		public Result(boolean element) {
			this.element = element;
		}
		
		public boolean getElement() {
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
				getSender().tell(new Result(flushed && error), getSelf());
			} else {
				unhandled(message);
			}	
		}
	}
}
