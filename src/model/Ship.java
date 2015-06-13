package model;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class Ship {

	public enum ShipType {
		B1		("B1", "BattleShip", 1, 5),
		C1		("C1", "Cruiser", 1, 4),
		C2		("C2", "Cruiser", 2, 4),
		D1		("D1", "Destroyer", 1, 3),
		D2		("D2", "Destroyer", 2, 3),
		D3		("D3", "Destroyer", 3, 3),
		S1		("S1", "Submarine", 1, 2),
		S2		("S2", "Submarine", 2, 2),
		S3		("S3", "Submarine", 3, 2),
		S4		("S4", "Submarine", 4, 2);
		
		public int size;
		public final String ident;
		public final String name;
		
		private ShipType(String ident, String name, int id, int size) {
			this.size = size;
			this.name = name;
			this.ident = ident;
		}
		
		public static ShipType fromIdent(String ident) {
			ShipType res = null;
			for(ShipType st : ShipType.values()) {
				if(st.ident.equals(ident))
					res = st;
			}
			return res;
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
		
		public static Orientation fromString(String o) {
			return (o.equals("h")) ? Orientation.HORIZONTAL : Orientation.VERTICAL;
		}
		
		public String shortName() {
			return (this.equals(Orientation.HORIZONTAL)) ? "horizontal" : "vertical";
		}
	}
	
	private ShipType type;
	private Point position;
	private Orientation orientation;
	private boolean[] hits;

	public Ship(ShipType type, Point coordinates, Orientation orientation) {
		this.type = type;
		this.position = coordinates;
		this.orientation = orientation;
		this.hits = new boolean[type.size];
	}

	public Point getPosition() {
		return position;
	}

	public Orientation getOrientation() {
		return orientation;
	}
	
	public ShipType getType() {
		return type;
	}

	public boolean isDestroyed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public JsonElement getAsJson(boolean forOwner) {
		JsonArray hits = new JsonArray();
		boolean[] boolHits = this.hits;
		
		if(!forOwner) {
			boolHits = new boolean[this.type.size];
			if(isDestroyed()) {
				Arrays.fill(boolHits, true);
			}
		}
		for(boolean h : boolHits) {
			JsonPrimitive hit = new JsonPrimitive(h);
			hits.add(hit);
		}
		return hits;
	}
}
