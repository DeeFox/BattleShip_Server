package main;

import java.util.HashMap;

import javax.websocket.Session;

public class SendLaterUtils implements Runnable {
	
	private static SendLaterUtils singleton = new SendLaterUtils();
	static {
		singleton.run();
	}
	
	private HashMap<Session, Integer> pingers;
	
	public SendLaterUtils() {
		this.pingers = new HashMap<Session, Integer>();
	}
	
	@Override
	public void run() {
		synchronized(pingers) {
			HashMap<Session, Integer> ps = new HashMap<Session, Integer>(pingers);
			for(Session s : ps.keySet()) {
				Integer c = ps.get(s);
				c = c - 1;
				if(c <= 0) {
					AnswerUtils.sendPing(s);
					c = 30;
				}
				pingers.put(s, c);
			}
		}
		try {
			Thread.sleep(1000);
			System.out.println("a");
		} catch (InterruptedException e) {
			
		}
	}
	
	public static void startPinging(Session sess) {
		singleton.addToPingList(sess);
	}



	private void addToPingList(Session sess) {
		synchronized(pingers) {
			pingers.put(sess, 15);
		}
	}
}
