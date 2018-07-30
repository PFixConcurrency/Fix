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

    public Vector<LocKSequence> lockVector = new Vector<LocKSequence>();//�����������������
    private String filePath;//Ҫ������ļ��ĵ�ַ
    private String fieldName;//ҪѰ�ҵĲ�����һ
    private String fieldLoc;// �����ľ���λ��һ
    //    private String fieldNameTwo;//ҪѰ�ҵĲ�������
//    private String fieldLocTwo;// �����ľ���λ�ö�
    private boolean checkFlag = false;

    //��ʱ���ڱ����������������
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
        for (int i = lockVector.size() - 1; i >= 0; i--) {//�Ӻ���ǰ��
            LocKSequence ls = lockVector.get(i);
            //�ҵ���ǰ��Ӧ��ǰ�ͷŵ���
            if (ls.lockName.equals(unlockedObject.toString()) && currentThread.getName().equals(ls.threadName)) {
                //Ѱ�ҵ�ǰ������û����ҪѰ�ҵı���
                for (LockElement le : ls.sequence) {
                    //�ȼ����û����
                    if (le.field.equals(fieldName) *//*&& le.location.equals(fieldLoc)*//*) {
                        System.out.println("*************" + le.toString());
                        System.out.println(lockVector.get(i).lockName);
                        checkFlag = true;
                        protectLockName = ls.lockName;
                        break;
                    }

                }
                //��յ�ǰ�ͷ������������
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
