package test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.runners.Parameterized.Parameters;

import criteria.Criterion;
import criteria.PRCriterion;
import schedule.ScheduleGenerator;

public class ActorSysGenTest extends ActorSysTest{

	protected static final String GEN_TRACES_FOLDER = "./gen-test-traces/";
	protected static final String GEN_MSGS_FOLDER = "./gen-test-msgs/";
	
	public static ScheduleGenerator generator;
	public static ArrayList<String> schedules;
	
	public ActorSysGenTest(int param) {
		super(param);
	} 
	
	protected static void generateSchedules() {
		createFolder(GEN_TRACES_FOLDER);
		createFolder(GEN_MSGS_FOLDER);		
    	ArrayList<String> traceFiles = getTraceFiles(RANDOM_TRACES_FOLDER); 
    	if(traceFiles.isEmpty()) 
    		Assert.fail("No trace files found. First, perform random testing to obtain a set of trace files...");    	
    	generator = new ScheduleGenerator();
    	schedules = generator.generateSchedules("prschedule", traceFiles, GEN_TRACES_FOLDER);
    	System.out.println("Number of generated schedules: " + schedules.size());
	}
	
	@Parameters  
    public static Collection<Object[]> generateParams() {  
    	
    	generateSchedules();
    	
    	testCount = schedules.size();
    	testPassed = new boolean[testCount];
    	
        List<Object[]> params = new ArrayList<Object[]>();  
        for (int i = 0; i < testCount; i++) {  
             params.add(new Object[] {i});  
        }  
        return params;  
    }  
    
    protected static void measureCoverage() {
    	Criterion cov = new PRCriterion("PairOfReceives");
    	ArrayList<String> traceFiles = getTraceFiles(RANDOM_TRACES_FOLDER);
    	traceFiles.addAll(schedules);
    	cov.measureCoverage(traceFiles);
    }
}
