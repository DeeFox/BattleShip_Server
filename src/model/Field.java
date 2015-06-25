package model;

import java.util.HashMap;

import model.Ship.ShipType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Field {
	
	private boolean[][] opponentShots;
	private Ship[][] fields;
	
	private HashMap<ShipType, Ship> ships;
	
	public Field() {
		this.ships = new HashMap<ShipType, Ship>();
		for(ShipType t : ShipType.values()) {
			this.ships.put(t, null);
		}
		
		this.fields = new Ship[10][10];
		this.opponentShots = new boolean[10][10];
	}
	
	public int getOpponentShotCount() {
		int cnt = 0;
		for(int y = 0; y < 10; y++) {
			for(int x = 0; x < 10; x++) {
				if(opponentShots[x][y]) {
					cnt++;
				}
			}
		}
		return cnt;
	}
	
	public boolean placeShip(Ship ship) {
		ShipType type = ship.getType();
		
		// Check if shiptype already set
		if(this.ships.get(type) != null)
			return false;
		
		// Check for collisions
		if(isSpaceOccupied(ship))
			return false;
		
		// Place ship
		this.ships.put(type, ship);
		for(int i = 0; i < ship.getType().size; i++) {
			int posX = ship.getPosition().getX() + (ship.getOrientation().x * i);
			int posY = ship.getPosition().getY() + (ship.getOrientation().y * i);
			this.fields[posX][posY] = ship;
		}
		
		return true;
	}
	
	public static String getCharForNumber(int i) {
	    return i > 0 && i < 27 ? String.valueOf((char)(i + 'A' - 1)) : null;
	}
	
	public boolean allShipsPlaced() {
		boolean shipsLeft = false;
		for(ShipType s : this.ships.keySet()) {
			if(this.ships.get(s) == null)
				shipsLeft = true;
		}
		return !shipsLeft;
	}
	
	public boolean allShipsDestroyed() {
		boolean shipsLeft = false;
		for(ShipType s : this.ships.keySet()) {
			Ship ship = this.ships.get(s);
			if(ship != null) {
				if(!ship.isDestroyed())
					shipsLeft = true;
			}
		}
		return !shipsLeft;
	}
	
	public static boolean isValidPoint(Point p) {
		return (p.getX() >= 0 && p.getX() < 10 && p.getY() >= 0 && p.getY() < 10);
	}
	
	public boolean areAdjascentFieldsFree(Point p) {
		Point[] pts = new Point[8];
		pts[0] = new Point(p.getX(), p.getY() - 1);
		pts[1] = new Point(p.getX() - 1, p.getY());
		pts[2] = new Point(p.getX() + 1, p.getY());
		pts[3] = new Point(p.getX(), p.getY() + 1);
		
		pts[4] = new Point(p.getX() - 1, p.getY() - 1);
		pts[5] = new Point(p.getX() + 1, p.getY() - 1);
		pts[6] = new Point(p.getX() + 1, p.getY() + 1);
		pts[7] = new Point(p.getX() - 1, p.getY() + 1);
		
		for(Point pt : pts) {
			if(isValidPoint(pt)) {
				if(this.fields[pt.getX()][pt.getY()] != null)
					return false;
			}
		}
		return true;
	}
	
	public boolean isSpaceOccupied(Ship ship) {
		boolean isOccupied = false;
		for(int i = 0; i < ship.getType().size; i++) {
			int posX = ship.getPosition().getX() + (ship.getOrientation().x * i);
			int posY = ship.getPosition().getY() + (ship.getOrientation().y * i);
			if(posX < 0 || posX > 9 || posY < 0 || posY > 9) {
				isOccupied = true;
			} else {
				if(this.fields[posX][posY] != null || 
						!areAdjascentFieldsFree(new Point(posX, posY)))
						isOccupied = true;
			}
		}
		return isOccupied;
	}
	
	public boolean alreadyFiredHere(Point p) {
		return this.opponentShots[p.getX()][p.getY()];
	}
	
	public Ship fire(Point p) {
		this.opponentShots[p.getX()][p.getY()] = true;
		
		Ship dest = this.fields[p.getX()][p.getY()];
		if(dest != null) {
			dest.registerHit(p);
			return dest;
		}
		return null;
	}

	public static int posFromLetter(char letter) {
		int pos = (int) letter;
		if(pos >= 65 && pos <= 90) {
			return pos - 64;
		}
		return -1;
	}
	
	public String getFieldString(Point p) {
		Ship f = this.fields[p.getX()][p.getY()];
		if(f == null) {
			if(this.opponentShots[p.getX()][p.getY()]) {
				return "+";
			} else {
				return ".";
			}
		} else {
			if(this.opponentShots[p.getX()][p.getY()]) {
				return "X";
			} else {
				return "O";
			}
		}
	}
	
	// New Stuff
	public JsonElement getFieldAsJson(boolean forOwner) {
		JsonObject res = new JsonObject();
		JsonArray hits = new JsonArray();
		JsonArray missed = new JsonArray();
		
		for(int y = 0; y < 10; y++) {
			for(int x = 0; x < 10; x++) {
				if(opponentShots[x][y]) {
					Point c = new Point(x, y);
					JsonElement pos = c.asJsonElement();
					
					Ship dest = this.fields[c.getX()][c.getY()];
					if(dest != null) {
						hits.add(pos);
					} else {
						missed.add(pos);
					}
				}
			}
		}
		res.add("hits", hits);
		res.add("missed", missed);
		
		JsonArray ships = new JsonArray();
		for(ShipType st : this.ships.keySet()) {
			Ship sp = this.ships.get(st);
			if(sp != null) {
				JsonObject ship = new JsonObject();
				ship.addProperty("type", sp.getType().ident);
				if(forOwner || sp.isDestroyed()) {
					ship.addProperty("x", sp.getPosition().getX());
					ship.addProperty("y", sp.getPosition().getY());
					ship.addProperty("orientation", sp.getOrientation().ident());
				}
				JsonElement boolHits = sp.getAsJson(forOwner);
				ship.add("hits", boolHits);
				ships.add(ship);
			}
		}
		res.add("ships", ships);
		
		return res;
	}
}
