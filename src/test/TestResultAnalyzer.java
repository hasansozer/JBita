package test;

import util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Scanner;

public class TestResultAnalyzer {
	
	private static final String UML_SEQ_DIAG_FILENAME = "umlSeqDiag";
	private static final String PLANTUML_COMMAND = "java -jar ./lib/plantuml.jar ./" + UML_SEQ_DIAG_FILENAME + ".txt";
	
	private static int testCount = 0;
	private static int failedTestIndex = -1;
	private static ArrayList<Boolean> testResults = new ArrayList<Boolean>();
	private static ArrayList<ArrayList<String>> msgSequences = new ArrayList<ArrayList<String>>();
	private static ArrayList<Pair<Integer,Integer>> traceSimilarities = new ArrayList<Pair<Integer,Integer>>();
	
	public static void analyzeMessages(String msgsFolder, boolean []tests) {
		
		testCount = tests.length;
		for(int i=0; i < testCount; i++) {
			testResults.add(tests[i]);
			if(!tests[i]) 
				failedTestIndex = i;
		}
		if(failedTestIndex < 0) {
    		System.out.println("No failed test found...");
    		return;
    	}
		
		System.out.println("Analyzing failed test traces...");
		
		readMessageSequencesFromFile(msgsFolder);
    	
    	System.out.println("Calculating similarity of traces...");
    	
    	calculateTraceSimilarity();
    	
    	if(traceSimilarities.get(0).getRight() == msgSequences.get(failedTestIndex).size()) {
    		System.out.println("Execution trace # " 
    							+ traceSimilarities.get(0).getLeft()
    							+ " is exactly the same as the failed execution trace...");
    		return;
    	}
    	
    	System.out.println("Slicing and depicting the failed execution trace...");
    	
    	generateSequenceDiagram(getSlicedMsgSequence());
    	
		displaySequenceDiagram();
	}
	
	private static void calculateTraceSimilarity() {
		ArrayList<String> failedTrace = msgSequences.get(failedTestIndex);
		
		for(int i = 0; i < testCount; i++) {
			if(i != failedTestIndex) {
				ArrayList<String> comparedTrace = msgSequences.get(i);
				if(failedTrace.size() == comparedTrace.size()) {
					int count = 0;
					for(int j = 0; j < failedTrace.size(); j++) 
						if(failedTrace.get(j).compareTo(comparedTrace.get(j))== 0)
							count++;	
					traceSimilarities.add(new Pair<Integer, Integer>(i,count));
				} else
					System.out.println("Message missing with respect to trace # " + i + "...");
			}
		}
		traceSimilarities.sort(new Comparator<Pair<Integer,Integer>>() {
				public int compare(Pair<Integer,Integer> p1, Pair<Integer,Integer> p2) {
				   return p2.getRight() - p1.getRight();
			   }
			});
		//for(Pair<Integer,Integer> pair : traceSimilarities)
		//	System.out.println(pair.getLeft() + ": " + pair.getRight());
	}

	private static void readMessageSequencesFromFile(String folderName) {
		File folder = new File(folderName);
    	File[] listOfFiles = folder.listFiles();
    	
    	if(listOfFiles.length != testCount) {
    		System.out.println("Error: The number of trace files do not match the number of tests!");
    		return;
    	}
    	
    	for(int i = 0; i < testCount; i++)
    		msgSequences.add(getMsgSequence(listOfFiles[i].getPath()));
	}

	private static ArrayList<String> getSlicedMsgSequence() {
		ArrayList<String> slicedTrace = new ArrayList<String>();
		ArrayList<String> failedTrace = msgSequences.get(failedTestIndex);
		ArrayList<String> comparedTrace = msgSequences.get(traceSimilarities.get(0).getLeft());
		
		for(int i = 0; i < failedTrace.size(); i++)
			if(comparedTrace.get(i).compareTo(failedTrace.get(i)) != 0) 
					slicedTrace.add(failedTrace.get(i));
	
		return slicedTrace;
	}
/* v1	
	private static ArrayList<String> getSlicedMsgSequence() {
		ArrayList<String> slicedTrace = new ArrayList<String>();
		
		ArrayList<String> failedTrace = msgSequences.get(failedTestIndex);
		int msgCount = failedTrace.size();
		int traceIndex = 0;
		int msgIndex = 0;
		
		while (msgIndex < msgCount && traceIndex < testCount) {
			
			while(!testResults.get(traceIndex))
				traceIndex++;
			
			if(traceIndex < testCount) {
				
				ArrayList<String> comparedTrace = msgSequences.get(traceIndex);
				
				while(msgIndex < msgCount
					&& comparedTrace.get(msgIndex).compareTo(failedTrace.get(msgIndex)) == 0)
					msgIndex++;
				
				while(msgIndex < msgCount
					&& comparedTrace.get(msgIndex).compareTo(failedTrace.get(msgIndex)) != 0) {
					slicedTrace.add(failedTrace.get(msgIndex));
					msgIndex++;
				}
			} 
			
			while(msgIndex < msgCount) {
				slicedTrace.add(failedTrace.get(msgIndex));
				msgIndex++;
			}	
		}
		return slicedTrace;
	}
*/
	private static void displaySequenceDiagram() {
		try {
			Process p = Runtime.getRuntime().exec(PLANTUML_COMMAND);
			p.waitFor();
			
			TestResultFrame view = new TestResultFrame(UML_SEQ_DIAG_FILENAME + ".png");

		} catch (Exception e) {
			System.out.println("Error while creating the sequence diagram...");
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> getMsgSequence(String filePath) {
		ArrayList<String> msgSequence = new ArrayList<String>();
		try {
			Scanner sc = new Scanner(new File(filePath));
	        while(sc.hasNextLine())
	        	msgSequence.add(sc.nextLine());
		} catch (FileNotFoundException e) {
			System.out.println("Error occured while reading message sequence from file " + filePath);
			e.printStackTrace();
		}
		return msgSequence;
	}
	
	public static void generateSequenceDiagram(ArrayList<String> msgSequence) {
		try {
			FileWriter writer = new FileWriter(UML_SEQ_DIAG_FILENAME  + ".txt", false);
			
			writer.write("@startuml\n");
			for(String event: msgSequence) {
				writer.write(event + "\n");
			}
			writer.write("@enduml\n");
			writer.close();
		} catch (IOException e) {
			System.out.println("Error in writing to file " + UML_SEQ_DIAG_FILENAME + ".txt");
			e.printStackTrace();
		}
	}
}
