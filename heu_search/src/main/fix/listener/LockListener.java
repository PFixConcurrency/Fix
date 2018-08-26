package fix.listener;

import fix.entity.lock.LocKSequence;
import fix.entity.lock.LockElement;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.*;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;

import java.io.*;
import java.util.Vector;


public class LockListener extends PropertyListenerAdapter {
    public Vector<LocKSequence> LockVector = new Vector<LocKSequence>();
    private String filePath;
    private String fieldName;
    private Vector<String> oneLockfieldVector = new Vector<String>();

    public Vector<String> getOneLockfieldVector() {
        return oneLockfieldVector;
    }


    public LockListener(String fieldName) {
        super();
        this.fieldName = fieldName;
    }


    public LockListener(String filePath, String fieldName) {
        super();
        this.filePath = filePath;
        this.fieldName = fieldName;
    }

    @Override
    public void objectLocked(VM vm, ThreadInfo currentThread, ElementInfo lockedObject) {
        LocKSequence locKSequence = new LocKSequence(lockedObject.toString(),currentThread.getName());
        LockVector.add(locKSequence);

    }

    @Override
    public void searchStarted(Search search) {
        File f = new File(filePath);
        if(f.exists())
            f.delete();
    }

    @Override
    public void searchFinished(Search search) {

    }

    @Override
    public void objectUnlocked(VM vm, ThreadInfo currentThread, ElementInfo unlockedObject) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath,true)));
            for(int i = LockVector.size() - 1; i >= 0; i--){
                LocKSequence ls = LockVector.get(i);

                if(ls.lockName.equals(unlockedObject.toString())&& currentThread.getName().equals(ls.threadName)){

                    bw.write(ls.lockName + "\t" + ls.threadName + "\n");
                    for(LockElement le : ls.sequence){
                        bw.write(le.toString() + "\n");
                    }
                    bw.write("-----------------\n");


                    if(ls.matchField(fieldName)){
                        Vector<String> v = ls.fieldOnSameLock();
                        for(String s : v){
                            if(!oneLockfieldVector.contains(s)){
                                oneLockfieldVector.add(s);
                            }
                        }

                    }


//					ls.sequence.clear();
                    break;
                }
            }
            bw.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try {
                bw.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    @Override
    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction,
                                    Instruction executedInstruction) {
        if (executedInstruction instanceof FieldInstruction) {
            FieldInstruction fins = (FieldInstruction)executedInstruction;
            FieldInfo fi = fins.getFieldInfo();
            ElementInfo ei = fins.getElementInfo(currentThread);
            for(int i = LockVector.size() - 1; i >= 0; i--){
                LocKSequence ls = LockVector.get(i);
                if(ls.lockName.equals(ei.toString()) && currentThread.getName().equals(ls.threadName)){
                    ls.sequence.add(new LockElement(ei.toString(), fi.getName(), currentThread.getName(), fins.getFileLocation()));
                    break;
                }
            }
        }
    }

}
