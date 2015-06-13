package model;

import java.util.ArrayList;

import model.Ship.ShipType;

public class Field {
	
	private boolean[][] opponentShots;
	private ArrayList<Ship> ships;
	private Ship[][] fields;
	
	private int[] shipCountLimits;
	
	public Field() {
		this.shipCountLimits = new int[4];
		this.shipCountLimits[0] = 1;
		this.shipCountLimits[1] = 2;
		this.shipCountLimits[2] = 3;
		this.shipCountLimits[3] = 4;
		
		this.ships = new ArrayList<Ship>();
		
		this.fields = new Ship[10][10];
		this.opponentShots = new boolean[10][10];
	}
	
	// TODO
	public boolean placeShip(Ship ship) {
		ShipType type = ship.getType();
		
		// Check if already all ships of this type were set
		if(this.shipCountLimits[type.id] == 0)
			return false;
		
		// Check for collisions
		if(isSpaceOccupied(ship))
			return false;
		
		// Place ship
		this.ships.add(ship);
		for(int i = 0; i < ship.getType().size; i++) {
			int posX = ship.getPosX() + (ship.getOrientation().x * i);
			int posY = ship.getPosY() + (ship.getOrientation().y * i);
			this.fields[posX][posY] = ship;
		}
		
		this.shipCountLimits[type.id]--;
		return true;
	}
	
	public boolean allShipsPlaced() {
		boolean shipsLeft = false;
		for(int i : this.shipCountLimits) {
			if(i > 0)
				shipsLeft = true;
		}
		return !shipsLeft;
	}
	
	public boolean allShipsDestroyed() {
		boolean shipsLeft = false;
		for(Ship ship : this.ships) {
			if(!ship.isDestroyed())
				shipsLeft = true;
		}
		return !shipsLeft;
	}
	
	private boolean isSpaceOccupied(Ship ship) {
		boolean isOccupied = false;
		for(int i = 0; i < ship.getType().size; i++) {
			int posX = ship.getPosX() + (ship.getOrientation().x * i);
			int posY = ship.getPosY() + (ship.getOrientation().y * i);
			if(posX < 0 || posX > 9 || posY < 0 || posY > 9) {
				isOccupied = true;
			} else {
				if(this.fields[posX][posY] != null)
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
			dest.incrementHitCounter();
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
}
