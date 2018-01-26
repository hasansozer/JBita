package util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Logger {

	private ArrayList<LoggedMessage> messages = new ArrayList<LoggedMessage>();
	
	public void logLine(String format) {
		System.out.println(format);
	}
	
	public void logMessage(String send, String recv, String msgType) {
		messages.add(new LoggedMessage(send, recv, msgType));
	}

	public void reset() {
		messages.clear();
	}

	public void outputMessages(String logFile) {
		try {
			FileWriter writer = new FileWriter(logFile, false);
			for(LoggedMessage msg: messages) {
				writer.write(msg + "\n");
			}
			writer.close();
		} catch (IOException e) {
			System.out.println("Error in writing to file " + logFile);
			e.printStackTrace();
		}
	}

}
