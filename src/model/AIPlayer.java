package model;

import java.text.DecimalFormat;
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
    private boolean[][] fires;
    private boolean[][] destroyedShips;
    private ArrayList<Point> currentShip;
	
	private String difficulty = "easy";
	private boolean hasPlacedShips = false;
	private Game game;
	private Player me;
	
	public AIPlayer(Player me, String mode) {
		this.difficulty = mode;
		this.me = me;
		
        this.fires = new boolean[10][10];
        
        this.destroyedShips = new boolean[10][10];
        this.currentShip = new ArrayList<Point>();
	}

	public boolean hasPlacedShips() {
		return this.hasPlacedShips;
	}

	public void triggerShipPlacement() {
		this.hasPlacedShips = true;
		SendLaterUtils.callLater(this, "place", 3);
	}
	
	public void triggerAttack() {
		SendLaterUtils.callLater(this, "attack", 2);
	}
	
	public void placeShips() {
		Field aiField = this.game.getPlayerField(this.me);
		
        // Try to place all ships randomly with double safety
		boolean done = false;
		int completeTries = 0;
		while(!done) {
		    // For a new try, clear the field
		    aiField.clear();
		    boolean error = false;
		    
		    // Try to place each shiptype randomly
            for (ShipType t : ShipType.values()) {
                
                // Try to place the shiptype at random locations, max 500 tries
                boolean placed = false;
                int i = 0;
                while (!placed && !error) {
                    Orientation o = (rnd.nextBoolean()) ? Orientation.HORIZONTAL : Orientation.VERTICAL;
                    Point pos = new Point(rnd.nextInt(10), rnd.nextInt(10));
                    Ship s = new Ship(t, pos, o);
                    boolean occupied = aiField.isSpaceOccupied(s);
                    if (!occupied) {
                        aiField.placeShip(s);
                        placed = true;
                    }
                    if(i > 500) {
                        error = true;
                    }
                    i++;
                }
            }
            // If there was an error in the try, do a new one, if already more than 10 errors, quit
            if(error) {
                if(completeTries > 10) {
                    String err = "Die AI hat sich beim Schiffe platzieren aufgehangen =(";
                    AnswerUtils.sendError(this.game.getOtherPlayer(this.me).getSession(), err);
                    return;
                }
            } else {
                done = true;
            }
            completeTries++;
		}
        System.out.println("## Complete Tries: " + completeTries);
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
		case "congratswin":
			congrats(true);
			break;
		case "congratsgg":
            congrats(false);
            break;
		case "havefun":
		    havefun();
		    break;
		}
	}
	
	private void havefun() {
        String msg = "Viel Glück! Viel Erfolg!";
        AnswerUtils.sendChatMessage(this.game.getOtherPlayer(this.me).getSession(), msg);
    }

    private void congrats(boolean win) {
	    String msg = "";
	    if(win) {
	        msg = "Glückwunsch! Gutes Spiel!";
	    } else {
	        msg = "Vielen Dank! Gutes Spiel!";
	    }
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
		if(msg.equals("stats")) {
		    calculateStatistics();
		}
	}
	
	private void calculateStatistics() {
		System.out.println("Calc");
	    DecimalFormat df = new DecimalFormat("###.##");
	    
        Field myField = this.game.getPlayerField(this.me);
        int opShots = myField.getOpponentShotCount();
        int opHits = myField.getOpponentHitCount();
        System.out.println("Calc2");
        
        Field opField = this.game.getPlayerField(this.game.getOtherPlayer(this.me));
        int myShots = opField.getOpponentShotCount();
        int myHits = opField.getOpponentHitCount();
        
        System.out.println("* op " + opShots + " my " + myShots);
        
        if(opShots > 1 && myShots > 1) {
            double myProgress = myHits / 30.0 * 100.0;
            double opProgress = opHits / 30.0 * 100.0;
            String leading = "";
            if(myProgress > opProgress) {
                leading = "Momentan führe ich mit " + df.format(myProgress) + "% zu " + df.format(opProgress) + "%";
            } else if(opProgress < myProgress) {
                leading = "Momentan führst du mit " + df.format(opProgress) + "% zu " + df.format(myProgress) + "%";
            } else {
                leading = "Momentan sind wir gleichauf mit " + df.format(myProgress) + "% zu " + df.format(opProgress) + "%";
            }
            System.out.println("* send1");
            AnswerUtils.sendChatMessage(this.game.getOtherPlayer(this.me).getSession(), leading);
            
            double myQuote = (myHits*1.0) / (myShots * 1.0) * 100.0;
            double opQuote = (opHits*1.0) / (opShots * 1.0) * 100.0;
            String opQ = "Deine Trefferquote: " + df.format(opQuote) + "%";
            String myQ = "Meine Trefferquote: " + df.format(myQuote) + "%";
            System.out.println("* send2");
            AnswerUtils.sendChatMessage(this.game.getOtherPlayer(this.me).getSession(), opQ);
            AnswerUtils.sendChatMessage(this.game.getOtherPlayer(this.me).getSession(), myQ);
            
        } else {
            String msg = "Statistiken gibt es erst nachdem jeder einmal geschossen hat.";
            System.out.println("* sendErr");
            AnswerUtils.sendChatMessage(this.game.getOtherPlayer(this.me).getSession(), msg);
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
                if (!Field.isValidPoint(tr) || alreadyFiredHere(tr) || isCellExcluded(tr)) {
                    newTries.remove(tr);
                }
            }
            tries = newTries;

            int size = tries.size();
            int pos = rnd.nextInt(size);
            return tries.get(pos);
        } else if (mode.equals("destroy")) {
            Point target = new Point(this.lastCoord.getX() + this.destroyDirX, this.lastCoord.getY() + this.destroyDirY);
            if (!Field.isValidPoint(target) || !this.wasLastHit || alreadyFiredHere(target) || isCellExcluded(target)) {
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
            if (!alreadyFiredHere(tmp) && !isCellExcluded(tmp) && Field.isValidPoint(tmp))
                validCoord = true;
        }
        return tmp;
    }
	
	private boolean isCellExcluded(Point p) {
        Point[] pts = new Point[8];
        pts[0] = new Point(p.getX(), p.getY() - 1);
        pts[1] = new Point(p.getX() - 1, p.getY());
        pts[2] = new Point(p.getX() + 1, p.getY());
        pts[3] = new Point(p.getX(), p.getY() + 1);
        pts[4] = new Point(p.getX() - 1, p.getY() - 1);
        pts[5] = new Point(p.getX() + 1, p.getY() - 1);
        pts[6] = new Point(p.getX() - 1, p.getY() + 1);
        pts[7] = new Point(p.getX() + 1, p.getY() + 1);

        for (Point pt : pts) {
            if (Field.isValidPoint(pt)) {
                if (this.destroyedShips[pt.getX()][pt.getY()])
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
        	if (this.mode == "search") {
                this.lastHitCoord = target;
                this.firstHitCoord = target;
                this.mode = "search2nd";
            } else if (this.mode == "search2nd") {
                this.destroyDirX = target.getX() - this.lastHitCoord.getX();
                this.destroyDirY = target.getY() - this.lastHitCoord.getY();
                this.mode = "destroy";
            }
        	
        	// Register as Hit for current Ship
            this.currentShip.add(target);
            
            if (t.isDestroyed()) {
                this.mode = "search";
                
                // Register full hit
                for(Point p : this.currentShip) {
                    this.destroyedShips[p.getX()][p.getY()] = true;
                }
                this.currentShip.clear();
            }
            this.wasLastHit = true;
        } else {
        	this.wasLastHit = false;
        }
	}

	public void triggerCongrats(boolean win) {
	    String type = (win) ? "congratswin" : "congratsgg";
		SendLaterUtils.callLater(this, type, 2);
	}

    public void triggerHaveFun() {
        SendLaterUtils.callLater(this, "havefun", 1);
    }
}
