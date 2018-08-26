package fix.entity.lock;

import java.util.Vector;

//Defines the LockPath class to hold the execution sequence after the lock is acquired
public class LocKSequence {
    public String lockName;
    public String threadName;
    public Vector<LockElement> sequence = new Vector<LockElement>();

    public LocKSequence(String lockName, String threadName) {
        this.lockName = lockName;
        this.threadName = threadName;
    }

    //Depending on the name entered, check if the current LockSequence is to be found
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

    //Return all the variables in the sequence, and de-weight
    public Vector<String> fieldOnSameLock(){
        Vector<String> resultVector = new Vector<String>();
        for(LockElement le : this.sequence){
            if(!resultVector.contains(le.field)){
                resultVector.add(le.field);
            }

        }
        return resultVector;
    }

    public void clearAll() {
        this.sequence.clear();
    }
}
