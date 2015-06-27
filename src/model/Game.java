package model;

import java.util.HashMap;
import java.util.Random;

import javax.websocket.Session;

import main.AnswerUtils;
import model.Ship.Orientation;
import model.Ship.ShipType;

import com.google.gson.JsonElement;

public class Game {
	
	private static Random rnd = new Random();
	public static boolean getRandomBoolean() {
		return rnd.nextBoolean();
	}
	
	private Player player1;
	private Player player2;
	
	private Field player1Field;
	private Field player2Field;
	
	public enum GameState {
		BOTH_PLAYERS_PLACING_SHIPS,
		PLAYER1_FINISHED_PLACING_SHIPS,
		PLAYER2_FINISHED_PLACING_SHIPS,
		PLAYER1_TURN, PLAYER2_TURN,
		GAME_OVER
	}
	
	private GameState state;

	public Game(Player p1, Player p2) {
		state = GameState.BOTH_PLAYERS_PLACING_SHIPS;
		player1 = p1;
		player2 = p2;
		
		player1Field = new Field();
		player2Field = new Field();
	}
	
	private boolean isPlayer1(Player p) {
		return (p == player1);
	}
	
	public Field getPlayerField(Player p) {
		return (isPlayer1(p)) ? player1Field : player2Field;
	}
	
	public GameState getState() {
		return state;
	}

	public void placeShip(Player player, HashMap<String, String> fields) {
		// AI: if player placed first ship, initiate the ai ship placement
		if(getOtherPlayer(player).isAIPlayer()) {
			AIPlayer ai = getOtherPlayer(player).getAI();
			if(!ai.hasPlacedShips()) {
				ai.triggerShipPlacement();
			}
		}
		
		// Check if all fields are ok
		Point coordinates = new Point();
		try {
			coordinates = Point.fromStrings(fields.get("x"), fields.get("y"));
		} catch(NumberFormatException e) {
			AnswerUtils.sendError(player.getSession(), "Malformed Coordinates.");
			return;
		}
		
		String orient = fields.get("orientation");
		if(!(orient.equals("h") || orient.equals("v"))) {
			AnswerUtils.sendError(player.getSession(), "Malformed Orientation.");
			return;
		}
		Orientation orientation = Orientation.fromString(orient);
		
		ShipType type = ShipType.fromIdent(fields.get("shiptype"));
		if(type == null) {
			AnswerUtils.sendError(player.getSession(), "Unknown ShipType.");
			return;
		}
		
		Ship ship = new Ship(type, coordinates, orientation);
		Field pf = getPlayerField(player);
		boolean success = pf.placeShip(ship);
		if(!success) {
			AnswerUtils.sendError(player.getSession(), "Illegal ship placement attempt.");
		} else {
			// Send out field to user
			
			sendPlayerFieldUpdate(player, player);
			
			// Check if all ships placed
			if(pf.allShipsPlaced()) {
				playerFinishedPlacingShips(player);
			}
		}
	}

	public boolean playerAllowedToPlaceShips(Player player) {
		if(state.equals(GameState.BOTH_PLAYERS_PLACING_SHIPS) || 
			(isPlayer1(player) && state.equals(GameState.PLAYER2_FINISHED_PLACING_SHIPS)) ||
			(!isPlayer1(player) && state.equals(GameState.PLAYER1_FINISHED_PLACING_SHIPS))) {
			return true;
		} else {
			return false;
		}
	}
	
	private void sendPlayerFieldUpdate(Player playerField, Player toPlayer) {
		Field f = (isPlayer1(playerField)) ? player1Field : player2Field;
		Session sess = toPlayer.getSession();
		
		boolean isOwner = (playerField == toPlayer);
		String fieldType = (isOwner) ? "your_field" : "opponent_field";
		
		JsonElement field = f.getFieldAsJson(isOwner);
		AnswerUtils.sendPlayerField(sess, field, fieldType);
	}
	
	public void playerFinishedPlacingShips(Player player) {
		// Send out a log messages
		String msg = "Alle Schiffe platziert.";
		AnswerUtils.sendLogMessage(player.getSession(), msg, player.getUsername());
		AnswerUtils.sendLogMessage(getOtherPlayer(player).getSession(), msg, player.getUsername());
		
		if(state.equals(GameState.BOTH_PLAYERS_PLACING_SHIPS)) {
			if(isPlayer1(player)) {
				state = GameState.PLAYER1_FINISHED_PLACING_SHIPS;
			} else {
				state = GameState.PLAYER2_FINISHED_PLACING_SHIPS;
			}
		} else {
			diceOutBeginner();
			msg = "Das Spiel beginnt!";
			AnswerUtils.sendLogMessage(player.getSession(), msg, "Server");
			AnswerUtils.sendLogMessage(player.getSession(), msg, "Server");
		}
		sendGameState(player1);
		sendGameState(player2);
	}

	private void sendGameState(Player player) {
		String msg = "";
		if( playerAllowedToAttack(player) ) {
			msg = "your_turn";
		}
		if( (isPlayer1(player) && state.equals(GameState.PLAYER2_TURN)) ||
			(!isPlayer1(player) && state.equals(GameState.PLAYER1_TURN)) ) {
			msg = "opponent_turn";
		}
		if( (isPlayer1(player) && state.equals(GameState.PLAYER1_FINISHED_PLACING_SHIPS)) ||
			(!isPlayer1(player) && state.equals(GameState.PLAYER2_FINISHED_PLACING_SHIPS)) ) {
			msg = "opponent_placing_ships";
		}
		if(!msg.equals(""))
			AnswerUtils.sendGameState(player.getSession(), msg);
	}

	private void diceOutBeginner() {
		boolean player1Begins = Game.getRandomBoolean();
		
		if(player1Begins) {
			state = GameState.PLAYER1_TURN;
		} else {
			state = GameState.PLAYER2_TURN;
			if(player2.isAIPlayer()) {
				player2.getAI().triggerAttack();
			}
		}
	}

	public boolean playerAllowedToAttack(Player player) {
		if( (isPlayer1(player) && state.equals(GameState.PLAYER1_TURN)) ||
			(!isPlayer1(player) && state.equals(GameState.PLAYER2_TURN)) ) {
			return true;
		} else {
			return false;
		}
	}

	public Ship attack(Player player, HashMap<String, String> fields) {
		Point coordinates = new Point();
		try {
			coordinates = Point.fromStrings(fields.get("x"), fields.get("y"));
		} catch(NumberFormatException e) {
			AnswerUtils.sendError(player.getSession(), "Malformed Coordinates.");
			return null;
		}
		
		Player otherPlayer = getOtherPlayer(player);
		Field pf = getPlayerField(otherPlayer);
		if(pf.alreadyFiredHere(coordinates)) {
			AnswerUtils.sendError(player.getSession(), "Already fired here.");
			return null;
		}
		Ship target = pf.fire(coordinates);
		
		sendAttackLog(coordinates, target, player);
		
		sendPlayerFieldUpdate(otherPlayer, otherPlayer);
		sendPlayerFieldUpdate(otherPlayer, player);
		
		if(pf.allShipsDestroyed()) {
			playerWon(player);
			if(player.isAIPlayer()) {
				player.getAI().triggerCongrats();
			}
		} else {
			nextPlayerTurn(player);
			sendGameState(player);
			sendGameState(otherPlayer);
			
			if(otherPlayer.isAIPlayer()) {
				otherPlayer.getAI().triggerAttack();
			}
		}
		return target;
	}
	
	private void sendAttackLog(Point p, Ship target, Player player) {
		String msg = "";
		msg = "Attacke auf " + p.toBSString() + ". ";
		if(target == null) {
			 msg += "Kein Treffer!";
		} else {
			if(target.isDestroyed()) {
				msg += "Schiff versenkt!";
			} else {
				msg += "Schiff getroffen!";
			}
		}
		AnswerUtils.sendLogMessage(player.getSession(), msg, player.getUsername());
		AnswerUtils.sendLogMessage(getOtherPlayer(player).getSession(), msg, player.getUsername());
	}

	private void playerWon(Player player) {
		state = GameState.GAME_OVER;
		
		// Log Message
		Field pf = getPlayerField(getOtherPlayer(player));
		int cnt = pf.getOpponentShotCount();
		String msg = "Spieler '" + player.getUsername() + "' gewinnt das Spiel nach " + cnt + " Zuegen.";
		AnswerUtils.sendLogMessage(player.getSession(), msg, "Server");
		AnswerUtils.sendLogMessage(getOtherPlayer(player).getSession(), msg, "Server");
		
		sendGameOver(player, "winner");
		sendGameOver(getOtherPlayer(player), "loser");
	}
	
	public void playerLeft(Player player) {
		System.out.println("!! Player left - " + player.getSession().getId());
		sendGameOver(getOtherPlayer(player), "player_left");
	}
	
	private void sendGameOver(Player player, String outcome) {
		AnswerUtils.sendGameOver(player.getSession(), outcome);
	}

	public Player getOtherPlayer(Player player) {
		return (isPlayer1(player)) ? player2 : player1;
	}
	
	private void nextPlayerTurn(Player player) {
		if(isPlayer1(player)) {
			state = GameState.PLAYER2_TURN;
		} else {
			state = GameState.PLAYER1_TURN;
		}
	}

	public void sendChatMessage(Player player, HashMap<String, String> fields) {
		Player otherPlayer = getOtherPlayer(player);
		String msg = fields.get("message");
		if(!(msg.length() > 0)) {
			return;
		}
		
		if(otherPlayer.isAIPlayer()) {
			otherPlayer.getAI().handleChatMsg(msg);
		} else {
			AnswerUtils.sendChatMessage(otherPlayer.getSession(), msg);
		}
	}
	
	public String toString() {
	    String res = "";
	    res = "Game [" + player1.getUsername() + ", " + player2.getUsername() + "] " + state.name();
	    return res;
	}
}
