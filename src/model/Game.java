package model;

import java.util.HashMap;

import javax.websocket.Session;

import main.AnswerUtils;
import model.Ship.Orientation;
import model.Ship.ShipType;

import com.google.gson.JsonElement;

public class Game {
	
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
	
	private Field getPlayerField(Player p) {
		return (isPlayer1(p)) ? player1Field : player2Field;
	}
	
	public GameState getState() {
		return state;
	}

	public void placeShip(Player player, HashMap<String, String> fields) {
		// Check if all fields are ok
		Point coordinates = new Point();
		try {
			coordinates = Point.fromStrings(fields.get("x"), fields.get("y"));
		} catch(NumberFormatException e) {
			// TODO error
			return;
		}
		
		String orient = fields.get("orientation");
		if(!(orient.equals("h") || orient.equals("v"))) {
			// TODO error
			return;
		}
		Orientation orientation = Orientation.fromString(orient);
		
		ShipType type = ShipType.fromIdent(fields.get("shiptype"));
		if(type == null) {
			// TODO error
			return;
		}
		
		Ship ship = new Ship(type, coordinates, orientation);
		Field pf = getPlayerField(player);
		boolean success = pf.placeShip(ship);
		if(!success) {
			// TODO error
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
		
	}
}
