package model;

public class Point {
	private int x;
	private int y;
	
	public Point(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public Point() {
		this(0, 0);
	}
	
	public static Point fromStrings(String x, String y) throws NumberFormatException {
		Point p = new Point();
		p.setX(Integer.parseInt(x));
		p.setY(Integer.parseInt(y));
		
		if(p.getX() < 0 || p.getX() > 9 || p.getY() < 0 || p.getY() > 9) {
			throw new NumberFormatException("Coordinates out of range");
		}
		return p;
	}
	
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	
}
