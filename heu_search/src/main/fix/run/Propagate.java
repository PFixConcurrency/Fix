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
                    //����û�����
                    if (!numSet.contains(poi)) {
                        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwn.getPosition().split(":")[0];
                        if (!hasSync(rwn, sequence, sourceClassPath) &&
                                !UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath)) {

                            //�������ķ�Χ
                            adjustSyncScope(analyseJavaPath);

                            if (firstLoc > 0 && lastLoc > 0) {
                                if (numSet.contains(firstLoc - 1) || numSet.contains(lastLoc + 1)) {
                                    //�޸ĺϲ�
                                    merge(lockName, analyseJavaPath);
                                } else {
                                    //����
                                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//����
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

    //�ϲ�
    private static void merge(String lockName, String analyseJavaPath) {
        int oldStart = 0, oldEnd = 0;
        boolean flagSearch = false;
        int now = 0, next = 0;
        for (Object o : numSet) {
            //�ҵ������غϵ�
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

        //oldStart�����һ��Ԫ��
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

    //�������ķ�Χ
    private static void adjustSyncScope(String analyseJavaPath) {

        //�жϼ����᲻���forѭ���Ƚ���
        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();

        //���᲻�ᶨ����������ڣ�ʹ�ñ���������
        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();
    }

    //���
    public static void clearSet() {
        numSet.clear();
    }
}
