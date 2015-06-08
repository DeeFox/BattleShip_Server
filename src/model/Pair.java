package model;

public class Pair<U, V> {
	private U var1;
	private V var2;
	
	public Pair(U var1, V var2) {
		super();
		this.var1 = var1;
		this.var2 = var2;
	}

	public U getVar1() {
		return var1;
	}

	public void setVar1(U var1) {
		this.var1 = var1;
	}

	public V getVar2() {
		return var2;
	}

	public void setVar2(V var2) {
		this.var2 = var2;
	}
	
	
}
