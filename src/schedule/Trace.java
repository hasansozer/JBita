package schedule;

import util.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class Trace {
	
	private List<Event> completeTrace = Collections.synchronizedList(new ArrayList<Event>());
	private Set<Integer> outOfScheduleIndexes = Collections.synchronizedSet(new HashSet<Integer>());
	private ConcurrentHashMap<String, Integer> timeoutSeqMap = new ConcurrentHashMap<String, Integer>();
	private HashSet<Event> mockActorsEvents = new HashSet<Event>();
	
	private int isIdleCount = 0;
	private int relaxedIndex = -1;
	
	public Trace() {
		
	}
	
	public synchronized void setRelaxedIndex(int index) {
		relaxedIndex = index;
	}
	
	public synchronized int getRelaxedIndex() {
		return relaxedIndex;
	}
	
	public synchronized int size() {
		return completeTrace.size();
	}

	public Event getEvent(Integer index) {
		if(index < 0) {
			if(mockActorsEvents.size() == 0)
				initializeMockActorsEvents();
			for(Event event: mockActorsEvents) {
				if(event.index == index)
					return event;
			}
		}
		if(index >= 0 && index < completeTrace.size())
			return completeTrace.get(index);
		else
			return null;
	}
	
	private void initializeMockActorsEvents() {
	    mockActorsEvents.add(Event.rootEvent);
	    mockActorsEvents.add(Event.schedulerEvent);
	    mockActorsEvents.add(Event.httpEvent);
	  }

	public int getScheduleIndex(int traceIndex) {
		int scheduleIndex = traceIndex;
		
		if (outOfScheduleIndexes.contains(traceIndex))
		      return -2;
		
		for(int changedIndex : outOfScheduleIndexes) {
			if(traceIndex >= changedIndex)
				scheduleIndex -= 1;
		}
		return scheduleIndex;
	}

	public void outputTrace(String traceFile) {
		try {
			FileWriter writer = new FileWriter(traceFile, false);
			
			for(Event event: completeTrace) {
				writer.write(event.toString() + "\n");
			}
			
			writer.close();
		} catch (IOException e) {
			System.out.println("Error in writing to file " + traceFile);
			e.printStackTrace();
		}
		System.out.println("The output is written in " + traceFile);
	}

	public int addEvent(Object message, String sender, String receiver,
			boolean isPromiseResponse, boolean outOfSchedule, VectorClock vc, Boolean cmh) {
	    LogicalMessage logicalMessage = (LogicalMessage) message;

	    if (logicalMessage.message.toString().startsWith("IsIdle"))
	    	isIdleCount++;

	    if (Messages.isSchedulerMessage(logicalMessage.message)) {
	    	int newSeq = 1;
	    	if (timeoutSeqMap.contains(receiver)) 
	    		newSeq = timeoutSeqMap.get(receiver) + 1;
	    	      
	    	timeoutSeqMap.put(receiver, newSeq);

	    	EventID creatorID = logicalMessage.creatorID;
	    	logicalMessage.creatorID = new EventID(creatorID.creatorIndex, newSeq);
	    }

	    Event event = new Event(completeTrace.size(), 
	    						logicalMessage, 
	    						sender, 
	    						receiver, 
	    						vc, 
	    						isPromiseResponse, 
	    						cmh);
	    int index = addEvent(event);
	    if (outOfSchedule)
	    	outOfScheduleIndexes.add(index);
	    
	    return index;
	}

	private int addEvent(Event event) {
		completeTrace.add(event);
		return event.index;
	}

	public void reset() {
		completeTrace.clear();
	    outOfScheduleIndexes.clear();
	    relaxedIndex = -1;
	    mockActorsEvents.clear();
		isIdleCount = 0;
		timeoutSeqMap.clear();
	}
	
	public static Trace parse(String traceFile, boolean fillHash) {
		Trace parsedTrace = new Trace();
		ArrayList<Integer> hashCodes = new ArrayList<Integer>();

		try (BufferedReader br = new BufferedReader(new FileReader(traceFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				Event event = Event.parse(line);
				if (event != null) {
					if (fillHash) {
						Integer hashCodesArray[] = new Integer[hashCodes.size()];
						hashCodesArray = hashCodes.toArray(hashCodesArray);
						int eventHash = Event.getHashCode(line, hashCodesArray);
						hashCodes.add(eventHash);
						event.hashCodeInTrace = eventHash;
					}
					parsedTrace.addEvent(event);
				} else
					parsedTrace.setRelaxedIndex(parsedTrace.getTrace().size());
			}
		} catch (IOException e) {
			System.out.println("Error reading from file " + traceFile);
			e.printStackTrace();
		}

		return parsedTrace;
	}

	public List<Event> getTrace() {
		return completeTrace;
	}

	public Integer[] writeHashCodes(String traceFile, String hashFilePath) {
		ArrayList<Integer> hashCodes = new ArrayList<Integer>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(traceFile))) {
			
			FileWriter writer = new FileWriter(traceFile, false);
			int eventHash = -1;
			String line;
			while ((line = br.readLine()) != null) {
				Integer hashCodesArray[] = new Integer[hashCodes.size()];
				hashCodesArray = hashCodes.toArray(hashCodesArray);
				eventHash = Event.getHashCode(line, hashCodesArray);
				hashCodes.add(eventHash);
				writer.write(eventHash + "\n");
			}
			
			writer.close();
		} catch (IOException e) {
			System.out.println("Error reading/writing from/to file " + traceFile + " -> " + hashFilePath);
			e.printStackTrace();
		}
		
		Integer hashCodesArray[] = new Integer[hashCodes.size()];
		return hashCodes.toArray(hashCodesArray);
	}
}
