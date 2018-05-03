package fix.entity.lock;

import java.util.Vector;

//定义LockPath类，用来存放获取锁之后的执行序列
public class LocKSequence {
    public String lockName;
    public String threadName;
    public Vector<LockElement> sequence = new Vector<LockElement>();

    public LocKSequence(String lockName, String threadName) {
        this.lockName = lockName;
        this.threadName = threadName;
    }

    //根据输入的name，检查当前的LockSequence是否是需要找的
    public boolean matchField(String fieldName){
        for(LockElement le : this.sequence){
            if(le.field.equals(fieldName))
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public boolean equals(LocKSequence ls) {
        if(ls.lockName.equals(this.lockName) && ls.threadName.equals(this.threadName))
            return true;
        else
            return false;
    }

    //返回sequence里面所有的变量,去重
    public Vector<String> fieldOnSameLock(){
        Vector<String> resultVector = new Vector<String>();
        for(LockElement le : this.sequence){
            if(!resultVector.contains(le.field)){
                resultVector.add(le.field);//去重
            }

        }
        return resultVector;
    }

    public void clearAll() {
        this.sequence.clear();
    }
}
