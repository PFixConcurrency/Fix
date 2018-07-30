package fix.entity;

import p_heu.entity.LockNode;

import java.util.HashMap;
import java.util.List;

public class LockNodeAndNum {
    LockNode lockNode = null;
    int appearNum = 0;

    public LockNodeAndNum(LockNode lockNode, int appearNum) {
        this.lockNode = lockNode;
        this.appearNum = appearNum;
    }

    public LockNode getLockNode() {
        return lockNode;
    }

    public void setLockNode(LockNode lockNode) {
        this.lockNode = lockNode;
    }

    public int getAppearNum() {
        return appearNum;
    }

    public void setAppearNum(int appearNum) {
        this.appearNum = appearNum;
    }

    public void add(){
        this.appearNum = this.appearNum + 1;
    }
}
