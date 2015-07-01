package model;

import javax.websocket.Session;

public class Player {
	
	private static int idCounter = 0;
	
	private String username;
	private int id;
	private Session session;
	
	private boolean isAIPlayer = false;
	private AIPlayer ai = null;
	
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
	
	public boolean isAIPlayer() {
		return this.isAIPlayer;
	}
	
	public void setAsAIPlayer(String mode) {
		this.isAIPlayer = true;
		this.ai = new AIPlayer(this, mode);
	}
	
	public AIPlayer getAI() {
		return this.ai;
	}
	
	public String toString() {
		String sess = (this.isAIPlayer) ? "AI" + this.ai.getMode() : this.session.getId();
		return "Player " + this.username + " ID:" + this.id + " " + sess;
	}
}
