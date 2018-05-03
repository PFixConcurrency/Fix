package fix.entity.lock;

//用来表示程序中原有的锁
public class ExistLock {
    String lockName = "";//锁名
    int startLine = 0;//加锁开始行
    int endLine = 0;//加锁结束行

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
}
