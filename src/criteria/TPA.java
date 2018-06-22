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
	
	public static void main(String args[]) {
	
		Path scriptPath = FileSystems.getDefault().getPath(SCRIPT_PATH, TPA_SCRIPT);
		
		String command = "poset1 <- matrix(c(1,0,1,0,0,1,0,1,0,0,1,0,0,0,0,1),byrow=TRUE,ncol=4)\n";
		command += "print(brute.force.count.linear.extensions(poset1))\n";
		command += "print(tpa.approximation(poset1,1,0.1))\n";
		
		runScriptandRemove(createScript(scriptPath, command));
	}
	
	private static Path createScript(Path scriptPath, String command) {
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
