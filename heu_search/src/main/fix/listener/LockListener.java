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
    public Vector<LocKSequence> LockVector = new Vector<LocKSequence>();//存放遇到的所有锁。
    private String filePath;//要输出的文件的地址
    private String fieldName;//要寻找的参数名
    private Vector<String> oneLockfieldVector = new Vector<String>();

    public Vector<String> getOneLockfieldVector() {
        return oneLockfieldVector;
    }


    public LockListener(String fieldName) {
        super();
        this.fieldName = fieldName;
    }


    public LockListener(String filePath, String fieldName) {//给定一个变量
        super();
        this.filePath = filePath;
        this.fieldName = fieldName;
    }

    @Override
    public void objectLocked(VM vm, ThreadInfo currentThread, ElementInfo lockedObject) {
//		System.out.println("输出加锁:" + lockedObject.toString() + "," + currentThread.getName());
        LocKSequence locKSequence = new LocKSequence(lockedObject.toString(),currentThread.getName());
        LockVector.add(locKSequence);

    }

    @Override
    public void searchStarted(Search search) {
        //每次启动前，检查是否有上次的文件残留
        File f = new File(filePath);
        if(f.exists())
            f.delete();
    }

    @Override
    public void searchFinished(Search search) {

    }

    @Override
    public void objectUnlocked(VM vm, ThreadInfo currentThread, ElementInfo unlockedObject) {
//		System.out.println("输出释放锁:" + unlockedObject.toString() + "," + currentThread.getName());
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath,true)));//往file里面增加内容
            for(int i = LockVector.size() - 1; i >= 0; i--){//从后往前找
                LocKSequence ls = LockVector.get(i);
                //对应当前释放的锁
                if(ls.lockName.equals(unlockedObject.toString())&& currentThread.getName().equals(ls.threadName)){
                    //写入文件
                    bw.write(ls.lockName + "\t" + ls.threadName + "\n");
                    for(LockElement le : ls.sequence){
                        bw.write(le.toString() + "\n");
                    }
                    bw.write("-----------------\n");

                    //返回变量名
                    if(ls.matchField(fieldName)){
                        Vector<String> v = ls.fieldOnSameLock();
                        for(String s : v){
                            if(!oneLockfieldVector.contains(s)){
                                oneLockfieldVector.add(s);//去重加入
                            }
                        }

                    }

                    //清空对应的sequence
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
            for(int i = LockVector.size() - 1; i >= 0; i--){//从后往前找
                LocKSequence ls = LockVector.get(i);
                if(ls.lockName.equals(ei.toString()) && currentThread.getName().equals(ls.threadName)){
//                    System.out.println("里面的是" + ei.toString() + "," + fi.getName() + "," + currentThread.getName() + "," + fins.getFileLocation());
                    ls.sequence.add(new LockElement(ei.toString(), fi.getName(), currentThread.getName(), fins.getFileLocation()));
                    break;
                }
            }
        }
    }

}
