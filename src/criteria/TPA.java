package criteria;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TPA {
	
	public static final String R_BIN_FOLDER = "C:\\Program Files\\R\\R-3.5.0\\bin\\";
	public static final String SCRIPT_PATH = "C:\\Users\\hasans\\Documents\\GitHub\\JBita\\lib\\";
	public static final String TPA_SCRIPT = "tpa.r";
	
	private static Path scriptPath = null;
	private static String posetMatrix = null;
	
	public static void main(String args[]) {
		
		/*
		1,0,1,0
		0,1,0,1
		0,0,1,0
		0,0,0,1
		 */
		boolean [][] depmatrix = new boolean[4][4];
		depmatrix[0][0] = true;
		depmatrix[0][2] = true;
		depmatrix[1][1] = true;
		depmatrix[1][3] = true;
		depmatrix[2][2] = true;
		depmatrix[3][3] = true;		
		
		setup(depmatrix);
		getBruteForceCount();
		getApproximation(1.0, 0.1); 
	}
	
	public static void setup(boolean dependencies[][]) {
		scriptPath = FileSystems.getDefault().getPath(SCRIPT_PATH, TPA_SCRIPT);
		posetMatrix = getPosetMatrix(dependencies);
	}
	
	public static void getBruteForceCount() {
		String command = posetMatrix
					+ "print(brute.force.count.linear.extensions(poset))\n";
		runScriptandRemove(createScript(command));
	}
	
	public static void getApproximation(double epsilon, double delta) {
		String command = posetMatrix
				+ "print(tpa.approximation(poset,"
				+ epsilon + "," + delta + "))\n";
		runScriptandRemove(createScript(command));
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

	private static void runScriptandRemove(Path tmpScriptPath) {
		try {
			Process process = new ProcessBuilder(R_BIN_FOLDER + "Rscript.exe",
					tmpScriptPath.toString()).start();

			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;

			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
			Files.delete(tmpScriptPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
