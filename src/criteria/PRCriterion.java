package criteria;

import schedule.Trace;
import util.LoggedMessage;
import schedule.Event;
import schedule.LogicalActor;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PRCriterion extends Criterion {

	public PRCriterion(String name) {
		super(name);
	}
	
	@Override
	public boolean doesSatisfy(Trace t, int i, int j) {
		Event ei = t.getEvent(i);
		Event ej = t.getEvent(j);
		return ei.receiverIDStr.equals(ej.receiverIDStr);
	}

	@Override
	public int measureCoverage(ArrayList<String> traceFiles) {
		
		System.out.println("---Measuring the coverage for the " + name + "criterion...");
		System.out.println("---Total # of trace files: " + traceFiles.size());
		
	    groupAndWriteReceivesBasedOnReceiver(traceFiles);
	    
	    System.out.println("---Total # of actors: " + actorFiles.size());

	    int totalCoverageValue = 0;
	    HashMap<String,Integer> actorsCoverageValue = new HashMap<String,Integer>();
	    HashMap<Integer,Integer> coveredPairs = new HashMap<Integer,Integer>();
	    HashMap<Integer,Integer> notCoveredPairs = new HashMap<Integer,Integer>();
	    
	    try  {
	    	String line = "";
	    	Iterator<Entry<Integer, ArrayList<String>>> it 
	    	= actorFiles.entrySet().iterator();
	    	while (it.hasNext()) {
	    		Map.Entry<Integer, ArrayList<String>> pair 
	    		= (Map.Entry<Integer, ArrayList<String>>)it.next();
	    		int actor = pair.getKey();
	    		ArrayList<String> files = pair.getValue();
	    		
	    		coveredPairs.clear();
	  	      	notCoveredPairs.clear();
	  	      	
	  	      	for(String file: files) {	  	      		
	  	      		ArrayList<String> hashLines = new ArrayList<String>();
	  	      		BufferedReader br = new BufferedReader(new FileReader(file));
	  	  	        while ((line = br.readLine()) != null) {
	  	  	        	hashLines.add(line);
	  	  	        }
	  	  	        br.close();
	  	  	        for (int i= 0; i < hashLines.size(); i++) {
	  	  	        	for (int j = i+1; j < hashLines.size(); j++) {
	  	  	        		
	  	  	        		int ei = Integer.parseInt(hashLines.get(i));
	  	  	        		int ej = Integer.parseInt(hashLines.get(j));
	  	  	        		
	  	  	        		if(!(coveredPairs.containsKey(ei) && coveredPairs.get(ei) == ej)
	  	  	        		&& !(coveredPairs.containsKey(ej) && coveredPairs.get(ej) == ei)) {
	  	  	        			boolean reversePairExists = 
	  	  	        					notCoveredPairs.containsKey(ej) 
	  	  	        					&& notCoveredPairs.get(ej) == ei;
	  	  	        			
	  	  	        			if (reversePairExists) {
	  	  	        				totalCoverageValue++;
	  	  	        				coveredPairs.put(ei,ej);
	  	  	        				notCoveredPairs.remove(ej,ei);
	  	  	        			} else
	  	  	        				notCoveredPairs.put(ei,ej);
	  	  	        		}
	  	  	        	}
	  	  	        }
	  	      	}
	  	      	actorsCoverageValue.put(actorHashToName.get(actor), coveredPairs.size());
	    	}
	    	
	    	System.out.println("Total covered pairs (for all actors) = " + totalCoverageValue);
	    	System.out.println("*************************************************************");
	    	
	    } catch(Exception e) {
	    	System.out.println("Error reading/writing from/to a file for calculating coverage...");
	    	System.out.println(e);
	    }
	    return totalCoverageValue;
	}
	
	private void groupAndWriteReceivesBasedOnReceiver(ArrayList<String> traceFiles) {
		
		try {
			
			for(String traceFile: traceFiles) {
				
  	      		//System.out.println("Analyzing trace file:" + traceFile);
	  	      	
				Trace trace = Trace.parse(traceFile, true);
				ArrayList<Integer> traceHashCodes = new ArrayList<Integer>();
				
				HashMap<Integer,FileWriter> actorsToWriterMap = new HashMap<Integer, FileWriter>();
				
				for (int i = 0; i < trace.size(); i++) {
					Event event = trace.getEvent(i);
					traceHashCodes.add(event.hashCodeInTrace);
					
					Integer[] hashCodeArray = new Integer[traceHashCodes.size()];
					hashCodeArray = traceHashCodes.toArray(hashCodeArray);
					int actorHash = LogicalActor.getHashCode(event.receiverIDStr, hashCodeArray);
		
					FileWriter writer = null;
					if(actorsToWriterMap.containsKey(actorHash))
						writer = actorsToWriterMap.get(actorHash);
					else {
						String fileName = traceFile.replace("-trace.txt", "-trace-" + event.receiverIDStr + actorHash + ".txt");
						writer = new FileWriter(fileName, false);
						actorsToWriterMap.put(actorHash, writer);
						if (actorFiles.containsKey(actorHash)) {
				              actorFiles.get(actorHash).add(fileName);
				        } else {
				              ArrayList<String> fileArray = new ArrayList<String>();
				              fileArray.add(fileName);
				              actorFiles.put(actorHash, fileArray);
				              actorHashToName.put(actorHash, event.receiverIDStr);
				        }
					}
					if (!event.promiseResponse)
				          writer.write(event.hashCodeInTrace + "\n");
				}
				Iterator<Entry<Integer,FileWriter>> it = actorsToWriterMap.entrySet().iterator();
			    while (it.hasNext()) {
			        Map.Entry<Integer,FileWriter> pair = (Map.Entry<Integer,FileWriter>)it.next();
			        pair.getValue().close();
			    }
			}	
		} catch (IOException e) {
			System.out.println("Error in writing to files for keeping receive evens for actors");
			e.printStackTrace();
		}
	}

	@Override
	public boolean satisfy(Trace t, int i, int j) {
		Event ei = t.getEvent(i);
		Event ej = t.getEvent(j);
		return (ei.receiverIDStr.equals(ej.receiverIDStr)) 
				&& (!ei.promiseResponse) && (!ej.promiseResponse);			  
	}
}
