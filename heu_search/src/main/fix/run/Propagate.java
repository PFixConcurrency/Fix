package fix.run;

import fix.analyzefile.CheckWhetherLocked;
import fix.analyzefile.UseASTAnalysisClass;
import fix.entity.ImportPath;
import fix.entity.LockPObject;
import fix.io.ExamplesIO;
import fix.io.MergePro;
import p_heu.entity.LockNode;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.sequence.Sequence;

import java.util.HashSet;
import java.util.Set;

public class Propagate {

    static ExamplesIO examplesIO = ExamplesIO.getInstance();

    public static Set numSet = new HashSet();//has lock line num

    private static int firstLoc = 0, lastLoc = 0;

    public static void p(String lockName, ReadWriteNode readWriteNode, Sequence sequence, String sourceClassPath) {

        for (Node n : sequence.getNodes()) {
            if (n instanceof ReadWriteNode) {
                ReadWriteNode rwn = (ReadWriteNode) n;
                if (rwn.getElement().equals(readWriteNode.getElement()) && rwn.getField().equals(readWriteNode.getField())) {
                    int poi = Integer.parseInt(rwn.getPosition().split(":")[1]);
                    firstLoc = poi;
                    lastLoc = firstLoc;
                    //这行没处理过
                    if (!numSet.contains(poi)) {
                        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwn.getPosition().split(":")[0];
                        if (!hasSync(rwn, sequence, sourceClassPath) &&
                                !UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath)) {

                            //调整锁的范围
                            adjustSyncScope(analyseJavaPath);

                            if (firstLoc > 0 && lastLoc > 0) {
                                if (numSet.contains(firstLoc - 1) || numSet.contains(lastLoc + 1)) {
                                    //修改合并
                                    merge(lockName, analyseJavaPath);
                                } else {
                                    //加锁
                                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//待定
                                }
                            }
                            addSet(firstLoc, lastLoc);
                        }
                        numSet.add(poi);
                    }
                }
            }
        }
    }

    //合并
    private static void merge(String lockName, String analyseJavaPath) {
        int oldStart = 0, oldEnd = 0;
        boolean flagSearch = false;
        int now = 0, next = 0;
        for (Object o : numSet) {
            //找到与其重合的
            if (!flagSearch) {
                now = (int) o;
                if (now >= firstLoc && now <= (lastLoc + 1)) {
                    flagSearch = true;
                    oldStart = now;
                }
            } else {
                next = (int) o;
                if ((next - now) != 1) {
                    oldEnd = now;
                } else {
                    now = next;
                }
            }
        }

        //oldStart是最后一个元素
        if (oldEnd == 0)
            oldEnd = now;

        firstLoc = Math.min(firstLoc, oldStart);
        lastLoc = Math.max(lastLoc, oldEnd);
//        System.out.println(firstLoc + "," + lastLoc + "," + oldStart + "," + oldEnd + ">>>>>");
//        System.exit(-1);
        MergePro.merge(firstLoc, lastLoc, oldStart, oldEnd, lockName, analyseJavaPath);
    }

    private static boolean hasSync(ReadWriteNode rwn, Sequence sequence, String sourceClassPath) {
        if (LockPObject.element.equals("null")) {
            return false;
        }
        for (Node n : sequence.getNodes()) {
            if (n instanceof ReadWriteNode) {
                ReadWriteNode rw = (ReadWriteNode) n;
                if (rw.equals(rwn)) {
                    break;
                }
            }
            if (n instanceof LockNode) {
                LockNode ln = (LockNode) n;
                if (ln.getLockElement().equals(LockPObject.element) && ln.getThread().equals(rwn.getThread()) && ln.getAcqOrRel().equals("acq")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void addSet(int firstLoc, int lastLoc) {
        for (int i = firstLoc; i <= lastLoc; i++) {
            numSet.add(i);
        }
    }

    public static void addSet(int poi) {
        numSet.add(poi);
    }

    //更改锁的范围
    private static void adjustSyncScope(String analyseJavaPath) {

        //判断加锁会不会和for循环等交叉
        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();

        //检查会不会定义变量在锁内，使用变量在锁外
        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();
    }

    //清空
    public static void clearSet() {
        numSet.clear();
    }
}
