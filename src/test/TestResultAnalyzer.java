package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class TestResultAnalyzer {
	
	public static final String UML_SEQ_DIAG_FILENAME = "umlSeqDiag";
	public static final String PLANTUML_COMMAND = "java -jar ./lib/plantuml.jar ./" + UML_SEQ_DIAG_FILENAME + ".txt";

	public static void analyzeMessages(String msgsFolder, boolean []testResults) {
		
		System.out.println("Analyzing failed test traces...");
		
		File folder = new File(msgsFolder);
    	File[] listOfFiles = folder.listFiles();
    	ArrayList<String> traceFiles = new ArrayList<String>(); 
    	for(File file : listOfFiles)
    		traceFiles.add(file.getPath());
    	
    	ArrayList<String> failedMsgSequence = null;
    	for(int i = 0; i < testResults.length; i++)
    		if(!testResults[i]) {
    			failedMsgSequence = getMsgSequence(traceFiles.get(i));
    			break;
    		}
    	
    	if(failedMsgSequence == null) {
    		System.out.println("No failed test found...");
    		return;
    	}
    	
    	generateUMLSequenceDiagram(failedMsgSequence);
    	
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
	
	public static void generateUMLSequenceDiagram(ArrayList<String> msgSequence) {
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
