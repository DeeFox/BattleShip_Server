package main;
import java.util.HashMap;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

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
	public void onClose(Session session, CloseReason reason) {
		// In case the player was signed into the lobby, remove him
		lobby.removePlayerFromLobby(session);
	}

	@OnOpen
	public void onConnect(Session session) {
		System.out.println("Client connected: " + session);
	}

	@OnError
	public void onError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	@OnMessage
	public void onMessage(String message, Session sess) {
		Pair<JsonObject,String> parsed = parseMessage(message, sess);
		
		if(parsed == null)
			return;
		
		JsonObject json = parsed.getVar1();
		String mode 	= parsed.getVar2();
		
		HashMap<String, String> fields;
		Player challengedPlayer;
		Player challengingPlayer;
		switch (mode) {
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
		}
	}
	
	private HashMap<String, String> parseRequiredFields(Session sess, JsonObject json, String[] fieldKeys) {
		HashMap<String, String> fields = new HashMap<String, String>();
		
		for(String k : fieldKeys) {
			String field = "";
			try {
				JsonElement jelem = json.get(k);
				field = jelem.getAsString();
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
			//JsonElement jelem = gson.fromJson(message, JsonElement.class);
			//json = jelem.getAsJsonObject();
			json = gson.fromJson(message, JsonObject.class);
			JsonElement jelemMode = json.get("action");
			mode = jelemMode.getAsString();
		} catch(Exception e) {
			AnswerUtils.sendError(sess, "Malformed JSON input (" + e.getLocalizedMessage() + ")");
			e.printStackTrace();
			return null;
		}
		return new Pair<JsonObject,String>(json, mode);
	}
}
