package main;

import java.util.HashMap;

import javax.websocket.Session;

public class SendLaterUtils extends Thread {
	
	private final int PINGDELAY = 15;
	
	private static SendLaterUtils singleton = new SendLaterUtils();
	static {
		singleton.start();
	}
	
	private HashMap<Session, Integer> pingers;
	
	public SendLaterUtils() {
		this.pingers = new HashMap<Session, Integer>();
	}
	
	public void run() {
		while(true) {
			synchronized(pingers) {
				HashMap<Session, Integer> ps = new HashMap<Session, Integer>(pingers);
				for(Session s : ps.keySet()) {
					Integer c = ps.get(s);
					c = c - 3;
					if(c <= 0) {
						AnswerUtils.sendPing(s);
						c = PINGDELAY;
					}
					pingers.put(s, c);
				}
			}
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				
			}
		}
	}
	
	public static void startPinging(Session sess) {
		singleton.addToPingList(sess);
	}

	public static void stopPinging(Session sess) {
		singleton.removeFromPingList(sess);
	}

	private void removeFromPingList(Session sess) {
		synchronized(pingers) {
			pingers.remove(sess);
		}
	}

	private void addToPingList(Session sess) {
		synchronized(pingers) {
			pingers.put(sess, PINGDELAY);
		}
	}
}
