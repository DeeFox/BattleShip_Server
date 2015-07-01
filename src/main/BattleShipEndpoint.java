package main;
import java.io.IOException;
import java.util.HashMap;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import model.Game;
import model.Lobby;
import model.Pair;
import model.Player;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


@ServerEndpoint(value = "/")
public class BattleShipEndpoint {

	private Gson gson = new Gson();
	private Lobby lobby = Lobby.getInstance();
	
	
	@OnClose
	public void onClose(Session sess, CloseReason reason) {
		System.out.println("!! Client disconnected: " + sess.getId());
		// In case the player was ingame, remove him and send gameover packets
		Pair<Player, Game> data = lobby.getGameForSession(sess);
		if(data != null) {
			Game g = data.getVar2();
			if(g != null) {
				g.playerLeft(data.getVar1());
			}
		}
		
		// In case the player was signed into the lobby, remove him
		lobby.removePlayerFromLobby(sess, true);
		
		// Stop the Pinger
		SendLaterUtils.stopPinging(sess);
		
		try {
            sess.close();
        } catch (IOException e) {
        }
	}

	@OnOpen
	public void onConnect(Session session) {
		System.out.println("!! Client connected: " + session.getId());
		SendLaterUtils.startPinging(session);
	}

	@OnError
	public void onError(Session sess, Throwable cause) {
		System.out.println("!! Socket error (" + cause.getMessage() + ")");
		// In case the player was ingame, remove him and send gameover packets
		Pair<Player, Game> data = lobby.getGameForSession(sess);
		Game g = data.getVar2();
		if(g != null) {
			g.playerLeft(data.getVar1());
		}
		
		// In case the player was signed into the lobby, remove him
		lobby.removePlayerFromLobby(sess, true);
		
		// Stop the Pinger
		SendLaterUtils.stopPinging(sess);
		
		try {
            sess.close();
        } catch (IOException e) {
        }
	}

	@OnMessage
	public void onMessage(String message, Session sess) {
		System.out.println(">> (" + sess.getId() + ") " + message);
		Pair<JsonObject,String> parsed = parseMessage(message, sess);
		
		if(parsed == null)
			return;
		
		JsonObject json = parsed.getVar1();
		String mode 	= parsed.getVar2();
		
		HashMap<String, String> fields;
		Player challengedPlayer;
		Player challengingPlayer;
		Player player;
		Pair<Player, Game> data;
		Game game;
		switch (mode) {
		// Lobby Functions
		case "lobbysignin":
			fields = parseRequiredFields(sess, json, new String[] { "username" });
			if(fields == null)
				return;
			
			Player p = new Player(fields.get("username"), sess);
			lobby.addPlayerToLobby(p);
			break;
		case "challengeplayer":
			fields = parseRequiredFields(sess, json, new String[] { "userid" });
			if(fields == null)
				return;
			
			challengedPlayer = lobby.getPlayerById(Integer.parseInt(fields.get("userid")));
			challengingPlayer = lobby.getPlayerBySession(sess);
			lobby.challengePlayer(challengingPlayer, challengedPlayer);
			break;
		case "revokechallenge":
			challengingPlayer = lobby.getPlayerBySession(sess);
			lobby.revokeChallenge(challengingPlayer);
			break;
		case "declinechallenge":
			challengedPlayer = lobby.getPlayerBySession(sess);
			lobby.declineChallenge(challengedPlayer);
			break;
		case "acceptchallenge":
			challengedPlayer = lobby.getPlayerBySession(sess);
			lobby.acceptChallenge(challengedPlayer);
			break;
		// Game Functions
		case "ship_placed":
			fields = parseRequiredFields(sess, json, new String[] { "shiptype", "x", "y", "orientation" });
			data = lobby.getGameForSession(sess);
			if(data == null || fields == null)
				return;
			
			game = data.getVar2();
			player = data.getVar1();
			if(game.playerAllowedToPlaceShips(player)) {
				game.placeShip(player, fields);
			} else {
				AnswerUtils.sendError(sess, "You are not in the right state to place ships.");
			}
			break;
		case "attack":
			fields = parseRequiredFields(sess, json, new String[] { "x", "y" });
			data = lobby.getGameForSession(sess);
			if(data == null || fields == null)
				return;
			
			game = data.getVar2();
			player = data.getVar1();
			if(game.playerAllowedToAttack(player)) {
				game.attack(player, fields);
			} else {
				AnswerUtils.sendError(sess, "You are not in the right state to perform an attack.");
			}
			break;
		case "send_chat":
			fields = parseRequiredFields(sess, json, new String[] { "message" });
			data = lobby.getGameForSession(sess);
			if(data == null || fields == null)
				return;
			
			game = data.getVar2();
			player = data.getVar1();
			
			game.sendChatMessage(player, fields);
			break;
		case "dbg":
			fields = parseRequiredFields(sess, json, new String[] { "type" });
			if(fields != null)
				debugRequested(sess, fields);
			break;
		case "priv":
			fields = parseRequiredFields(sess, json, new String[] { "userid", "msg" });
			if(fields != null) {
				try {
					Player pl = lobby.getPlayerById(Integer.parseInt(fields.get("userid")));
					AnswerUtils.sendLogMessage(pl.getSession(), fields.get("msg"), "Console");
				} catch(Exception e) {
					
				}
			}
		}
	}
	
	private void debugRequested(Session sess, HashMap<String, String> fields) {
		String type = fields.get("type");
		String resp = "";
		switch(type) {
		case "lobby":
			resp = this.lobby.lobbyDebug();
			break;
		case "pings":
		    resp = SendLaterUtils.getActivePingers();
		    break;
		}
		
		AnswerUtils.sendMessageToSession(sess, resp);
	}

	private HashMap<String, String> parseRequiredFields(Session sess, JsonObject json, String[] fieldKeys) {
		HashMap<String, String> fields = new HashMap<String, String>();
		
		for(String k : fieldKeys) {
			String field = "";
			try {
				JsonElement jelem = json.get(k);
				field = jelem.getAsString();
				if(field.length() == 0) {
					AnswerUtils.sendError(sess, "Crucial fields for this method missing!");
					return null;
				}
				fields.put(k, field);
			} catch(Exception e) {
				AnswerUtils.sendError(sess, "Crucial fields for this method missing!");
				return null;
			}
		}
		
		return fields;
	}
	
	private Pair<JsonObject,String> parseMessage(String message, Session sess) {
		JsonObject json = new JsonObject();
		String mode = "";
		try {
			json = gson.fromJson(message, JsonObject.class);
			JsonElement jelemMode = json.get("action");
			mode = jelemMode.getAsString();
		} catch(Exception e) {
			AnswerUtils.sendError(sess, "Malformed JSON input (" + e.getMessage() + ")");
			return null;
		}
		return new Pair<JsonObject,String>(json, mode);
	}
}
