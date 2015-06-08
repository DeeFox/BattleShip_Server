package model;

import javax.websocket.Session;

public class Player {
	
	private static int idCounter = 0;
	
	private String username;
	private int id;
	private Session session;
	
	public Player(String username, Session session) {
		super();
		this.username = username;
		this.session = session;
		this.id = Player.getNextId();
	}
	
	public static int getNextId() {
		int id = idCounter;
		idCounter++;
		return id;
	}

	public String getUsername() {
		return username;
	}

	public int getId() {
		return id;
	}

	public Session getSession() {
		return session;
	}
	
	
	
}
