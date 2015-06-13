package model;

import java.util.HashMap;
import java.util.Map;

public class Ship {
	public enum ShipType {
		BATTLESHIP	("Battleship", 0, 5),
		CRUISER		("Cruiser", 1, 4),
		DESTROYER	("Destroyer", 2, 3),
		SUBMARINE	("Submarine", 3, 2);
		
		public final String name;
		public final int id;
		public final int size;
		
		private static final Map<String, ShipType> shortNames;
	    static
	    {
	    	shortNames = new HashMap<String, ShipType>();
	    	shortNames.put("B", ShipType.BATTLESHIP);
	    	shortNames.put("C", ShipType.CRUISER);
	    	shortNames.put("D", ShipType.DESTROYER);
	    	shortNames.put("S", ShipType.SUBMARINE);
	    }
		
		private ShipType(String name, int id, int size) {
			this.id = id;
			this.name = name;
			this.size = size;
		}
		
		public static ShipType getFromShort(String sh) {
			return ShipType.shortNames.get(sh);
		}
	}
	
	public enum Orientation {
		HORIZONTAL (1,  0), 
		VERTICAL   (0,  1);
		
		public final int x;
		public final int y;
		
		private Orientation(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public char shortName() {
			return (this.equals(Orientation.HORIZONTAL)) ? 'h' : 'v';
		}
	}
	
	private int posX;
	private int posY;
	private Orientation orientation;
	private ShipType type;
	private int hitCounter = 0;
	
	public Ship(ShipType type, int posX, int posY, Orientation orientation) {
		this.type = type;
		this.posX = posX;
		this.posY = posY;
		this.orientation = orientation;
	}
	
	public void incrementHitCounter() {
		this.hitCounter++;
	}
	
	public boolean isDestroyed() {
		return (this.hitCounter == this.type.size);
	}

	public int getPosX() {
		return posX;
	}

	public int getPosY() {
		return posY;
	}

	public Orientation getOrientation() {
		return orientation;
	}

	public ShipType getType() {
		return type;
	}
	
	
}
