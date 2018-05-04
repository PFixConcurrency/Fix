package fix.entity.type;

//已经为关联变量添加的锁
//这个类的使用是为了防止出现
//synchronized (objectFix){ synchronized (objectFix){     	System.out.println("a = " + a + " b = " + b);
//	return (a + b == 0);
// } }    }
public class RelevantVarLockLine {
    public  int startLine = 0;
    public  int endLine = 0;

    public RelevantVarLockLine(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }
}
