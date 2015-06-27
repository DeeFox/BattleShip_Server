package main;

import java.util.HashMap;

import javax.websocket.Session;

import model.Pair;

public class SendLaterUtils extends Thread {
	
	private final int PINGDELAY = 15;
	private final int SENDLATERLOOP = 1;
	
	private static SendLaterUtils singleton = new SendLaterUtils();
	static {
		singleton.start();
	}
	
	private HashMap<Session, Integer> pingers;
	private HashMap<Pair<Callable, String>, Integer> calls;
	
	public SendLaterUtils() {
		this.pingers = new HashMap<Session, Integer>();
		this.calls = new HashMap<Pair<Callable, String>, Integer>();
	}
	
	public void run() {
		while(true) {
			// Do the pingers
			synchronized(pingers) {
				HashMap<Session, Integer> ps = new HashMap<Session, Integer>(pingers);
				for(Session s : ps.keySet()) {
					Integer c = ps.get(s);
					c = c - SENDLATERLOOP;
					if(c <= 0) {
						boolean success = AnswerUtils.sendPing(s);
						if(!success)
						    pingers.remove(s);
						c = PINGDELAY;
					}
					pingers.put(s, c);
				}
			}
			
			// Do the calls
			synchronized(calls) {
				HashMap<Pair<Callable, String>, Integer> copy = new HashMap<Pair<Callable, String>, Integer>(calls);
				for(Pair<Callable, String> c : copy.keySet()) {
					Integer i = copy.get(c);
					i = i - SENDLATERLOOP;
					if(i <= 0) {
						c.getVar1().call(c.getVar2());
						calls.remove(c);
					} else {
						calls.put(c, i);
					}
					
				}
			}
			
			try {
				Thread.sleep(1000 * SENDLATERLOOP);
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
	
	public static void callLater(Callable obj, String type, int delay) {
		Pair<Callable, String> p = new Pair<Callable, String>(obj, type);
		singleton.addToCallList(p, delay);
	}

	private void addToCallList(Pair<Callable, String> p, int delay) {
		synchronized(calls) {
			calls.put(p, delay);
		}
	}
	
	private String getPingersList() {
	    String res = "";
	    synchronized(pingers) {
            res = pingers.toString();
        }
	    return res;
	}
	
	public static String getActivePingers() {
	    return singleton.getPingersList();
	}
}
