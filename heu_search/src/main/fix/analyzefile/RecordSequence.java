package fix.analyzefile;

import fix.entity.record.MatchResult;
import fix.entity.record.NodeSequence;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.sequence.Sequence;

import java.util.ArrayList;
import java.util.List;

//operate sequence
public class RecordSequence {

    //Store all NodeSequence
    static List<NodeSequence> nodeSequenceList = new ArrayList<NodeSequence>();

    //All read-write nodes
    static List<ReadWriteNode> readWriteNodeList = new ArrayList<ReadWriteNode>();

    public static List<NodeSequence> getNodeSequenceList() {
        return nodeSequenceList;
    }

    public static List<ReadWriteNode> getReadWriteNodeList() {
        return readWriteNodeList;
    }

    public static void analyseReadWriteNodeList(List<Node> nodesList) {
        for (Node node : nodesList) {
            if (node instanceof ReadWriteNode) {
                readWriteNodeList.add((ReadWriteNode) node);
            }
        }
    }

    public static void display(Sequence firstFailAppearPlace) {
        List<Node> nodesList = firstFailAppearPlace.getNodes();
        for (Node node : nodesList) {
            if (node instanceof ReadWriteNode) {
//                System.out.println(node);
                divideNodes((ReadWriteNode) node);
            }
        }

        /*for(int i = 0; i < nodeSequenceList.size(); i++)
            System.out.println(nodeSequenceList.get(i).getIdList());
        System.out.println("========");*/

//        System.exit(-1);


        analyseReadWriteNodeList(nodesList);

    }

    //Sort the data in your sequence by thread and number of rows
    private static void divideNodes(ReadWriteNode node) {
        NodeSequence nodeSequence = new NodeSequence(node.getElement(), node.getField(), node.getPosition());
        //So let's see if there's any in the list, no, add, there's, find the one, add it in its arr
        MatchResult mr = listMatch(nodeSequence);
        if (mr.isFlag()) {
            nodeSequenceList.get(mr.getIndex()).add(node.getId());
        } else {
            nodeSequence.add(node.getId());
            nodeSequenceList.add(nodeSequence);
        }
    }

    private static MatchResult listMatch(NodeSequence ns) {
        MatchResult matchResult = new MatchResult();
        for (int i = 0; i < nodeSequenceList.size(); i++) {
            NodeSequence nodeS = nodeSequenceList.get(i);
            if (nodeS.getElement().equals(ns.getElement()) && nodeS.getField().equals(ns.getField()) && nodeS.getPosition().equals(ns.getPosition())) {
                matchResult.setFlag(true);
                matchResult.setIndex(i);
            }
        }
        return matchResult;
    }

    //Is not the last operation of the original statement
    public static boolean isLast(ReadWriteNode readWriteNode) {
        for (int i = 0; i < nodeSequenceList.size(); i++) {
            NodeSequence nodeS = nodeSequenceList.get(i);
            if (nodeS.getElement().equals(readWriteNode.getElement()) && nodeS.getField().equals(readWriteNode.getField()) && nodeS.getPosition().equals(readWriteNode.getPosition())) {
                int index = nodeS.getIdList().indexOf(readWriteNode.getId());
                if ((index + 1) == nodeS.getIdList().size())
                    return true;
            }
        }
        return false;
    }

    //Is not the first operation of the original statement
    public static boolean isFirst(ReadWriteNode readWriteNode) {
        for (int i = 0; i < nodeSequenceList.size(); i++) {
            NodeSequence nodeS = nodeSequenceList.get(i);
            if (nodeS.getElement().equals(readWriteNode.getElement()) && nodeS.getField().equals(readWriteNode.getField()) && nodeS.getPosition().equals(readWriteNode.getPosition())) {
                int index = nodeS.getIdList().indexOf(readWriteNode.getId());
                if (index == 0)
                    return true;
            }
        }
        return false;
    }
}
