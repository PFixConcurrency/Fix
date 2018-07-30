package fix.listener;

import fix.entity.lock.LocKSequence;
import fix.entity.lock.LockElement;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.jvm.bytecode.LockInstruction;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.*;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.bytecode.InstanceFieldInstruction;
import gov.nasa.jpf.vm.bytecode.LocalVariableInstruction;
import gov.nasa.jpf.vm.bytecode.StaticFieldInstruction;

import java.util.Vector;

public class LockListener2 extends PropertyListenerAdapter {

    public Vector<LocKSequence> lockVector = new Vector<LocKSequence>();//存放遇到的所有锁。
    private String filePath;//要输出的文件的地址
    private String fieldName;//要寻找的参数名一
    private String fieldLoc;// 变量的具体位置一
    //    private String fieldNameTwo;//要寻找的参数名二
//    private String fieldLocTwo;// 变量的具体位置二
    private boolean checkFlag = false;

    //此时正在保护共享变量的锁名
    private String protectLockName;


    public String getProtectLockName() {
        return protectLockName;
    }

    public boolean isCheckFlag() {
        return checkFlag;
    }

    public LockListener2() {
    }

    public LockListener2(String filePath, String fieldName, String fieldLoc) {
        this.filePath = filePath;
        this.fieldName = fieldName;
        this.fieldLoc = fieldLoc;
    }

    /*@Override
    public void objectWait(VM vm, ThreadInfo currentThread, ElementInfo waitingObject) {
        System.out.println("objectWait============" + waitingObject.toString());
    }

    @Override
    public void objectNotify(VM vm, ThreadInfo currentThread, ElementInfo notifyingObject) {
        System.out.println("objectNotify============" + notifyingObject.toString());
    }

    @Override
    public void objectNotifyAll(VM vm, ThreadInfo currentThread, ElementInfo notifyingObject) {
        System.out.println("objectNotifyAll============" + notifyingObject.toString());
    }*/


    @Override
    public void objectLocked(VM vm, ThreadInfo currentThread, ElementInfo lockedObject) {
//        System.out.println("acquire lock :" + lockedObject.toString() + "\t" + currentThread.getName() + "\t");
        System.out.println(lockedObject.toString() + "," + currentThread.getPC().getFileLocation() + "," + currentThread.getPC().getClass());
    }

    @Override
    public void objectUnlocked(VM vm, ThreadInfo currentThread, ElementInfo unlockedObject) {
        System.out.println(unlockedObject.toString() + "," + currentThread.getPC().getFileLocation() + "," + currentThread.getPC().getClass());

    }

    /* @Override
    public void objectUnlocked(VM vm, ThreadInfo currentThread, ElementInfo unlockedObject) {
        for (int i = lockVector.size() - 1; i >= 0; i--) {//从后往前找
            LocKSequence ls = lockVector.get(i);
            //找到当前对应当前释放的锁
            if (ls.lockName.equals(unlockedObject.toString()) && currentThread.getName().equals(ls.threadName)) {
                //寻找当前锁中有没有需要寻找的变量
                for (LockElement le : ls.sequence) {
                    //先检查有没变量
                    if (le.field.equals(fieldName) *//*&& le.location.equals(fieldLoc)*//*) {
                        System.out.println("*************" + le.toString());
                        System.out.println(lockVector.get(i).lockName);
                        checkFlag = true;
                        protectLockName = ls.lockName;
                        break;
                    }

                }
                //清空当前释放锁里面的内容
                lockVector.get(i).clearAll();
                lockVector.remove(i);
            }
        }
    }*/

    /*@Override
    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
        System.out.println(executedInstruction);
        if (executedInstruction instanceof FieldInstruction) {
            FieldInstruction fins = (FieldInstruction) executedInstruction;
            FieldInfo fi = fins.getFieldInfo();
            ElementInfo ei = fins.getElementInfo(currentThread);
            String res = fins.getFileLocation();

            System.out.println(fins.toString() + "   fi:" + fi + "    ei:" + ei + "    res:" + res);
        } else if(executedInstruction instanceof LockInstruction) {
            LockInstruction ls = (LockInstruction)executedInstruction;
            System.out.println(executedInstruction.getLineNumber() + "," + executedInstruction.getSourceOrLocation() + ","
                    + executedInstruction.getClass().toString());
            System.out.println("ls:" + ls.getLastLockRef() + "," + ls.toPostExecString());

        }
    }*/
}
