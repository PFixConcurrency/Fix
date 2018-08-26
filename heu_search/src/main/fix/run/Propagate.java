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
                    //this line is not dealt with
                    if (!numSet.contains(poi)) {
                        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwn.getPosition().split(":")[0];
                        if (!hasSync(rwn, sequence, sourceClassPath) &&
                                !UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath)) {

                            //adjust sync
                            adjustSyncScope(analyseJavaPath);

                            if (firstLoc > 0 && lastLoc > 0) {
                                //The lock that has been added crosses the lock that has been added
                                if (numSet.contains(firstLoc - 1) || numSet.contains(lastLoc + 1)) {
                                    //Changes and merge
                                    merge(lockName, analyseJavaPath);
                                } else if (contians()) {//The new lock contains the added lock
                                    merge(lockName, analyseJavaPath);
                                } else {
                                    //add sync
                                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);
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

    //Check that the new lock contains the locked
    private static boolean contians() {
        for (Object o : numSet) {
            int poi = (Integer) o;
            if (poi >= firstLoc && poi <= (lastLoc + 1)) {
                return true;
            }
        }
        return false;
    }

    //merge
    private static void merge(String lockName, String analyseJavaPath) {
        int oldStart = 0, oldEnd = 0;
        boolean flagSearch = false;
        int now = 0, next = 0;
        for (Object o : numSet) {
            //find overlap
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

        //oldStart is last element
        if (oldEnd == 0)
            oldEnd = now;

        firstLoc = Math.min(firstLoc, oldStart);
        lastLoc = Math.max(lastLoc, oldEnd);

        MergePro.merge(firstLoc, lastLoc, oldStart, oldEnd, lockName, analyseJavaPath);

        //If the combination is completed and then interlocks with other locks, a merge is required
        if (numSet.contains(lastLoc + 1)) {
            MergePro.mergeFirstOrLast(analyseJavaPath, lastLoc + 1, 1);
        } else if (numSet.contains(firstLoc)) {
            MergePro.mergeFirstOrLast(analyseJavaPath, firstLoc, 0);
        }
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

    //adjust sync
    private static void adjustSyncScope(String analyseJavaPath) {

        //Determine if the lock will cross the for loop, etc
        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();

        //Will the variable be defined inside the lock and used outside
        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();
    }

    //clear out
    public static void clearSet() {
        numSet.clear();
    }
}
