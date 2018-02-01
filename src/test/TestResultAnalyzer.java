package test;

import java.io.File;
import java.util.ArrayList;

public class TestResultAnalyzer {

	public static void analyzeMessages(String msgsFolder, boolean []testResults) {
		
		System.out.println("Analyzing failed test trace...");
		
		File folder = new File(msgsFolder);
    	File[] listOfFiles = folder.listFiles();
    	ArrayList<String> traceFiles = new ArrayList<String>(); 
    	for(File file : listOfFiles)
    		traceFiles.add(file.getPath());
	}
}
