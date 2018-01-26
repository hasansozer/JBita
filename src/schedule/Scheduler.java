package schedule;

import akka.actor.Actor;
import akka.actor.ActorPath;
import akka.actor.ActorCell;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.ScalaActorRef;
import akka.dispatch.Envelope;
import akka.pattern.PromiseActorRef;

import util.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import schedule.envelopes.*;

public class Scheduler {
	
	public static Logger logger = new Logger();
	
	private static Trace trace = new Trace();
	private static ConcurrentHashMap<String, Integer> currentReceives = new ConcurrentHashMap<String, Integer>();
	private static ConcurrentHashMap<String, LogicalActor> actorPathToLogicalIDMap = new ConcurrentHashMap<String, LogicalActor>();
	private static ConcurrentHashMap<ActorRef, String> promiseToParentMap = new ConcurrentHashMap<ActorRef, String>();
	private static Set<HeldEnvelope> heldEnvelopes = Collections.synchronizedSet(new HashSet<HeldEnvelope>());	
	private static Set<Pair<EventID, String>> scheduleEvelopesHashSet = null;
	private static LinkedList<ScheduleEnvelope> schedule = null;
	private static SystemVCManager vcManager = new SystemVCManager();	
	
	private static CountDownLatch emptyScheduleLatch = new CountDownLatch(0);
	private static boolean lockTimeoutSend = false;
	
	private static int relaxedPoint = -1;
	private static int relaxedPointIndex = -1;
	private static int isIdleCount = 0;
	
	private static ActorSystem system = null;
	
	private static String traceFileName = "";
	
	public static synchronized void setTraceName(String traceName) {
		traceFileName = traceName;
	}
	
	public static synchronized void setSchedule(String scheduleFile) {
		//TODO: Need to be implemented for enforcing a schedule
		scheduleEvelopesHashSet = Collections.synchronizedSet(new HashSet<Pair<EventID, String>>());	
	}
	
	public static synchronized void setSystem(ActorSystem actorSystem) {
		system = actorSystem;
		sendTimeoutIfNeeded();
	}
	
	public static synchronized void addStopEvent(ActorRef stoppedRef, ActorRef parent) {
		String parentPath = parent.path().toString();
		String stoppedPath = stoppedRef.path().toString();
		
		if(ActorAnalysis.isRoot(stoppedPath)) {
			if(ActorAnalysis.isUserGuardian(parentPath))
				parentPath = ActorAnalysis.getDeadLetterActorPath();
				Event e = trace.getEvent(currentReceives.get(parentPath));
				e.stop.add(actorPathToLogicalIDMap.get(stoppedPath).toString());
				logger.logLine(String.format("actor %s stops actor %s", parent, stoppedRef));
		}
	}

	public static synchronized void startActor(ActorCell actorCell, Actor actorObject) {
		logger.logLine("starts the actor " + actorObject);
	    String actorPath = actorCell.self().path().toString();
	    String actorObjectClass = actorObject.getClass().toString().replace("class ", "");
	    if (actorPathToLogicalIDMap.containsKey(actorPath))
	        actorPathToLogicalIDMap.get(actorPath).objectType = actorObjectClass;
	    else
	        actorPathToLogicalIDMap.put(actorPath, new LogicalActor(actorObjectClass));
	    checkForDispatch();
	}

	public static synchronized void addCreationEvent(ActorRef child, ActorRef parent) {
		setRootActor(child);
		if (parent == null) 
			logger.logLine("null parent ref");
		updateStatusForActorCreation(child.path().toString(), 
									parent.path().toString());
	}

	private static void setRootActor(ActorRef actor) {
		if (actor != null && !ActorAnalysis.isRootSet()) {
			ActorAnalysis.setRoot(actor.path().toString());
		    initForMockActors();
		}
	}

	private static void initForMockActors() {
		actorPathToLogicalIDMap.put(ActorAnalysis.getDeadLetterActorPath(), LogicalActor.DeadLetterID);
		actorPathToLogicalIDMap.put(ActorAnalysis.getHttpActorPath(), LogicalActor.GatlingHTTPHandlerID);
		actorPathToLogicalIDMap.put(ActorAnalysis.getSchedulerActorPath(), LogicalActor.SchedulerID);
		currentReceives.put(ActorAnalysis.getDeadLetterActorPath(), Event.rootEvent.index);
	    currentReceives.put(ActorAnalysis.getHttpActorPath(), Event.httpEvent.index);
	    currentReceives.put(ActorAnalysis.getSchedulerActorPath(), Event.schedulerEvent.index);
	}
	
	private static void updateStatusForActorCreation(String childPath, String parentPath) {

		if(!actorPathToLogicalIDMap.containsKey(childPath)
				|| actorPathToLogicalIDMap.get(childPath).getCreatorID().isEqualTo(new EventID(-1,-1))) {
			
			logger.logLine(String.format("&&&&&&&& %s creates %s", parentPath, childPath));
			VectorClock newVC = vcManager.updateVectorClockForCreate(parentPath, childPath);
			
			int creatorIndex = currentReceives.get(ActorAnalysis.getDeadLetterActorPath()); 
			if(currentReceives.containsKey(parentPath)) {
				creatorIndex = currentReceives.get(parentPath);
			}
			logger.logLine("&&&&&&&& creator Index = " + creatorIndex + currentReceives);
			
			Event e = trace.getEvent(creatorIndex);
			e.create.add(childPath);
			e.increaseClock(null);
			if(e.receiverIDStr.contains("HttpRequestAction")) {
				vcManager.updateVectorClockForReceive(ActorAnalysis.getHttpActorPath(), newVC);
			}
			
			if(!actorPathToLogicalIDMap.containsKey(childPath)) {
				actorPathToLogicalIDMap.put(childPath, new LogicalActor(e.getID(null)));
			} else {
				actorPathToLogicalIDMap.get(childPath).creatorID = e.getID(null);
			}
		}
	}

	public static synchronized void setCMH(ActorRef actor) {

		int index = currentReceives.get(actor.path().toString());
		Event e = trace.getEvent(index);
		
		if(e.promiseResponse) {
			
			boolean stop = false;
			String recIDStr = e.receiverIDStr;
			for(int i = index -1; i >= 0 && !stop; i--) {
				Event te = trace.getEvent(i);
				if(te.receiverIDStr.compareTo(recIDStr) == 0 && !te.promiseResponse) {
					te.cmh = true;
					stop = true;
				}
			}
		} else
			e.cmh = true;
	}

	public static synchronized boolean checkForDispatch() {
		 for(HeldEnvelope heldEnvelope : heldEnvelopes) {
			 if(heldEnvelope != null && canSend(heldEnvelope)) {
				 logger.logLine("Check for disapthcing: " + heldEnvelope);
				 heldEnvelopes.remove(heldEnvelope);
				 deliverEnvelope(heldEnvelope);
				 logger.logLine(heldEnvelope.toString() + "**** delivered");
				 return true;
			 }
		 }
		 sendTimeoutIfNeeded();
		 return false;
	}

	private static void deliverEnvelope(HeldEnvelope heldEnvelope) {
		logger.logLine("delivering envelope..." + heldEnvelope);
		if (heldEnvelope.envelope != null)
			heldEnvelope.receiver.tell(heldEnvelope.envelope.message(), 
										heldEnvelope.envelope.sender());
		else
			heldEnvelope.receiver.tell(heldEnvelope.promiseEnvelope.message, 
					heldEnvelope.promiseEnvelope.sender);
		logger.logLine("delivered envelope..." + heldEnvelope);
	}

	private static boolean canSend(HeldEnvelope heldEnvelope) {
		LogicalMessage logicalMessage = heldEnvelope.promiseEnvelope.message;
		if(heldEnvelope.envelope != null)
			logicalMessage = (LogicalMessage) heldEnvelope.envelope.message();
		String receiverPath = getActorPath(heldEnvelope.receiver, null);
		return canSend(logicalMessage, receiverPath, false);
	}
	
	private static boolean canSend(LogicalMessage logicalMessage, String receiver, boolean isPromise) {

		if (schedule == null || schedule.size() == 0) 
			return true;
		
		int creatorIndex = logicalMessage.creatorID.creatorIndex;
		EventID messageCreatorID = new EventID(trace.getScheduleIndex(creatorIndex), logicalMessage.creatorID.seqNum);
		String messageReceiverIDStr = LogicalActor.DeadLetterID.toString();		
		if(actorPathToLogicalIDMap.get(receiver) != null)
			messageReceiverIDStr = actorPathToLogicalIDMap.get(receiver).toString();		
		LogicalActor receiverActor = LogicalActor.parse(messageReceiverIDStr);
		EventID adjustedCreatorID = new EventID(trace.getScheduleIndex(receiverActor.creatorID.creatorIndex), receiverActor.creatorID.seqNum);
		receiverActor.creatorID = adjustedCreatorID;
		messageReceiverIDStr = receiverActor.toString();
		
		if (Messages.isSchedulerMessage(schedule.peek().logicalMessage.message)) {
			Object message = logicalMessage.message;
			boolean canSendTimeout = (Messages.isSchedulerMessage(message) 
					&& schedule.peek().receiver.equals(messageReceiverIDStr)
			        && !lockTimeoutSend);
			if (canSendTimeout)
				lockTimeoutSend = true; // do not send timeout unil this one is received
			return canSendTimeout;
		}
		
		if (schedule.peek().logicalMessage.creatorID == messageCreatorID 
				&& schedule.peek().receiver.equals(messageReceiverIDStr))
		      return true;
		
		// TODO: scheduleEvelopesHashSet.contains(messageCreatorID) should be changed:only index and receiver
	    if (isPromise && schedule.size() <= relaxedPoint 
	    		&& !scheduleEvelopesHashSet.contains(new Pair<EventID, String>(messageCreatorID, messageReceiverIDStr))
	    		&& creatorIndex >= relaxedPointIndex)
	      return true;
	    else {
	      logger.logLine("(" + logicalMessage.message + "," + messageReceiverIDStr + "," + messageCreatorID + ")" + "," + "(" + schedule.peek().receiver + "," + schedule.peek().logicalMessage.creatorID + ")" + " cannot go");

	      return false;
	    }
	}

	public static synchronized String getActorPath(ActorRef actorRef, Object message) {
		if (actorRef == null 
			|| actorRef.path().toString().equals(ActorAnalysis.getDeadLetterActorPath())) {
			if (message != null) {
				if (Messages.isHttpHandlerMessage(message))
			          return ActorAnalysis.getHttpActorPath();
			    if (Messages.isSchedulerMessage(message))
			          return ActorAnalysis.getSchedulerActorPath();
			}
			return ActorAnalysis.getDeadLetterActorPath();
		}
		
		if ((actorRef instanceof PromiseActorRef) 
				&& promiseToParentMap.contains(actorRef))
			return promiseToParentMap.get(actorRef);
		else
		    return actorRef.path().toString();
	}

	private static void sendTimeoutIfNeeded() {
		if(schedule != null && schedule.size() > 0 
			&& Messages.isSchedulerMessage(schedule.peek().logicalMessage.message)) {
			if(system != null) {
				String receiverPath = null;
				for (ConcurrentHashMap.Entry<String, LogicalActor> entry : actorPathToLogicalIDMap.entrySet()) {
					if (entry.getValue().toString().equals(schedule.peek().receiver)) {
						receiverPath = entry.getKey();
						break;
					}
				}
				if (receiverPath != null) {
					logger.logLine("Sending Timeout to " + system.actorFor(receiverPath) + " " + receiverPath);
					system.actorFor(receiverPath).tell(akka.actor.ReceiveTimeout.getInstance());
			    } else {
			    	logger.logLine("ERRORR! The receiver of time out does not exits in the system");
			    }
			} else
				logger.logLine("The schedule needs timeout and the system is null");
		}
	}

	public static synchronized Envelope aroundInvoke(Envelope envelope, ActorCell actorCell) {

	    Envelope realEnvelope = envelope;
	    if (envelope.message() instanceof LogicalMessage) {
	    	LogicalMessage logicalMessage = (LogicalMessage) envelope.message();

	    	String senderActor = getActorPath(envelope.sender(), logicalMessage.message);

	    	updateVCAndScheduleAndTraceForReceive(logicalMessage, senderActor, actorCell.self().path().toString(), false);
	    	realEnvelope = new Envelope(((LogicalMessage)envelope.message()).message, envelope.sender(), actorCell.system());
	    } else {
	    	logger.logLine(String.format("m=%s,  sender=%s, rec=%s",envelope.message(), envelope.sender(), actorCell.self()) + "-- out of trace ");
	    }
   	    return realEnvelope;
	}

	public static synchronized Envelope aroundSend(Envelope envelope, ActorCell rec) {

	    checkForError(envelope.message().toString());
	    setRootActor(rec.self());
	    
	    if ((isIdleCount < 2) && !Messages.isCreationMessage(envelope.message()) && !Messages.isStopMessage(envelope.message())) {
	    	
	    	ActorSystem system = rec.system();
	    	Envelope newEnvelope = envelope;
	    	Object realMessage = envelope.message();
	    	LogicalMessage logicalMessage = null;
	    	String senderActor = envelope.sender().path().toString();
	    	String receiverActor = rec.self().path().toString();
	    	
	    	boolean isPromise = (envelope.sender() != null) && (envelope.sender() instanceof PromiseActorRef);
	    	
	    	//Wrap the message with the logical ID
	        if (envelope.message() instanceof LogicalMessage) {
	          logicalMessage = (LogicalMessage) envelope.message();
	          realMessage = logicalMessage.message;
	        } else {
	          senderActor = getActorPath(envelope.sender(), envelope.message());
	          logicalMessage = createLogicalMessageForSend(envelope.message(), senderActor, receiverActor);
	          newEnvelope = new Envelope(logicalMessage, envelope.sender(), system);
	        }
	    	
	    	if (canSend(logicalMessage, receiverActor, isPromise)) 
	    		return newEnvelope;
	    	else {
	    		if (!Messages.isSchedulerMessage(realMessage))
	    		    heldEnvelopes.add(new HeldEnvelope(rec.self(), newEnvelope));
	    	    return null;
	        }
	    } else
	    	return envelope;
	}

	public static synchronized LogicalMessage createLogicalMessageForSend(Object message, 
			ActorRef sender, 
			ScalaActorRef receiver) {
	    String senderPath = getActorPath(sender, message);
	    String receiverPath = getActorPath((ActorRef)receiver, null);
	    LogicalMessage lmsg = createLogicalMessageForSend(message, senderPath, receiverPath);
	    return lmsg;
	  }
	
	private static LogicalMessage createLogicalMessageForSend(Object message, 
			String senderPath,
			String receiverPath) {
		int index = getMessageCreatorIndex(senderPath, receiverPath);
		Event creator = trace.getEvent(index);
		creator.increaseClock(receiverPath);
		VectorClock vc = vcManager.updateVectorClockForSend(senderPath);
		return new LogicalMessage(message, creator.getID(receiverPath), vc);
	}

	private static int getMessageCreatorIndex(String msgSenderPath, String receiverPath) {
		
		if (msgSenderPath.equals(ActorAnalysis.getHttpActorPath())) {
			for (int i = trace.size() - 1; i >= 0; i--) {
				Event event = trace.getEvent(i);
		        if (event.create.contains(receiverPath)) {
		          return i;
		        }
			}
			//logger.logLine("ERROR! Could not find the event with HttpActor creator received by" + receiverPath);
		}

		if (currentReceives.contains(msgSenderPath)) {
			return currentReceives.get(msgSenderPath);
		} else if (ActorAnalysis.isUserCreatedActor(msgSenderPath)) {

			ActorPath parentPath = system.actorFor(msgSenderPath).path().parent();
			String parentPathString = parentPath.toString();

			while (!ActorAnalysis.isUserGuardian(parentPathString)) {
				// If the message is sent in preStart method of the actor.
				if (currentReceives.contains(parentPathString)) {
					logger.logLine("calling add create event form pre start ... ");
					updateStatusForActorCreation(msgSenderPath, parentPathString);
					EventID senderCreatorID = ((LogicalActor)actorPathToLogicalIDMap.get(msgSenderPath)).creatorID;
					assert(senderCreatorID != new EventID(-1, -1));
					return senderCreatorID.creatorIndex;
				}
				parentPath = parentPath.parent();
				parentPathString = parentPath.toString();				
			}
			//logger.logLine("ERROR! Could not find the event with parent creator " + msgSenderPath);
		    return -1;
		}/*else {
			logger.logLine("ERROR! Could not find the event with sender " + msgSenderPath);
			System.out.println("\n" + currentReceives.toString() + "\n");
		}*/
		return -1;
	}

	private static void updateVCAndScheduleAndTraceForReceive(LogicalMessage logicalMessage, 
			String senderActorPath, String receiverActorPath, boolean isPromiseResponse) {

	    if (isIdleCount < 2) {
	        if (logicalMessage.message.toString().startsWith("IsIdle"))
	        	isIdleCount++;
	        
	        VectorClock newVC = vcManager.updateVectorClockForReceive(receiverActorPath, logicalMessage.vc);

	        String senderActorIDStr = LogicalActor.DeadLetterID.toString();
	        if (actorPathToLogicalIDMap.get(senderActorPath) != null)
	            actorPathToLogicalIDMap.get(senderActorPath).toString();

	        String receiverActorIDStr = LogicalActor.DeadLetterID.toString();
	        if (actorPathToLogicalIDMap.get(receiverActorPath) != null)
	            actorPathToLogicalIDMap.get(receiverActorPath).toString();

	        Pair<Boolean, Boolean> inSchedule_cmh = removeFromHeadOfSchedule(logicalMessage, receiverActorIDStr);
	        boolean outOfSchedule = !inSchedule_cmh.getLeft();

	        //If the message is a timeout message, 
	        //update the sequence number for that receiver in scheduler event.

	        if (senderActorPath.equals(ActorAnalysis.getSchedulerActorPath())) {
	          Event creatorEvent = trace.getEvent(logicalMessage.creatorID.creatorIndex);
	          creatorEvent.increaseClock(receiverActorPath);
	          logicalMessage.creatorID = creatorEvent.getID(receiverActorPath);
	        }

	        int newIndex = trace.addEvent(logicalMessage, senderActorIDStr, receiverActorIDStr, isPromiseResponse, outOfSchedule, newVC, inSchedule_cmh.getRight());
	        currentReceives.put(receiverActorPath, newIndex);
	   }
	   logger.logMessage(senderActorPath, receiverActorPath, logicalMessage.message.getClass().toString());
	   logger.logLine(String.format("m=%s, id=%s, sender=%s, rec=%s", logicalMessage.message, logicalMessage.creatorID, senderActorPath, receiverActorPath)
	        + "--trace " + (trace.size() - 1));
	}
	
	private static Pair<Boolean, Boolean> removeFromHeadOfSchedule(LogicalMessage logicalMessage, String receiverIDStr) {

		Pair<EventID, String> matchIDInscheduleEvelopesHashSet = null;
		boolean cmh = false;
		
		if(schedule != null && schedule.size() > 0) {
			
			boolean matchWithHead = false;
		    int messageCreatorIndex = logicalMessage.creatorID.creatorIndex;
		    EventID messageCreatorID = new EventID(trace.getScheduleIndex(messageCreatorIndex), logicalMessage.creatorID.seqNum);
		    
		    LogicalActor receiverActor = LogicalActor.parse(receiverIDStr);
		    EventID adjustedCreatorID = new EventID(trace.getScheduleIndex(receiverActor.creatorID.creatorIndex), receiverActor.creatorID.seqNum);
		    receiverActor.creatorID = adjustedCreatorID;
		    String messageReceiverIDStr = receiverActor.toString();
		    
		    EventID scheduleHeadCreatorID = schedule.peek().logicalMessage.creatorID;
		    String scheduleHeadReceiverIDStr = schedule.peek().receiver;
		    Object scheduleHeadMessage = schedule.peek().logicalMessage.message;
		    
		    // Check for timeout messages first
		    if (Messages.isSchedulerMessage(scheduleHeadMessage)) {
		        logger.logLine(scheduleHeadReceiverIDStr + " " + messageReceiverIDStr);
		        matchWithHead = (scheduleHeadReceiverIDStr.equals(messageReceiverIDStr) && Messages.isSchedulerMessage(logicalMessage.message));
		        matchIDInscheduleEvelopesHashSet = new Pair<EventID, String>(scheduleHeadCreatorID, scheduleHeadReceiverIDStr);
		        if (matchWithHead) {
		          lockTimeoutSend = false;
		          logger.logLine(logicalMessage.message + ", " + messageReceiverIDStr + " " + scheduleHeadMessage + ", " + scheduleHeadReceiverIDStr + "=== removed from head");
		        } else
		          logger.logLine(logicalMessage.message + ", " + messageReceiverIDStr + " " + scheduleHeadMessage + ", " + scheduleHeadReceiverIDStr + "=== not match with head");
		    } else {
		        if (scheduleHeadCreatorID == messageCreatorID && scheduleHeadReceiverIDStr.equals(messageReceiverIDStr)) {
		          logger.logLine(messageCreatorID + ", " + messageReceiverIDStr + " " + scheduleHeadCreatorID + ", " + scheduleHeadReceiverIDStr + "=== removed from head");
		          matchIDInscheduleEvelopesHashSet = new Pair<EventID, String>(scheduleHeadCreatorID, scheduleHeadReceiverIDStr);
		          matchWithHead = true;
		        }
		    }
		    
		    if (matchWithHead) {
		    	ScheduleEnvelope en = schedule.remove(0);
		        cmh = en.cmh;
		        assert(matchIDInscheduleEvelopesHashSet != null);
		        scheduleEvelopesHashSet.remove(matchIDInscheduleEvelopesHashSet);
		        if (schedule.size() == 0)
		        	emptyScheduleLatch.countDown();
		        return new Pair<Boolean, Boolean>(true, cmh);
		    } else {
		        logger.logLine(messageCreatorID + " " + scheduleHeadCreatorID + " do not remove from head");
		        return new Pair<Boolean, Boolean>(false, cmh);
		    }
		} 
		emptyScheduleLatch.countDown();
		return new Pair<Boolean, Boolean>(true, cmh);
	}
		
	public static synchronized boolean isRandom(ScalaActorRef receiver, Object message, ActorRef sender) {
		return (heldEnvelopes.size() == 0) && (schedule == null || schedule.size() == 0);
	}

	public static Object aroundSendToPromise(PromiseActorRef promiseActorRef, Object message, ActorRef sender) {
		
	    logger.logLine("***** Send to promise " + message + " " + sender + " ");
	    
	    if ((isIdleCount < 2) 
	    		&& (sender == null || !ActorAnalysis.isSystemCreatedActor(sender.path().toString()))) {

	      String senderActorPath = getActorPath(sender, message);
	      String receiverActorPath = getActorPath(promiseActorRef, null);

	      // If the message is logical message, it means it has been removed from held messages.
	      // Therefore, there is no need to check it with the schedule.
	      if (message instanceof LogicalMessage) {
	    	  LogicalMessage logicalMessage = (LogicalMessage) message;
	    	  updateVCAndScheduleAndTraceForReceive(logicalMessage, senderActorPath, receiverActorPath, true);
	    	  return (Object) logicalMessage.message;
	      } else {
	          //If the message is not logical message, it needs to check the order with schedule 
	          LogicalMessage logicalMessage = createLogicalMessageForSend(message, senderActorPath, receiverActorPath);
	          PromiseEnvelope promiseEnvelope = new PromiseEnvelope(logicalMessage, sender);

	          // Check the order with the schedule, if the message can be sent, return the message;
	          // otherwise return null
	          if (canSend(logicalMessage, receiverActorPath, true)) {
	            updateVCAndScheduleAndTraceForReceive(logicalMessage, senderActorPath, receiverActorPath, true);
	            return message;
	          } else {
	            heldEnvelopes.add(new HeldEnvelope(promiseActorRef, promiseEnvelope));
	            return null;
	          }
	       }
	    } else {
	      // In the case of random scheduling, the message might be a logical message;
	      // however, in other cases it is not
	      if (message instanceof LogicalMessage)
	          return (Object) ((LogicalMessage) message).message;
	      else 
	    	  return message;
	    }
	}
	
	private static void checkForError(String message) {
		if (message.contains("Error(java.lang.IllegalArgumentException:")
		    || message.contains("doesn't support message")) {
			System.out.println("***************************** BUG DETECTED *******************" + message);
			System.out.println("Stop scheduling..... ");
			reportError(message);
		}
	}
	
	private static synchronized void reportError(String error) {
		String bug = "error";
		if (error.contains("doesn't support message")) {
			if (error.contains("Terminator"))
				bug = "HTBug";
			else {
				if(Constants.HWBug)
					bug = "HWBug";
				else
					bug = "HW2Bug";
			}
		}
		String traceFile = traceFileName.replace("-trace.txt", String.format("-%s-trace.txt", bug));
		finish(traceFile);
		System.exit(1);
	}

	public static synchronized void finish(String traceFile) {
		trace.outputTrace(traceFile);
	}
	
	public static synchronized void reset() {
	    heldEnvelopes.clear();
	    schedule = null;
	    relaxedPoint = -1;
	    relaxedPointIndex = -1;
	    currentReceives.clear();
	    promiseToParentMap.clear();
	    actorPathToLogicalIDMap.clear();
	    vcManager.resetClocks();
	    trace.reset();
	    logger.reset();
	    emptyScheduleLatch = new CountDownLatch(0);
	    system = null;
	    isIdleCount = 0;
	    ActorAnalysis.reset();
	  }
}
