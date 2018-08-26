package p_heu.entity;

import java.util.HashMap;

//reorder sync
public class LockNode  extends Node {
    private String thread;
    private String lockElement;
    private String location;
    private String type;
    private String acqOrRel;

    public String getThread() {
        return thread;
    }

    public String getLockElement() {
        return lockElement;
    }

    public String getLocation() {
        return location;
    }

    public String getType() {
        return type;
    }

    public String getAcqOrRel() {
        return acqOrRel;
    }

    public LockNode(int id, String thread, String lockElement, String location, String type, String acqOrRel) {
        this.id = id;
        this.thread = thread;
        this.lockElement = lockElement;
        this.location = location;
        this.type = type;
        this.acqOrRel = acqOrRel;
    }

    @Override
    public boolean isIdentical(Node node) {
        if (node instanceof LockNode) {
            LockNode lockNode = (LockNode)node;
            return this.thread.equals(lockNode.getThread()) && this.lockElement.equals(lockNode.getLockElement())
                    && this.location.equals(lockNode.getLocation()) && this.type.equals(lockNode.getType());
        }
        return false;
    }

    @Override
    public boolean isSame(Node node) {
        if (node instanceof LockNode) {
            LockNode lockNode = (LockNode)node;
            return this.thread.equals(lockNode.getThread()) && this.lockElement.equals(lockNode.getLockElement())
                    && this.location.equals(lockNode.getLocation()) && this.type.equals(lockNode.getType());
        }
        return false;
    }

    @Override
    public String toString() {
        return "LockNode[" + id + "," + thread + "," + lockElement + "," + location + "," + type + "," + acqOrRel + "]";
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LockNode)) {
            return false;
        }

        LockNode ln = (LockNode)obj;

        return thread.equals(ln.getThread())
                && lockElement.equals(ln.getLockElement())
                && location.equals(ln.getLocation())
                && type.equals(ln.getType())
                && acqOrRel.equals(ln.getAcqOrRel());
    }
}
