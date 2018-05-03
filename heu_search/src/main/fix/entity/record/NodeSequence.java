package fix.entity.record;

import java.util.ArrayList;
import java.util.List;

//这个类根据实例、变量、线程、地址来区分ReadWriteNode,
//将id记录下来，表示同一个线程在同一行对同一个变量的执行顺序
public class NodeSequence {
    private String element;
    private String field;
    private String position;

    //记录原本ReadWriteNode的id
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
