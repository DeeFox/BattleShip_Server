package model;

import java.util.HashMap;
import java.util.HashSet;

import javax.websocket.Session;

import main.AnswerUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class Lobby {
	
	private static Lobby instance;
	
	private HashSet<Player> players;
	private HashMap<Player, Player> challenges;
	
	public static Lobby getInstance() {
		if(instance == null)
			instance = new Lobby();
		
		return instance;
	}
	
	public Lobby() {
		players = new HashSet<Player>();
		challenges = new HashMap<Player, Player>();
	}
	
	private JsonElement getPlayerlistAsJson(Player player) {
		JsonArray list = new JsonArray();
		for(Player p : players) {
			if(!p.equals(player)) {
				JsonObject playerjson = new JsonObject();
				playerjson.addProperty("username", p.getUsername());
				playerjson.addProperty("userid", p.getId());
				playerjson.addProperty("status", getChallengeStatus(p, player));
				list.add(playerjson);
			}
		}
		
		return list;
	}
	
	private String getChallengeStatus(Player forPlayer, Player refPlayer) {
		String status = "available";
		for(Player p : challenges.keySet()) {
			Player op = challenges.get(p);
			if(p.equals(refPlayer) && op.equals(forPlayer)) {
				status = "waitingforanswer";
			} else if(op.equals(forPlayer) || p.equals(refPlayer)) {
				status = "notavailable";
			}
		}
		return status;
	}
	
	private Player getChallengedBy(Player player) {
		Player res = null;
		for(Player p : challenges.keySet()) {
			Player op = challenges.get(p);
			if(player.equals(op)) {
				res = p;
			}
		}
		return res;
	}
	
	public Player getPlayerBySession(Session sess) {
		Player res = null;
		for(Player p : players) {
			if(p.getSession() == sess) {
				res = p;
			}
		}
		return res;
	}
	
	public Player getPlayerById(int id) {
		Player res = null;
		for(Player p : players) {
			if(p.getId() == id) {
				res = p;
			}
		}
		return res;
	}
	
	public void publishPlayerlist() {
		for(Player p : players) {
			JsonElement playerlistJson = getPlayerlistAsJson(p);
			Player challengedBy = getChallengedBy(p);
			String challenge = (challengedBy == null) ? "" : challengedBy.getUsername();
			AnswerUtils.sendLobbyPlayerlist(p.getSession(), playerlistJson, challenge);
		}
	}
	
	public void addPlayerToLobby(Player player) {
		this.players.add(player);
		publishPlayerlist();
	}
	
	public void removePlayerFromLobby(Session sess) {
		Player p = getPlayerBySession(sess);
		if(p == null)
			return;

		players.remove(p);
		publishPlayerlist();
	}

	public void challengePlayer(Player challengingPlayer,
			Player challengedPlayer) {
		// Start a game when 2 players challenged each other
		Player thirdPlayer = challenges.get(challengedPlayer);
		if(thirdPlayer != null && thirdPlayer == challengingPlayer) {
			startGame(challengingPlayer, challengedPlayer);
		}
		
		// Dont create Challenge when Players arent available
		if(challenges.keySet().contains(challengingPlayer) ||
			challenges.values().contains(challengedPlayer))
			return;
		
		challenges.put(challengingPlayer, challengedPlayer);
		publishPlayerlist();
	}

	public void revokeChallenge(Player challengingPlayer) {
		if(challenges.keySet().contains(challengingPlayer)) {
			challenges.remove(challengingPlayer);
			publishPlayerlist();
		}
	}

	public void declineChallenge(Player challengedPlayer) {
		for(Player p : challenges.keySet()) {
			Player op = challenges.get(p);
			if(challengedPlayer.equals(op)) {
				challenges.remove(p);
				publishPlayerlist();
			}
		}
	}
	
	public void startGame(Player player1, Player player2) {
		
	}

	public void acceptChallenge(Player challengedPlayer) {
		Player challengingPlayer = getChallengedBy(challengedPlayer);
		if(challengingPlayer == null)
			return;
		// TODO Error
		
		startGame(challengingPlayer, challengedPlayer);
	}
}
