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

@RunWith(Parameterized.class)
public class ActorSysTest {

	protected static final int MAX_TEST_COUNT = 3;
	protected static final String TRACES_FOLDER = "./test-traces/";
	protected static final String MSGS_FOLDER = "./test-msgs/";
	protected static boolean testPassed[] = new boolean[MAX_TEST_COUNT];
	
	protected int index;
	protected static boolean testFailed; 
	
	@Parameters  
    public static Collection<Object[]> generateParams() {  
         List<Object[]> params = new ArrayList<Object[]>();  
         for (int i = 0; i < MAX_TEST_COUNT; i++) {  
              params.add(new Object[] {i});  
         }  
         return params;  
    }   
    
	public ActorSysTest(int param) {
		index = param;
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
    	    	
    	testFailed = true;
    	testPassed[MAX_TEST_COUNT-1] = false;
    	    	
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
        }
    };
    
    private static void printTestResults() {
    	System.out.println("Test Results\n============");
    	for (int i = 0; i < MAX_TEST_COUNT; i++) {
    		System.out.println("Test #" + i + "\t\t: " + (testPassed[i]? "passed" : "failed"));
    	}
    }
    
    private static void measureCoverage() {
    	File folder = new File(TRACES_FOLDER);
    	File[] listOfFiles = folder.listFiles();
    	ArrayList<String> traceFiles = new ArrayList<String>(); 
    	for(File file : listOfFiles)
    		traceFiles.add(file.getPath());
    	Criterion cov = new PRCriterion("PairOfReceives");
    	cov.measureCoverage(traceFiles);
    }
}
