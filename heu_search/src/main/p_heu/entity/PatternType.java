package p_heu.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class PatternType {
    private String raw;
	private PatternTypeNode[] nodes;

	public PatternType(String content) {
	    this.raw = content;
        java.util.regex.Pattern pat = java.util.regex.Pattern.compile("([RW])(\\d+)\\((.+?)\\)");
        Matcher matcher = pat.matcher(content);

        List<PatternTypeNode> nodeList = new ArrayList<>();
        while (matcher.find()) {
            String type = matcher.group(1);
            String thread = matcher.group(2);
            String var = matcher.group(3);

            if (type.equals("R")) {
                type = "READ";
            }
            else if (type.equals("W")) {
                type = "WRITE";
            }
            else {
                throw new RuntimeException("Unexpected parameter.");
            }
            nodeList.add(new PatternTypeNode(var, thread, type));
        }
        this.nodes = nodeList.toArray(new PatternTypeNode[nodeList.size()]);
    }

    public String getRaw() {
	    return this.raw;
    }
	
	public PatternTypeNode[] getNodes() {
		return this.nodes;
	}
	
	public boolean isSame(PatternType patternType) {
		if (this.nodes.length != patternType.getNodes().length) {
			return false;
		}
		for (int i = 0; i < this.nodes.length; ++i) {
			if (!this.nodes[i].isSame(patternType.getNodes()[i])) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
	    StringBuilder stringBuilder = new StringBuilder("PatternType[");
	    for (Node node : this.nodes) {
	        stringBuilder.append(node + " ");
        }
        stringBuilder.append("]");
	    return stringBuilder.toString();
    }
}
