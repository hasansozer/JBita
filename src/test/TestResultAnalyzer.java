package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class TestResultAnalyzer {
	
	private static final String UML_SEQ_DIAG_FILENAME = "umlSeqDiag";
	private static final String PLANTUML_COMMAND = "java -jar ./lib/plantuml.jar ./" + UML_SEQ_DIAG_FILENAME + ".txt";
	
	private static ArrayList<String> traceFiles = new ArrayList<String>();
	private static ArrayList<String> failedMsgSequence = null;
	
	public static void analyzeMessages(String msgsFolder, boolean []testResults) {
		
		System.out.println("Analyzing failed test traces...");
		
		readMessageSequencesFromFile(msgsFolder, testResults);
    	if(failedMsgSequence == null) {
    		System.out.println("No failed test found...");
    		return;
    	}
    	
    	generateSequenceDiagram(getSlicedMsgSequence(testResults));
		displaySequenceDiagram();
	}
	
	private static void readMessageSequencesFromFile(String folderName, boolean []results) {
		File folder = new File(folderName);
    	File[] listOfFiles = folder.listFiles();
    	
    	if(listOfFiles.length != results.length) {
    		System.out.println("Error: The number of trace files do not match the number of tests!");
    		return;
    	}
    	
    	for(int i = 0; i < listOfFiles.length; i++) {
    		traceFiles.add(listOfFiles[i].getPath());
    		if(!results[i]) 
    			failedMsgSequence = getMsgSequence(traceFiles.get(i));
    	}
	}
	
	private static ArrayList<String> getSlicedMsgSequence(boolean []results) {
		ArrayList<String> slicedTrace = new ArrayList<String>();
		
		int traceIndex = 0;
		int msgIndex = 0;
		
		while (msgIndex < failedMsgSequence.size() && traceIndex < traceFiles.size()) {
			
			while(!results[traceIndex])
				traceIndex++;
			
			if(traceIndex < traceFiles.size()) {
				
				ArrayList<String> comparedTrace = getMsgSequence(traceFiles.get(traceIndex));
				
				while(msgIndex < failedMsgSequence.size()
					&& comparedTrace.get(msgIndex).compareTo(failedMsgSequence.get(msgIndex)) == 0)
					msgIndex++;
				
				while(msgIndex < failedMsgSequence.size()
					&& comparedTrace.get(msgIndex).compareTo(failedMsgSequence.get(msgIndex)) != 0) {
					slicedTrace.add(failedMsgSequence.get(msgIndex));
					msgIndex++;
				}
			} 
			
			while(msgIndex < failedMsgSequence.size()) {
				slicedTrace.add(failedMsgSequence.get(msgIndex));
				msgIndex++;
			}	
		}
		
		return slicedTrace;
	}

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
