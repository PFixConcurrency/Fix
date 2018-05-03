package p_heu.entity;

public abstract class Node {
	protected int id;
	
	public int getId() {
		return this.id;
	}
	
	public abstract boolean isIdentical(Node node);
	
	public abstract boolean isSame(Node node);

	public abstract String toString();
}
