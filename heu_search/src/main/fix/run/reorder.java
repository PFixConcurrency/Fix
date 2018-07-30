package fix.run;

import p_heu.entity.ReadWriteNode;
import p_heu.entity.pattern.Pattern;
import p_heu.run.Unicorn;

import java.util.List;

public class reorder {

    public static void main(String[] args) {

    }

    public static void r(List<Unicorn.PatternCounter> list, int index, String varName,String position){
System.out.println("this method");
        for(int i = index; i < list.size(); i++){
            Pattern pattern = list.get(i).getPattern();
            for(ReadWriteNode rw : pattern.getNodes()){
//                if(rw.getField().equals(varName) && rw.getType().equals("write")){
                    if(rw.getField().equals(varName) && rw.getType().equals("WRITE") && !rw.getPosition().equals(position)){
                        System.out.println(rw);
                    }
//                }

            }
        }
        System.exit(-1);
    }
}
