package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import main.AnswerUtils;
import main.Callable;
import main.SendLaterUtils;
import model.Ship.Orientation;
import model.Ship.ShipType;

public class AIPlayer implements Callable {

	private static Random rnd = new Random();
	
	// AI vars
    private String mode = "search";
    private int destroyDirX;
    private int destroyDirY;
    private Point lastHitCoord;
    private Point lastCoord;
    private Point firstHitCoord;
    private boolean wasLastHit = false;
    private boolean[][] tries;
    private boolean[][] hits;
    private boolean[][] fires;
	
	private String difficulty = "easy";
	private boolean hasPlacedShips = false;
	private Game game;
	private Player me;
	
	public AIPlayer(Player me, String mode) {
		this.difficulty = mode;
		this.me = me;
		
		this.tries = new boolean[10][10];
        this.hits = new boolean[10][10];
        this.fires = new boolean[10][10];
	}

	public boolean hasPlacedShips() {
		return this.hasPlacedShips;
	}

	public void triggerShipPlacement() {
		this.hasPlacedShips = true;
		SendLaterUtils.callLater(this, "place", 3);
	}
	
	public void triggerAttack() {
		String pname = game.getOtherPlayer(me).getUsername();
		if(pname.equals("KITest")) {
			doTurn();
			return;
		}
		
		SendLaterUtils.callLater(this, "attack", 2);
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
                if(i > 500) {
                	String err = "Die AI hat sich beim Schiffe platzieren aufgehangen =(";
                	AnswerUtils.sendError(this.game.getOtherPlayer(this.me).getSession(), err);
                	return;
                }
                i++;
            }
            tries.put(t, i);
        }
        System.out.println("## " + tries.toString());
        this.game.playerFinishedPlacingShips(this.me);
	}

	@Override
	public void call(String type) {
		switch(type) {
		case "place":
			placeShips();
			break;
		case "attack":
			doTurn();
			break;
		case "congrats":
			congrats();
			break;
		}
	}
	
	private void congrats() {
		String msg = "Glückwunsch! Gutes Spiel! :)";
		AnswerUtils.sendChatMessage(this.game.getOtherPlayer(this.me).getSession(), msg);
	}

	public String getMode() {
		return this.difficulty;
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
	
	// From SP
	private Point calcTurnCoord() {
        if (mode.equals("search")) {
            if (this.difficulty.equals("hard"))
                return calcBetterSearchCoord();

            // Random Coord
            boolean validCoord = false;
            Point tmp = new Point(-1, -1);
            while (!validCoord) {
                tmp = new Point(rnd.nextInt(10), rnd.nextInt(10));
                if (!alreadyFiredHere(tmp))
                    validCoord = true;
            }
            this.tries[tmp.getX()][tmp.getY()] = true;
            return tmp;
        } else if (mode.equals("search2nd")) {
            ArrayList<Point> tries = new ArrayList<Point>();
            Point lh = this.lastHitCoord;
            tries.add(new Point(lh.getX(), lh.getY() - 1));
            tries.add(new Point(lh.getX() - 1, lh.getY()));
            tries.add(new Point(lh.getX(), lh.getY() + 1));
            tries.add(new Point(lh.getX() + 1, lh.getY()));

            ArrayList<Point> newTries = new ArrayList<Point>(tries);
            for (Point tr : tries) {
                if (!Field.isValidPoint(tr) || alreadyFiredHere(tr)) {
                    newTries.remove(tr);
                }
            }
            tries = newTries;

            int size = tries.size();
            int pos = rnd.nextInt(size);
            return tries.get(pos);
        } else if (mode.equals("destroy")) {
            Point target = new Point(this.lastCoord.getX() + this.destroyDirX, this.lastCoord.getY() + this.destroyDirY);
            if (!Field.isValidPoint(target) || !this.wasLastHit || alreadyFiredHere(target)) {
                this.destroyDirX = this.destroyDirX * -1;
                this.destroyDirY = this.destroyDirY * -1;
                this.lastCoord = this.firstHitCoord;

                target = new Point(this.lastCoord.getX() + this.destroyDirX, this.lastCoord.getY() + this.destroyDirY);
            }
            return target;
        }
        return null;
    }
	
	private boolean alreadyFiredHere(Point p) {
        return this.fires[p.getX()][p.getY()];
    }
	
	private Point calcBetterSearchCoord() {
        boolean validCoord = false;
        Point tmp = new Point(-1, -1);
        while (!validCoord) {
            int ty = rnd.nextInt(10);
            int tx = ty % 2;
            tx += (rnd.nextInt(5) * 2);
            tmp = new Point(ty, tx);
            if (!alreadyFiredHere(tmp) && !alreadyTriedOnAdjascent(tmp) && Field.isValidPoint(tmp))
                validCoord = true;
        }
        this.tries[tmp.getX()][tmp.getY()] = true;
        return tmp;
    }
	
	private boolean alreadyTriedOnAdjascent(Point p) {
        Point[] pts = new Point[4];
        pts[0] = new Point(p.getX(), p.getY() - 1);
        pts[1] = new Point(p.getX() - 1, p.getY());
        pts[2] = new Point(p.getX() + 1, p.getY());
        pts[3] = new Point(p.getX(), p.getY() + 1);

        for (Point pt : pts) {
            if (Field.isValidPoint(pt)) {
                if (this.tries[pt.getX()][pt.getY()] || this.hits[pt.getX()][pt.getY()])
                    return true;
            }
        }
        return false;
    }
	
	public void doTurn() {
		Point target = calcTurnCoord();
		System.out.println("AI firing on " + target.toString());
		
		HashMap<String, String> fields = new HashMap<String, String>();
		fields.put("x", String.valueOf(target.getX()));
		fields.put("y", String.valueOf(target.getY()));
		
		Ship t = this.game.attack(this.me, fields);
		
		this.fires[target.getX()][target.getY()] = true;
        this.lastCoord = target;
        
        if(t != null) {
        	this.hits[target.getX()][target.getY()] = true;
        	if (this.mode == "search") {
                this.lastHitCoord = target;
                this.firstHitCoord = target;
                this.mode = "search2nd";
            } else if (this.mode == "search2nd") {
                this.destroyDirX = target.getX() - this.lastHitCoord.getX();
                this.destroyDirY = target.getY() - this.lastHitCoord.getY();
                this.mode = "destroy";
            }
            
            if (t.isDestroyed()) {
                this.mode = "search";
            }
            this.wasLastHit = true;
        } else {
        	this.wasLastHit = false;
        }
	}

	public void triggerCongrats() {
		SendLaterUtils.callLater(this, "congrats", 2);
	}
}
