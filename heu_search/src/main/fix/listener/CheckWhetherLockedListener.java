package fix.listener;

import fix.entity.lock.LocKSequence;
import fix.entity.lock.LockElement;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.*;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;

import java.util.Vector;

public class CheckWhetherLockedListener extends PropertyListenerAdapter {
    public Vector<LocKSequence> lockVector = new Vector<LocKSequence>();//Store all locks we find
    private String filePath;//The address of the file to output
    private String fieldName;//The parameter to look for is name one
    private String fieldLoc;// The exact location of the variable 1

    private boolean checkFlag = false;

    //The lock name of the Shared variable is being protected
    private String protectLockName;


    public String getProtectLockName() {
        return protectLockName;
    }

    public boolean isCheckFlag() {
        return checkFlag;
    }

    public CheckWhetherLockedListener() {
    }

    public CheckWhetherLockedListener(String filePath, String fieldName, String fieldLoc) {
        this.filePath = filePath;
        this.fieldName = fieldName;
        this.fieldLoc = fieldLoc;
    }

    @Override
    public void objectLocked(VM vm, ThreadInfo currentThread, ElementInfo lockedObject) {
        LocKSequence locKSequence = new LocKSequence(lockedObject.toString(), currentThread.getName());
        lockVector.add(locKSequence);
    }


    @Override
    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
        if (executedInstruction instanceof FieldInstruction) {
            FieldInstruction fins = (FieldInstruction) executedInstruction;
            FieldInfo fi = fins.getFieldInfo();
            ElementInfo ei = fins.getElementInfo(currentThread);
            String res = fins.getFileLocation();

            boolean flag = true;

           /* String[] className = res.split("/");
            System.out.println("hah"+className[className.length -  1]);
            if(className[className.length -  1].contains("Test") *//*&& lockVector.size() > 0*//*){

            }*/
           for (int i = lockVector.size() - 1; i >= 0; i--) {
                LocKSequence ls = lockVector.get(i);
                if (ls.lockName.equals(ei.toString()) && currentThread.getName().equals(ls.threadName)) {
//                    System.out.println("lock loc:" + res);
//                    System.out.println("lock name:" + ls.lockName);
                    ls.sequence.add(new LockElement(ei.toString(), fi.getName(), currentThread.getName(), fins.getFileLocation()));
                    break;
                }
            }
        } else {

        }
    }


    @Override
    public void objectUnlocked(VM vm, ThreadInfo currentThread, ElementInfo unlockedObject) {
        for (int i = lockVector.size() - 1; i >= 0; i--) {
            LocKSequence ls = lockVector.get(i);

            if (ls.lockName.equals(unlockedObject.toString()) && currentThread.getName().equals(ls.threadName)) {

                for (LockElement le : ls.sequence) {
                    if (le.field.equals(fieldName) && le.location.equals(fieldLoc)) {
//                        System.out.println("*************" + le.toString());
                        System.out.println(ls.lockName + "\t" + unlockedObject.toString() + "\t" + ls.threadName);
                        checkFlag = true;
                        protectLockName = ls.lockName;
                        break;
//                        System.out.println("1有");
                    }

                }
                lockVector.get(i).clearAll();
                lockVector.remove(i);
            }
        }
    }
}