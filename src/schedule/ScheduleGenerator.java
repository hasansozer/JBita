package schedule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Constants;
import util.Pair;
import criteria.*;

public class ScheduleGenerator {
	
	private Criterion criterion = new PRCriterion("PairOfReceives");
	HashSet<Pair<Integer,Integer>> coveredPairs;
	public int maxSchedule = -1;
	
	public ArrayList<String> generateSchedules(String name, 
			ArrayList<String> randomTracesPath, 
			String generatedSchedulesPath) {
		
		System.out.println("Genarting schedule...");

		ArrayList<Trace> randomTraces = new ArrayList<Trace>(); 
		for(String traceFile: randomTracesPath) {
			randomTraces.add(Trace.parse(traceFile,true));
		}
		
		System.out.println("# of random traces: " + randomTraces.size());
		
		coveredPairs = new HashSet<Pair<Integer,Integer>>();
	    
		updateCoveredPairs(randomTraces,0);
    			
		System.out.println("# of covered pairs by random traces: " + coveredPairs.size());
		
		//---------------------------------
		ArrayList<Trace> newTraces = new ArrayList<Trace>();
		int traceCount = 0;
		
		for(Trace trace: randomTraces){
			int length = trace.size();
			boolean saturated = true;
			for(int i = 0; i < length; i++) {
				for(int j = i; j < length; j++) {
					Event eventI = trace.getEvent(i);
					Event eventJ = trace.getEvent(j);
			        int ei = eventI.hashCodeInTrace;
			        int ej = eventJ.hashCodeInTrace;
			        Pair<Integer,Integer> pair = new Pair<Integer,Integer>(ei,ej);

					if(criterion.satisfy(trace, i, j) 
							&& canBeReordered(i, j, trace)
							&& !coveredPairs.contains(pair))
						{
						Trace newTrace = reorderAndGenerate(i, j, trace, i - 1, false);
		            	newTraces.add(newTrace);
		            	ArrayList<Trace> tmpList = new ArrayList<Trace>();
		            	tmpList.add(newTrace);
		            	updateCoveredPairs(tmpList, i);
		            	saturated = false;
		            	traceCount += 1;
		            	if (newTraces.size() == maxSchedule) 
		            		break;
					} // else: already covered
				}
			}
		}
		
		ArrayList<String> schedules = new ArrayList<String>();
		int index = 1;
		for(Trace newTrace : newTraces) {
			String scheduleFileName = generatedSchedulesPath 
									+ name + "-" + index + "-schedule.txt";
			newTrace.outputTrace(scheduleFileName);
			schedules.add(scheduleFileName);
			index++;
		}
		
		return schedules;
	}
	
	private Trace reorderAndGenerate(int i, int j, Trace originalTrace, int prefixIndex, boolean keepTail) {
		
		Trace generatedTrace = new Trace();
		HashMap<Integer,Integer> indexMap = new HashMap<Integer,Integer>();
		copyPrefix(originalTrace, generatedTrace, prefixIndex);
		ArrayList<Integer> noCausallyRelatedIJ = new ArrayList<Integer>();
		
		HashSet<Integer> mustHappenBeforeI = getMustHappenBeforeEvents(i, originalTrace, prefixIndex + 1);
		HashSet<Integer> mustHappenBeforeJ = getMustHappenBeforeEvents(j, originalTrace, prefixIndex + 1);
			
		for(int k = prefixIndex + 1; k < i; k++) {
			if (mustHappenBeforeI.contains(k) 
				|| mustHappenBeforeJ.contains(k)) {
		        Event event = originalTrace.getEvent(k);
		        addChangedEvent(event, generatedTrace, indexMap);
		    } else
		        noCausallyRelatedIJ.add(k);
		}
		
		for (int k = i + 1; k < j; k++) {
			if ((mustHappenBeforeJ.contains(k) 
				&& !isCausallyRelated(i, k, originalTrace))) {
		        Event event = (Event)originalTrace.getEvent(k);
		        addChangedEvent(event, generatedTrace, indexMap);
		    } else if (!isCausallyRelated(i, k, originalTrace))
		        noCausallyRelatedIJ.add(k);
		}
		
		Event eventI = originalTrace.getEvent(i);
		Event eventJ = originalTrace.getEvent(j);
		addChangedEvent(eventJ, generatedTrace, indexMap);
		generatedTrace.setRelaxedIndex(generatedTrace.size());
		addChangedEvent(eventI, generatedTrace, indexMap);

		if (keepTail)
			copyTail(originalTrace, generatedTrace, i, j, noCausallyRelatedIJ, indexMap);

		updateCreatorIndex(indexMap, generatedTrace);
	
		return generatedTrace;
	}

	private void updateCreatorIndex(HashMap<Integer, Integer> indexMap, Trace trace) {
		ArrayList<Event> updatedGeneratedTrace = new ArrayList<Event>();
		ArrayList<Event> unUpdatedGeneratedTrace = (ArrayList<Event>) trace.getTrace();
		for(Event event : unUpdatedGeneratedTrace) {
			EventID msgCreatorID = ((LogicalMessage)event.message).creatorID;
			EventID newMsgCreatorID = new EventID(msgCreatorID.creatorIndex, msgCreatorID.seqNum);
			if(indexMap.containsKey(msgCreatorID.creatorIndex))
				newMsgCreatorID = new EventID(indexMap.get(msgCreatorID.creatorIndex), msgCreatorID.seqNum);
			
			LogicalActor receiverActor = LogicalActor.parse(event.receiverIDStr);
			String newReceiverStr = event.receiverIDStr;
			if(indexMap.containsKey(receiverActor.creatorID.creatorIndex)) {
				int newIndex = indexMap.get(receiverActor.creatorID.creatorIndex);
				receiverActor.creatorID = new EventID(newIndex, receiverActor.creatorID.seqNum);
				newReceiverStr = receiverActor.toString();
			}
			
			LogicalActor senderActor = LogicalActor.parse(event.senderIDStr);
			String newSenderStr = event.senderIDStr;
			if (indexMap.containsKey(senderActor.creatorID.creatorIndex)) {
				int newIndex = indexMap.get(senderActor.creatorID.creatorIndex);
				senderActor.creatorID = new EventID(newIndex, senderActor.creatorID.seqNum);
				newSenderStr = senderActor.toString();
			}
			
			updatedGeneratedTrace.add(event.cloneEvent(newMsgCreatorID, newSenderStr, newReceiverStr));
		}
		
	    unUpdatedGeneratedTrace.clear();
	    unUpdatedGeneratedTrace.addAll(updatedGeneratedTrace);
	}

	private void copyTail(Trace originalTrace, Trace generatedTrace, int i, int j,
			ArrayList<Integer> noCausallyRelated, HashMap<Integer, Integer> indexMap) {
		for(int k : noCausallyRelated) {
			Event event = originalTrace.getEvent(k);
			addChangedEvent(event, generatedTrace, indexMap);
		}
		for(int k = j + 1; j < originalTrace.size();  k++) {
			Event event = originalTrace.getEvent(k);
			if(!event.receiverIDStr.contains(Constants.GUARDIAN)
				&& !event.senderIDStr.contains(Constants.SYSTEM_GUARDIAN)
				&& !isCausallyRelated(i, k, originalTrace)
				&& !isCausallyRelated(j, k, originalTrace))
				addChangedEvent(event, generatedTrace, indexMap);
		}
	}

	private HashSet<Integer> getMustHappenBeforeEvents(int eventIndex, Trace trace, int startIndex) {
		HashSet<Integer> mustHappenBeforeEvents = new HashSet<Integer>();
		int eventCreatorIndex = trace.getEvent(eventIndex).getCreatorID().creatorIndex;
	
		//Causally related to event at <code>eventIndex</code> 
	    for (int i = startIndex; i < eventIndex; i++) {
	      if (isCausallyRelated(i, eventIndex, trace)) {
	        mustHappenBeforeEvents.add(i);
	      }
	    }
	    
	    // Preserving the FIFO constraints
	    for (int i = startIndex; i < eventIndex; i++) {
	      if (haveFIFOConstraint(i, eventIndex, trace)) {
	        int creatorIndex = trace.getEvent(i).getCreatorID().creatorIndex;
	        if (creatorIndex == eventCreatorIndex 
	        	|| (creatorIndex < startIndex 
	        		&& creatorIndex < eventCreatorIndex)) {
	          mustHappenBeforeEvents.add(i);
	        }
	      }
	    }
	    
	    for(int i = startIndex; i < eventIndex; i++) {
	    	
	    	Event event = trace.getEvent(i);
	    	
	    	//Checking for casually related to current events added because of FIFO constraints
	        boolean causuallyRelatedToCurrentEvents = false;
	        for(int fifoIndex : mustHappenBeforeEvents) {
	        	if(isCausallyRelated(i, fifoIndex, trace)) {
	        		causuallyRelatedToCurrentEvents = true;
	        		break;
	        	}
	        }	
	        
	        if ( causuallyRelatedToCurrentEvents)
	            mustHappenBeforeEvents.add(i);
	        else if(event.promiseResponse) {
	        	//Synchronous messaging constraints
	        	int promiseCount = 1;
	        	int creatorIndex = ((LogicalMessage)event.message).creatorID.creatorIndex;
	        	Event creatorEvent = trace.getEvent(creatorIndex);
	        	
	        	// find the last asynchronous receive event that causes the message sent synchronously
	            while (promiseCount > 0 && creatorIndex >= startIndex) {
	              if (creatorEvent.promiseResponse) 
	            	  promiseCount++;
	              else 
	            	  promiseCount--;
	              creatorIndex = ((LogicalMessage)creatorEvent.message).creatorID.creatorIndex;
	              creatorEvent = trace.getEvent(creatorIndex);
	            }
	            
	            // synchronous messaging is a part of the added events
	            if (mustHappenBeforeEvents.contains(creatorIndex)) { 
	              for (int k = startIndex; k < i; k++) {
	                if (isCausallyRelated(k, i, trace))
	                  mustHappenBeforeEvents.add(k);
	              }
	              mustHappenBeforeEvents.add(i);
	            }
	        }
	    }
		return mustHappenBeforeEvents;
	}
	
	private void addChangedEvent(Event event, Trace generatedTrace, HashMap<Integer, Integer> indexMap) {
		Event clonedEvent = (Event)event.clone();
		int newIndex = generatedTrace.addEventAndUpdateIndex(clonedEvent); 
		if (event.index != newIndex)
			indexMap.put(event.index, newIndex);
	}

	private void copyPrefix(Trace originalTrace, Trace generatedTrace, int prefixIndex) {
		for (int i = 0; i < prefixIndex; i++) {
			Event event = (Event)originalTrace.getEvent(i).clone();
		    generatedTrace.addEventAndUpdateIndex(event);
		}
	}

	private int updateCoveredPairs(ArrayList<Trace> traces, int startIndex) {
	    int lastUsefulIndex = 0;
	    for (Trace trace : traces) {
	      int length = trace.size();
	      for (int i = startIndex; i < length; i++) {
	        for (int j = i + 1; j < length; j++) {
	          Event eventI = trace.getEvent(i);
	          Event eventJ = trace.getEvent(j);
	          int ei = eventI.hashCodeInTrace;
	          int ej = eventJ.hashCodeInTrace;
	          Pair<Integer,Integer> pair = new Pair<Integer,Integer>(ei,ej);
	          
	          if (criterion.satisfy(trace, i, j)
	        		  && canBeReordered(i, j, trace)
	        		  && !coveredPairs.contains(pair))
	            {
	            coveredPairs.add(pair);
	            lastUsefulIndex = j;
	          }
	        }
	      }
	    }

	    return lastUsefulIndex;
	}

	private boolean haveOtherReorderingConstraints(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);
		return (haveFIFOConstraint(i, j, trace) 
				|| eventI.promiseResponse || eventJ.promiseResponse);
	}

	private boolean haveFIFOConstraint(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);

		String receiverI = eventI.receiverIDStr;
		String receiverJ = eventJ.receiverIDStr;
		String senderI = eventI.senderIDStr;
		String senderJ = eventJ.senderIDStr;

		return ((senderI.equals(senderJ) && receiverI.equals(receiverJ)));
	}	
	
	private boolean isCausallyRelated(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);

		LogicalMessage logicalMessageJ = (LogicalMessage)eventJ.message;
		int creatorIndex = logicalMessageJ.creatorID.creatorIndex;

		if (creatorIndex == i)
			return true;
		else if (i == j 
			|| VectorClock.greaterThanEq(logicalMessageJ.vc, eventI.vc)) 
			return true;

		return false;
	}
	
	private boolean canBeReordered(int i, int j, Trace t) {
		return !isCausallyRelated(i, j, t) 
				&& !haveOtherReorderingConstraints(i, j, t);
	}
}
