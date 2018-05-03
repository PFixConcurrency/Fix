package soot.structure.entity;

/*
存储需要最终需要上锁的位置
 */
public class LockingPlace {

    private Method method;
    private int from;
    private int to;

    public LockingPlace(Method method, int from, int to) {
        this.method = method;
        this.from = from;
        this.to = to;
    }

    public Method getMethod() {
        return method;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }
}
