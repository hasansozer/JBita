package criteria;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import schedule.Event;
import schedule.LogicalMessage;
import schedule.Trace;
import schedule.VectorClock;

public class TPA {
	
	public static final String R_BIN_FOLDER = "C:\\Program Files\\R\\R-3.5.0\\bin\\";
	public static final String SCRIPT_PATH = "C:\\Users\\hasans\\Documents\\GitHub\\JBita\\lib\\";
	public static final String TPA_SCRIPT = "tpa.r";
	
	private static Path scriptPath = null;
	private static String posetMatrix = null;
	
	public static void main(String args[]) {
		/* 1,0,1,0
		 * 0,1,0,1
		 * 0,0,1,0
		 * 0,0,0,1	
		boolean [][] depmatrix = new boolean[4][4];
		depmatrix[0][0] = true;
		depmatrix[0][2] = true;
		depmatrix[1][1] = true;
		depmatrix[1][3] = true;
		depmatrix[2][2] = true;
		depmatrix[3][3] = true;		
		*/
		boolean [][] depmatrix = getDependencyMatrix("./rand-test-traces/trace-test0.txt");
		int count = 0;
		for(int i=0; i < depmatrix.length; i++)
			for(int j=0; j < depmatrix.length; j++)
				if(depmatrix[i][j])
					count++;
		System.out.println("# of dependencies: " + count);
		setup(depmatrix);
		System.out.println(getBruteForceCount());
		System.out.println(getApproximation(0.1, 0.1)); 
	}
	
	public static boolean[][] getDependencyMatrix(String traceFile) {
		Trace t = Trace.parse(traceFile,true);
		int length = t.size();
		boolean[][] matrix = new boolean[length][length];
		
		for(int i=0; i < length; i++)
			for(int j=0; j < length; j++)
				matrix[i][j] = (i != j) && !canBeReordered(i, j, t);
		
		return matrix;
	}
	
	public static void setup(boolean dependencies[][]) {
		scriptPath = FileSystems.getDefault().getPath(SCRIPT_PATH, TPA_SCRIPT);
		posetMatrix = getPosetMatrix(dependencies);
	}
	
	public static int getBruteForceCount() {
		String command = posetMatrix
					+ "print(brute.force.count.linear.extensions(poset))\n";
		String output = runScriptandRemove(createScript(command));
		return Integer.parseInt(output.substring(output.indexOf(" ")+1));
	}
	
	public static double getApproximation(double epsilon, double delta) {
		String command = posetMatrix
				+ "print(tpa.approximation(poset,"
				+ epsilon + "," + delta + "))\n";
		String output = runScriptandRemove(createScript(command));
		return Double.parseDouble(output.substring(output.indexOf(" ")+1));
	}
	
	private static String getPosetMatrix(boolean dependencies[][]) {
		String result = "poset <- matrix(c(";
		
		for (int i = 0; i < dependencies.length; i++)
			for (int j = 0; j < dependencies.length; j++)
				result += dependencies[i][j] ? "1," : "0," ;
		 
		result = result.substring(0,result.length()-1)
				+ "),byrow=TRUE,ncol=" 
				+ dependencies[0].length 
				+ ")\n";
		
		return result;
	}
	
	private static Path createScript(String command) {
		Path tmpScriptPath = FileSystems.getDefault().getPath(SCRIPT_PATH, "tmp.r");
		try {
			Files.copy(scriptPath, tmpScriptPath);
			Files.write(tmpScriptPath, command.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tmpScriptPath;
	}

	private static String runScriptandRemove(Path tmpScriptPath) {
		String output = "";
		try {
			Process process = new ProcessBuilder(R_BIN_FOLDER + "Rscript.exe",
					tmpScriptPath.toString()).start();

			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			
			while ((line = br.readLine()) != null) {
				output = line;
			}
			Files.delete(tmpScriptPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return output;
	}
	
	private static boolean haveOtherReorderingConstraints(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);
		return (haveFIFOConstraint(i, j, trace) 
				|| eventI.promiseResponse || eventJ.promiseResponse);
	}
	
	private static boolean haveFIFOConstraint(int i, int j, Trace trace) {
		Event eventI = trace.getEvent(i);
		Event eventJ = trace.getEvent(j);

		String receiverI = eventI.receiverIDStr;
		String receiverJ = eventJ.receiverIDStr;
		String senderI = eventI.senderIDStr;
		String senderJ = eventJ.senderIDStr;

		return ((senderI.equals(senderJ) && receiverI.equals(receiverJ)));
	}	
	
	private static boolean isCausallyRelated(int i, int j, Trace trace) {
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
	
	private static boolean canBeReordered(int i, int j, Trace t) {
		return !isCausallyRelated(i, j, t) 
				&& !haveOtherReorderingConstraints(i, j, t);
	}
}
