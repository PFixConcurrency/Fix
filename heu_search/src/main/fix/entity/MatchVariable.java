package fix.entity;
/**
 * This is a class for matching related variables
 */

import org.eclipse.jdt.core.dom.ASTNode;

import java.util.HashSet;
import java.util.Set;

public class MatchVariable {
    private Set<ASTNode> matchSet = new HashSet<ASTNode>();//Store the accessed variables to match
    private int startLine;
    private int endLine;
    private int lockNum = 0;

    private ASTNode sameFatherNode = null;//father node

    public Set<ASTNode> getMatchSet() {
        return matchSet;
    }
    public void setMatchSet(Set<ASTNode> matchSet) {
        this.matchSet = matchSet;
    }
    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }
    public ASTNode getSameFatherNode() {
        return sameFatherNode;
    }
    public int getLockNum() {
        return lockNum;
    }
    public void setSameFatherNode(ASTNode sameFatherNode) {
        this.sameFatherNode = sameFatherNode;
    }
    //add node to set
    public void addMatchSet(ASTNode node){
        this.matchSet.add(node);
    }

    //is empty
    public boolean matchSetIsEmpty(){
        return this.matchSet.isEmpty();
    }

    //clear out
    public void clear(){
        this.matchSet.clear();
        this.sameFatherNode = null;
    }

    //match
    public boolean equalTarget(Set<String> target){
        if(this.matchSet.size() > target.size())
            return false;

        return true;
    }

    public void update() {
        lockNum++;
    }

    //Look for two different astnodes with the same parent
    public void searchSame(ASTNode node){
        if(this.sameFatherNode.equals(node))
            return ;
        else{
            ASTNode saveNode = this.sameFatherNode;//Node for manipulation
            ASTNode startNode = saveNode;
            ASTNode endNode = node;
            for(ASTNode iNode = saveNode; iNode != null; iNode = iNode.getParent()){
                for(ASTNode jNode = node;jNode != null;jNode = jNode.getParent()){
                    if(iNode.equals(jNode)){
                        this.startLine = startNode.getStartPosition();
                        this.endLine = endNode.getStartPosition() + endNode.getLength();
                        this.sameFatherNode = startNode;
                        return;
                    }
                    endNode = jNode;
                }
                startNode = iNode;
            }
        }
    }

    public boolean containsIn(Set<String> variableSet) {

        for (ASTNode matchSetNode : matchSet){
            String match = matchSetNode.toString();
            System.out.println(match+"=====");
            for (String s : variableSet){
//                if(!match.equals(s))
//                    return false;
                System.out.println(s);
            }

        }
        return true;
    }
}
