package util;

/*
 * 
 * Hasan Sozer (hasan.sozer@ozyegin.edu.tr)
 * 
 * This class is used for keeping logged messages and obtaining their String representations
 * so that the output can be directly used by PlantUML for automatically drawing 
 * UML sequence diagrams
 * 
 * (c) 2018
 * 
 */

public class LoggedMessage {

	private String sender;
	private String receiver;
	private String msgType;
	
	public LoggedMessage(String sender, String receiver, String msgType) {
		this.sender = sender;
		this.receiver = receiver;
		this.msgType = msgType;
	}
	
	@Override
	public String toString() {
		String snd = "\"" + sender.substring(sender.lastIndexOf("/")+1,sender.length()) + "\""; 
		String recv = "\"" + receiver.substring(receiver.lastIndexOf("/")+1,receiver.length()) + "\"";
		String msg = "\"" + msgType.replaceAll("class ", "") + "\"";
		
		return snd	+ " -> " +  recv + ": " + msg;
	}

}
