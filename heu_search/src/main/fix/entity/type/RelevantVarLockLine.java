package fix.entity.type;

//�Ѿ�Ϊ����������ӵ���
//������ʹ����Ϊ�˷�ֹ����
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
