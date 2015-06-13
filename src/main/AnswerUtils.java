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
		System.out.println("Sending msg " + msg);
		Async as = sess.getAsyncRemote();
		as.sendText(msg);
	}

	public static void sendGameStart(Session sess, String opName) {
		JsonObject answer = new JsonObject();
		answer.addProperty("action", "startgame");
		answer.addProperty("opponent_name", opName);
		
		sendMessageToSession(sess, answer.toString());
	}
}
