package util;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import scala.actors.threadpool.Arrays;

public class Messages {
	
	public static String actorCreationMessage = "CreateRandomNameChild"; // for Akka 2.0
	public static String newActorCreationMessage = "NullMessage"; // for Akka 2.1
	public static String actorStopMessage = "StopChild";

	private static final String[] HTTP_MESSAGES = new String [] 
			{"OnHeaderWriteCompleted",
		    "OnContentWriteCompleted",
		    "OnStatusReceived",
		    "OnHeadersReceived",
		    "OnBodyPartReceived",
		    "OnCompleted",
		    "OnThrowable"};
	public static Set<String> gatlingHttpHandlerMessages = Collections.synchronizedSet(new HashSet<String>(Arrays.asList(HTTP_MESSAGES))); 
			
	public static Set<String> schedulerMessages = Collections.synchronizedSet(new HashSet<String>(Arrays.asList(new String[]{"ReceiveTimeout"})));
			
	public static synchronized boolean isSchedulerMessage(Object message) {
		return schedulerMessages.contains(message.toString());
	}

	public static boolean isHttpHandlerMessage(Object message) {
		String messageStr = message.toString();
		boolean hasParameter = (messageStr.indexOf("(") >= 0);
		boolean isObject = (messageStr.indexOf("@") >= 0);
		String messageType = messageStr;
		if(hasParameter)
			messageType = messageStr.substring(0, messageStr.indexOf("("));
		else if(isObject)
			messageType = messageStr.substring(0, messageStr.indexOf("@"));
		
		return gatlingHttpHandlerMessages.contains(messageType);
	}
	
	public static boolean isCreationMessage(Object message) {
		return message.toString().contains(actorCreationMessage) || message.toString().contains(newActorCreationMessage);
	}
	
	public static boolean isStopMessage(Object message) {
		return message.toString().contains(actorStopMessage);
	}
}
