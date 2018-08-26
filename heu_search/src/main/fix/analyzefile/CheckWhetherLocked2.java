package fix.analyzefile;

import fix.entity.ImportPath;
import fix.listener.CheckWhetherLockedListener;
import fix.listener.LockListener2;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;

public class CheckWhetherLocked2 {

    static boolean flagUseJPFCheckWhetherLock = false;//jpf check whether has sync

    //The form has to be "account/Account.java:32"
    public static void main(String[] args) {
        System.out.println(check("test/Test.java:13","a", ImportPath.examplesRootPath + "/out/production/Patch","D:\\Patch\\examples\\wrongLock\\WrongLock.java"));
//        UseASTAnalysisClass.setFlagUseASTCheckWhetherLock(false);
    }
    public static boolean check(String variableLoc, String variableName, String classpath, String javaFilePath) {
        String[] str = new String[]{
//                "+classpath=" + ImportPath.examplesRootPath + "\\out\\production\\Patch",
                "+classpath=" + classpath,
                "+search.class=fix.search.SingleExecutionSearch",
                ImportPath.projectName + "." + ImportPath.mainClassName
        };
        Config config = new Config(str);
        JPF jpf = new JPF(config);
//        CheckWhetherLockedListener checkWhetherLockedListener = new CheckWhetherLockedListener(ImportPath.examplesRootPath + "\\examples\\lock.txt", variableName, variableLoc);
        LockListener2 checkWhetherLockedListener = new LockListener2(ImportPath.examplesRootPath + "\\examples\\lock.txt", variableName, variableLoc);
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

}
