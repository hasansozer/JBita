package test;

import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.rules.TestWatcher;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import criteria.*;
import schedule.Scheduler;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@RunWith(Parameterized.class)
public class ActorSysTest {

	protected static final int TIMEOUT_SEC = 30;
	protected static final int MAX_RANDOM_TEST_COUNT = 3;
	protected static final String TRACES_FOLDER = "./test-traces/";
	protected static final String RANDOM_TRACES_FOLDER = "./rand-test-traces/";
	protected static final String MSGS_FOLDER = "./test-msgs/";
	
	protected int index;
	protected static int testCount;
	protected static boolean testPassed[]; 
	protected static boolean testFailed; 
	
	@Parameters  
    public static Collection<Object[]> generateParams() {  
    	
    	testCount = MAX_RANDOM_TEST_COUNT;
    	testPassed = new boolean[testCount];
    	
         List<Object[]> params = new ArrayList<Object[]>();  
         for (int i = 0; i < testCount; i++) {  
              params.add(new Object[] {i});  
         }  
         return params;  
    }   
    
	public ActorSysTest(int param) {
		index = param;
		createFolder(RANDOM_TRACES_FOLDER);
		createFolder(TRACES_FOLDER);
		createFolder(MSGS_FOLDER);
	}
	
	protected static void createFolder(String folderName) {
		File folder = new File(folderName);
		if(!folder.exists())
			folder.mkdir();
	}
	
	@Rule
	public TestWatcher watchman = new TestWatcher() {
	 
		@Override
	    protected void failed(Throwable e, Description description) {
			testPassed[index] = false;
			testFailed = true;
        }
	 
		@Override
		protected void succeeded(Description description) {
			testPassed[index] = true;
        }
	};
	
	@BeforeClass
    public static void setUpClass() {
		testFailed = false;
    }
 
    @AfterClass
    public static void tearDownClass() {
    	
    	printTestResults();
    	measureCoverage();
 
    	if(testFailed) 
    	    TestResultAnalyzer.analyzeMessages(MSGS_FOLDER, testPassed);
    }
	    
	@Rule
    public ExternalResource externalResource = new ExternalResource() {
		
        protected void before() throws Throwable { 
        	System.out.println("Test Case #" + index + " started..."); 
        	Scheduler.reset();
        }
        	   
        protected void after() { 
        	System.out.println("Test Case #" + index + " ended..."); 
        	Scheduler.finish(TRACES_FOLDER + "trace-test" + index + ".txt");
        	Scheduler.logger.outputMessages(MSGS_FOLDER + "trace-test" + index + ".txt");
        	copyFolder(TRACES_FOLDER, RANDOM_TRACES_FOLDER);
        }
    };
    
    protected static void printTestResults() {
    	System.out.println("Test Results\n============");
    	for (int i = 0; i < testCount; i++) {
    		System.out.println("Test #" + i + "\t\t: " + (testPassed[i]? "passed" : "failed"));
    	}
    }
    
    protected static void measureCoverage() {
    	Criterion cov = new PRCriterion("PairOfReceives");
    	cov.measureCoverage(getTraceFiles(TRACES_FOLDER));
    }
    
    protected static ArrayList<String> getTraceFiles(String folderName) {
    	File folder = new File(folderName);
    	File[] listOfFiles = folder.listFiles();
    	ArrayList<String> traceFiles = new ArrayList<String>(); 
    	for(File file : listOfFiles)
    		traceFiles.add(file.getPath());
    	return traceFiles;
    }
    
    protected static void copyFolder(String src, String dest) {
    	try {
    		File srcDir = new File(src);
			File destDir = new File(dest);
    		String files[] = srcDir.list();
    		for (String file : files) {
    			File srcFile = new File(srcDir, file);
    			File destFile = new File(destDir, file);
        		Files.copy(srcFile.toPath(), 
        				destFile.toPath(), 
        				StandardCopyOption.REPLACE_EXISTING);
    		}
    	} catch (IOException e) {
    		System.out.println("Error while copying random trace files: " + e);
    	}
    }
}
