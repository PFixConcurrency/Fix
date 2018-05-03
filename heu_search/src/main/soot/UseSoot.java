package soot;

import p_heu.entity.ReadWriteNode;
import soot.structure.entity.CommonCaller;

import java.util.Set;

public class UseSoot {

    //单例模式
    private static UseSoot useSoot = new UseSoot();

    private UseSoot() {
    }

    public static UseSoot getInstance() {
        return useSoot;
    }

    //加锁行数
    private int minLine = Integer.MAX_VALUE;
    private int maxLine = 0;

    //加锁的文件
    private String syncJava = "";

    public int getMinLine() {
        return minLine;
    }

    public int getMaxLine() {
        return maxLine;
    }

    public String getSyncJava() {
        return syncJava;
    }

    public static void main(String[] args) {
//        ReadWriteNode rw1 = new ReadWriteNode(1, "datarace.CustomerInfo@16f", "accounts", "WRITE", "main", "datarace/Account.java:12");
//        ReadWriteNode rw2 = new ReadWriteNode(2, "datarace.CustomerInfo@16f", "accounts", "READ", "Thread-1", "datarace/Account.java:8");
        //拿不到signature
        ReadWriteNode rw1 = new ReadWriteNode(1, "org.apache.commons.pool.impl.CursorableLinkedList$Listable@17d", "_next", "READ", "Thread-2", "org/apache/commons/pool/impl/CursorableLinkedList.java:976");
        ReadWriteNode rw2 = new ReadWriteNode(2, "org.apache.commons.pool.impl.CursorableLinkedList$Listable@17d", "_next", "WRITE", "Thread-1", "org/apache/commons/pool/impl/CursorableLinkedList.java:988");
        /*ReadWriteNode rw1 = new ReadWriteNode(1, "account2.Account@167", "Balance", "WRITE", "Thread-2", "buggyprogram/BuggyProgram.java:352");
        ReadWriteNode rw2 = new ReadWriteNode(2, "account2.Account@167", "Balance", "READ", "Thread-1", "buggyprogram/BuggyProgram.java:367");*/

        UseSoot useSoot = UseSoot.getInstance();
        useSoot.getCallGraph(rw1, rw2);
        System.out.println(useSoot.getMinLine());
        System.out.println(useSoot.getMaxLine());
        System.out.println(useSoot.getSyncJava());
    }

    //得到函数调用图
    public void getCallGraph(ReadWriteNode rw1, ReadWriteNode rw2) {
        //处理数据
        String position = rw1.getPosition();
        String element = rw1.getElement();
        int index = element.indexOf('@');
        String classNameOne = index == -1 ? element : element.substring(0, index);

        int classLineOne = Integer.parseInt(position.split(":")[1]);

        position = rw2.getPosition();
        index = element.indexOf('@');
        String classNameTwo = index == -1 ? element : element.substring(0, index);

        int classLineTwo = Integer.parseInt(position.split(":")[1]);

        System.out.println(classNameOne + "," + classLineOne + "," + classNameTwo + "," + classLineTwo);
//        System.exit(-1);
        //利用soot得到调用图
        Set<CommonCaller> callGraphInfo = Main.getCallGraphInfo(classNameOne, classLineOne, classNameTwo, classLineTwo);

        System.out.println(callGraphInfo);
//        System.exit(-1);

        for (CommonCaller caller : callGraphInfo) {
            syncJava = caller.getMethod().getClassName();
            int tempMin = Math.min(caller.getLeftRow(), caller.getRightRow());
            int tempMax = Math.max(caller.getLeftRow(), caller.getRightRow());
            if (minLine > tempMin) {
                minLine = tempMin;
            }
            if (maxLine < tempMax) {
                maxLine = tempMax;
            }
        }

        int indexInner = syncJava.indexOf('$');
        if (indexInner != -1) {
            syncJava = syncJava.substring(0, indexInner);
        }
        syncJava = syncJava.replaceAll("\\.", "/") + ".java";


    }
}
