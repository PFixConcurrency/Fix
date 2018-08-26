package fix.entity.type;

//To prevent
//synchronized (objectFix){ synchronized (objectFix){     	 a = a + b;
// } }    }
public class RelevantVarLockLine {
    public  int startLine = 0;
    public  int endLine = 0;

    public RelevantVarLockLine(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
    }
}
