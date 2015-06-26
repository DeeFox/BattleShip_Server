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
	private HashSet<Player> allPlayers;
	private HashMap<Player, Player> challenges;
	
	private HashMap<Player, Game> games;
	
	public static Lobby getInstance() {
		if(instance == null)
			instance = new Lobby();
		
		return instance;
	}
	
	public Lobby() {
		players = new HashSet<Player>();
		allPlayers = new HashSet<Player>();
		challenges = new HashMap<Player, Player>();
		games = new HashMap<Player, Game>();
		
		setupAIPlayers();
	}
	
	private void setupAIPlayers() {
		// Create 2 AI Players, set their AI flags
		Player AIDumb = new Player("[Bot] Graham (einfach)", null);
		Player AIHard = new Player("[Bot] Cliffe (schwer)", null);
		AIDumb.setAsAIPlayer("easy");
		AIHard.setAsAIPlayer("hard");
		// Add them to the playerlist
		players.add(AIDumb);
		players.add(AIHard);
		allPlayers.add(AIDumb);
		allPlayers.add(AIHard);
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
		for(Player p : allPlayers) {
			if(p.getSession() == sess) {
				res = p;
			}
		}
		return res;
	}
	
	public Player getPlayerById(int id) {
		Player res = null;
		for(Player p : allPlayers) {
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
		this.allPlayers.add(player);
		publishPlayerlist();
	}
	
	public void removePlayerFromLobby(Session sess, boolean disconnected) {
		
		// Remove Player from Playerlist
		Player p = getPlayerBySession(sess);
		if(p == null)
			return;
		if(p.isAIPlayer())
			return;
		players.remove(p);
		
		if(disconnected)
			allPlayers.remove(p);
		
		// Remove all challenges with that player
		HashMap<Player, Player> chals = new HashMap<Player, Player>(challenges);
		for(Player pl : chals.keySet()) {
			Player op = chals.get(pl);
			if(p == pl || p == op) {
				challenges.remove(pl);
			}
		}
		
		// Publish the updated list
		publishPlayerlist();
	}

	public void challengePlayer(Player challengingPlayer,
			Player challengedPlayer) {
		// Don't create Challenge if challenging Player already challenged
		// another player
		if(challenges.keySet().contains(challengingPlayer))
			return;
		
		// If challenged Player is AI, directly start game and dont remove AI
		// As opponent create copy of AI Player with mode
		if(challengedPlayer.isAIPlayer()) {
			Player realOp = new Player(challengedPlayer.getUsername(), null);
			realOp.setAsAIPlayer(challengedPlayer.getAI().getMode());
			Game g = startGame(challengingPlayer, realOp);
			realOp.getAI().setGame(g);
			return;
		}
		
		// Start a game when 2 players challenged each other
		Player thirdPlayer = challenges.get(challengedPlayer);
		if(thirdPlayer != null && thirdPlayer == challengingPlayer) {
			startGame(challengingPlayer, challengedPlayer);
		}
		
		// Dont create Challenge when other player is already being challenged
		if(challenges.values().contains(challengedPlayer))
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
	
	public Game startGame(Player player1, Player player2) {
		Game g = new Game(player1, player2);
		games.put(player1, g);
		games.put(player2, g);
		
		// Remove Players from Lobby Playerlist
		removePlayerFromLobby(player1.getSession(), false);
		removePlayerFromLobby(player2.getSession(), false);
		
		// Send Gamestart packets to players
		AnswerUtils.sendGameStart(player1.getSession(), player2.getUsername());
		AnswerUtils.sendGameStart(player2.getSession(), player1.getUsername());
		
		return g;
	}

	public void acceptChallenge(Player challengedPlayer) {
		Player challengingPlayer = getChallengedBy(challengedPlayer);
		if(challengingPlayer == null)
			return;
		// TODO Error
		
		startGame(challengingPlayer, challengedPlayer);
	}

	public Pair<Player, Game> getGameForSession(Session sess) {
		Player p = getPlayerBySession(sess);
		if(p == null)
			return null;
		Game g = games.get(p);
		return new Pair<Player, Game>(p, g);
	}

	public String lobbyDebug() {
		String dbg = "Players in Lobby:\n";
		dbg += this.players.toString() + "\nAll players:\n";
		dbg += this.allPlayers.toString() + "\nChallenges:\n";
		dbg += this.challenges.toString();
		return dbg;
	}
}
