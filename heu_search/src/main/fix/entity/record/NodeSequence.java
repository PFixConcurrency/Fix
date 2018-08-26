package fix.entity.record;

import java.util.ArrayList;
import java.util.List;

//This class differentiates ReadWriteNode by instance, variable, thread, and address
//Record the id to indicate the order in which the same thread executes the same variable on the same line
public class NodeSequence {
    private String element;
    private String field;
    private String position;

    //Record the id of the original ReadWriteNode
    private List<Integer> idList = new ArrayList<Integer>();


    public String getElement() {
        return element;
    }

    public String getField() {
        return field;
    }

    public String getPosition() {
        return position;
    }

    public NodeSequence(String element, String field, String position) {
        this.element = element;
        this.field = field;
        this.position = position;
    }

    public void add(int id){
        idList.add(id);
    }

    public List<Integer> getIdList() {
        return idList;
    }


    @Override
    public String toString() {
        return element +"," + field + "," + position;
    }
}
