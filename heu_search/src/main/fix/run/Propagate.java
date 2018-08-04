package fix.run;

import fix.analyzefile.CheckWhetherLocked;
import fix.analyzefile.UseASTAnalysisClass;
import fix.io.ExamplesIO;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.sequence.Sequence;

import java.util.HashSet;
import java.util.Set;

public class Propagate {

    static ExamplesIO examplesIO = ExamplesIO.getInstance();

    public static Set numSet = new HashSet();//has lock line num


    public static void p(String lockName, ReadWriteNode readWriteNode, Sequence sequence, String sourceClassPath, String analyseJavaPath) {

        for (Node n : sequence.getNodes()) {
            if (n instanceof ReadWriteNode) {
                ReadWriteNode rwn = (ReadWriteNode) n;
                if (rwn.getElement().equals(readWriteNode.getElement()) && rwn.getField().equals(readWriteNode.getField())) {
                    int poi = Integer.parseInt(rwn.getPosition().split(":")[1]);
                    int firstLoc = poi, lastLoc = firstLoc;
                    //这行没处理过
                    if (!numSet.contains(poi)) {
                        numSet.add(poi);
                       /* System.out.println("get");
                        System.out.println(rwn);
                        System.out.println(firstLoc + "," + lastLoc + "," + analyseJavaPath);
                        System.out.println(!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(11, 11, analyseJavaPath) + "get2");*/
                        if (!CheckWhetherLocked.check(rwn.getPosition(), rwn.getField(), sourceClassPath, analyseJavaPath) && !UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc + 1, analyseJavaPath)) {

                            System.out.println("dao1");
                            System.out.println(rwn);
                            //判断加锁会不会和for循环等交叉
                            UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                            firstLoc = lockLine.getFirstLoc();
                            lastLoc = lockLine.getLastLoc();

                            //检查会不会定义变量在锁内，使用变量在锁外
                            lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                            firstLoc = lockLine.getFirstLoc();
                            lastLoc = lockLine.getLastLoc();

                            if (firstLoc > 0 && lastLoc > 0) {
                                //加锁
                                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//待定
                            }

                        }
                    }
                }
            }
        }
    }
}
