package main;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.Session;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class AnswerUtils {
	
	public static void sendError(Session sess, String err) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "error");
		answer.addProperty("errmsg", err);
		
		sendMessageToSession(sess, answer.toString());
	}
	
	public static void sendLobbyPlayerlist(Session sess, JsonElement playerlist, String challengeUser) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "lobbyplayerlist");
		answer.add("playerlist", playerlist);
		answer.addProperty("challengedby", challengeUser);
		
		sendMessageToSession(sess, answer.toString());
	}
	
	public static void sendMessageToSession(Session sess, String msg) {
		// ignore message if target session is null ( AI etc. )
		if(sess == null)
			return;
		
		try {
			System.out.println("<< (" + sess.getId() + ") " + msg);
			Async as = sess.getAsyncRemote();
			as.sendText(msg);
		} catch(Exception e) {
			System.out.println("!! Error sending message to " + sess.getId());
		}
	}

	public static void sendGameStart(Session sess, String opName) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "startgame");
		answer.addProperty("opponent_name", opName);
		
		sendMessageToSession(sess, answer.toString());
	}

	public static void sendPlayerField(Session sess, JsonElement field, String fieldType) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "field_update");
		answer.addProperty("type", fieldType);
		answer.add("data", field);
		
		sendMessageToSession(sess, answer.toString());
	}

	public static void sendGameState(Session sess, String state) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "gamestate");
		answer.addProperty("state", state);
		
		sendMessageToSession(sess, answer.toString());
	}

	public static void sendGameOver(Session sess, String outcome) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "gameover");
		answer.addProperty("outcome", outcome);
		
		sendMessageToSession(sess, answer.toString());
	}

	public static void sendChatMessage(Session sess, String msg) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "chatmsg");
		answer.addProperty("message", msg);
		
		sendMessageToSession(sess, answer.toString());
	}
	
	public static void sendLogMessage(Session sess, String msg, String sender) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "logmsg");
		answer.addProperty("sender", sender);
		answer.addProperty("message", msg);
		
		sendMessageToSession(sess, answer.toString());
	}
	
	public static void sendPing(Session sess) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "ping");
		
		sendMessageToSession(sess, answer.toString());
	}
}
