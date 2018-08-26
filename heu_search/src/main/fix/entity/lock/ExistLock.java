package fix.entity.lock;

//Used to represent the original lock in a program
public class ExistLock {
    String lockName = "";
    int startLine = 0;
    int endLine = 0;

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public ExistLock() {
    }

    public ExistLock(String lockName, int startLine, int endLine) {
        this.lockName = lockName;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public ExistLock(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }
}
