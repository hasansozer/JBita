package util;

public class ActorAnalysis {

	private static String root = null;

	public static void setRoot(String actorPath) {
		String [] elements = actorPath.split("/");
		root = elements[0] + "/" + elements[1] + "/" + elements[2];
	}
	
	public static void reset() {
		root = null;
	}
	 
	public static boolean isRootSet() {
		return (root != null);
	}
	  
	public static boolean isRoot(String actorPath) {
		return (root != null) && actorPath.endsWith(String.format("%s/", root));
	}
	
	public static boolean isUserGuardian(String actorPath) {
		return actorPath.endsWith("/user");
	} 
	
	public static String getDeadLetterActorPath() {
		return String.format("%s/deadLetters", root);
	}
	
	public static String getHttpActorPath() {
		return String.format("%s/http", root);
	}
	
	public static String getSchedulerActorPath() {
		return String.format("%s/timer", root);
	}
	
	public static String getSystemGuardianActorPath() {
		return String.format("%s/system", root);
	}
	
	public static boolean isSystemCreatedActor(String actorPath) {
		return actorPath.contains("/system") || isUserGuardian(actorPath);
	}
	
	public static boolean isUserCreatedActor(String actorPath) {
		return actorPath.contains("/user/");
	} 
	
	public static boolean isTemp(String actorPath) {
		return actorPath.contains("/temp");
	}
}
