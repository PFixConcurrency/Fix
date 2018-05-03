package p_heu.entity;

public class PatternTypeNode extends Node {
	protected String var;
	protected String thread;
	protected String type;
	
	public PatternTypeNode(String var, String thread, String type) {
		this.id = -1;
		this.var = var;
		this.thread = thread;
		this.type = type;
	}

	@Override
	public boolean isIdentical(Node node) {
		return isSame(node);
	}

	@Override
	public boolean isSame(Node node) {
		if (node instanceof PatternTypeNode) {
			PatternTypeNode ptNode = (PatternTypeNode)node;
			return this.var.equals(ptNode.getVar()) && this.thread.equals(ptNode.getThread())
					&& this.type.equals(ptNode.getType());
		}
		return false;
	}
	
	public String getVar() {
		return this.var;
	}
	
	public String getThread() {
		return this.thread;
	}
	
	public String getType() {
		return this.type;
	}

	public String toString() {
		return "PatternTypeNode[" + var + "," + thread + "," + type + "]";
	}
}
