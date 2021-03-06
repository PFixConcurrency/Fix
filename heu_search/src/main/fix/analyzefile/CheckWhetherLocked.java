package fix.analyzefile;

import fix.entity.ImportPath;
import fix.entity.LockPObject;
import fix.listener.CheckWhetherLockedListener;
import fix.listener.LockListener2;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import p_heu.entity.LockNode;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.sequence.Sequence;

public class CheckWhetherLocked {

    static boolean flagUseJPFCheckWhetherLock = false;//jpf check whether has sync

    //The form has to be "account/Account.java:32"
    public static void main(String[] args) {
//        System.out.println(check("atmoerror/BankAccount.java:11", "total", ImportPath.examplesRootPath + "/out/production/Patch"));
//        UseASTAnalysisClass.setFlagUseASTCheckWhetherLock(false);
    }

    public static boolean check(String variableLoc, String variableName, String classpath) {
        String[] str = new String[]{
//                "+classpath=" + ImportPath.examplesRootPath + "\\out\\production\\Patch",
                "+classpath=" + classpath,
                "+search.class=fix.search.SingleExecutionSearch",
                ImportPath.projectName + "." + ImportPath.mainClassName
        };
        Config config = new Config(str);
        JPF jpf = new JPF(config);
        CheckWhetherLockedListener checkWhetherLockedListener = new CheckWhetherLockedListener(ImportPath.examplesRootPath + "\\examples\\lock.txt", variableName, variableLoc);
//        LockListener2 checkWhetherLockedListener = new LockListener2(ImportPath.examplesRootPath + "\\examples\\lock.txt", variableName, variableLoc);
        jpf.addListener(checkWhetherLockedListener);
//        LockListener lockListener = new LockListener(ImportPath.examplesRootPath + "\\examples\\lock.txt","test");
//        jpf.addListener(lockListener);
        jpf.run();
//        System.out.println(checkWhetherLockedListener.isCheckFlag());
        flagUseJPFCheckWhetherLock = checkWhetherLockedListener.isCheckFlag();
        //One needs to process the clas file
        //One for Java file processing
        //because jpf can't analysesynchronized (this) {
        //                tmp = new MyListNode(x, p._current._next);
        //            }
        //AST can't analyse synchronized Function
        //so we combine


        return flagUseJPFCheckWhetherLock;
    }


    public static boolean checkHasSync(ReadWriteNode rwn, Sequence sequence) {
        if (LockPObject.element.equals("null")) {
            return false;
        }
        String targetFile = rwn.getPosition().split(":")[0];
        for (Node n : sequence.getNodes()) {
            if (n instanceof ReadWriteNode) {
                ReadWriteNode rw = (ReadWriteNode) n;
                if (rw.equals(rwn)) {
                    break;
                }
            }
            if (n instanceof LockNode) {
                LockNode ln = (LockNode) n;
                String nowFile = ln.getLocation().split(":")[0];
                if (ln.getLockElement().equals(LockPObject.element) && ln.getThread().equals(rwn.getThread()) &&
                        ln.getAcqOrRel().equals("acq") && nowFile.equals(targetFile)) {
                    return true;
                }
            }
        }
        return false;
    }

}
