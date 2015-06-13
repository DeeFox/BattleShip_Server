package model;

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
	}
	
	
}
