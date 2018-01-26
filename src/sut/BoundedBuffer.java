package sut;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.dispatch.Await;
import akka.dispatch.Future;
import akka.pattern.Patterns;
import akka.util.Duration;
import akka.util.Timeout;

public class BoundedBuffer {

	static class Put {
		private final int element;
		
		public Put(int element) {
			this.element = element;
		}
		
		public int getElement() {
			return element;
		}
	}
	
	static class Get {
	}
	
	static class Element {
		private final int value;
		
		public Element(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
	
	public static class Produce {
	}
	
	public static class Consume {
	}

	public static class Buffer extends UntypedActor {
		private int content[];
		private int head, tail, size, capacity;
	
		public Buffer(final int capacity) {
			this.capacity = capacity;
			this.head = this.tail = this.size = 0;
			this.content = new int[capacity];
		}
		
		public void onReceive(Object message) {
			
			if(message instanceof Put) {
				if(size < capacity) {
					content[tail] = ((Put)message).getElement();
					tail = (tail + 1) % capacity;
					size++;				
				}
			} else if(message instanceof Get) {
				int value = -1;
				if(size > 0) {
					value = content[head];
					head = (head + 1) % capacity;
					size--;					
				} 
			
				getSender().tell(new Element(value), getSelf());
			} else {
				unhandled(message);
			}
		}
	}	
	
	public static class Producer extends UntypedActor {
		
		private final ActorRef buffer;
		public int counter;
		
		public Producer(ActorRef buffer) {
			this.buffer = buffer;
			this.counter = 0;
		}
	
		public void onReceive(Object message) {
			if(message instanceof Produce) {			
				buffer.tell(new Put(counter), getSelf());
				counter++;
				if(counter == Integer.MAX_VALUE)
					counter = 0;
			} else {
				unhandled(message);
			}
		}
	}
	
	public static class Consumer extends UntypedActor {
		
		private final ActorRef buffer;
		
		public Consumer(ActorRef buffer) {
			this.buffer = buffer;
		}
		
		public void onReceive(Object message) {
			if(message instanceof Consume) {					
				Timeout timeout = new Timeout(Duration.create(20, "seconds"));
				Future<Object> future = Patterns.ask(buffer, new Get(), timeout);
				try {
					Element e = (Element) Await.result(future, timeout.duration());				
					getSender().tell(new String(""+e.getValue()), getSelf());
				} catch (Exception e) {
					System.out.println("Consumer timeout waiting for buffer...");
				}
			} else {
				unhandled(message);
			}
		}
	}
}
