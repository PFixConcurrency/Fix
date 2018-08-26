package soot.structure.entity;

/*
Storage requires the location where the lock is ultimately required
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
