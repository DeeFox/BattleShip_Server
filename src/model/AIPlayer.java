package model;

import java.util.HashMap;
import java.util.Random;

import main.AnswerUtils;
import main.Callable;
import main.SendLaterUtils;
import model.Game.GameState;
import model.Ship.Orientation;
import model.Ship.ShipType;

public class AIPlayer implements Callable {

	private static Random rnd = new Random();
	
	private String mode = "easy";
	private boolean hasPlacedShips = false;
	private Game game;
	private Player me;
	
	public AIPlayer(Player me, String mode) {
		this.mode = mode;
		this.me = me;
	}

	public boolean hasPlacedShips() {
		return this.hasPlacedShips;
	}

	public void triggerShipPlacement() {
		SendLaterUtils.callLater(this, "place", 3);
	}
	
	public void placeShips() {
		Field aiField = this.game.getPlayerField(this.me);
		
        // Try to place all ships randomly
		HashMap<ShipType, Integer> tries = new HashMap<ShipType, Integer>();
        for (ShipType t : ShipType.values()) {
            boolean placed = false;
            int i = 0;
            while (!placed) {
                Orientation o = (rnd.nextBoolean()) ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                Point pos = new Point(rnd.nextInt(10), rnd.nextInt(10));
                Ship s = new Ship(t, pos, o);
                boolean occupied = aiField.isSpaceOccupied(s);
                if (!occupied) {
                    aiField.placeShip(s);
                    placed = true;
                }
                i++;
            }
            tries.put(t, i);
        }
        System.out.println("## " + tries.toString());
        this.hasPlacedShips = true;
        this.game.playerFinishedPlacingShips(this.me);
	}

	@Override
	public void call(String type) {
		switch(type) {
		case "place":
			placeShips();
			break;
		}
	}
	
	public String getMode() {
		return this.mode;
	}

	public void handleChatMsg(String msg) {
		if(msg.equals("id")) {
			String id = this.me.getUsername() + " - " + this.me.getId();
			AnswerUtils.sendChatMessage(this.game.getOtherPlayer(this.me).getSession(), id);
		}
	}
	
	public void setGame(Game g) {
		this.game = g;
	}
}
