package soot.structure.entity;

/*
Find the function common call function and the line where the respective call is
 */
public class CommonCaller {

    private Method method;
    private int leftRow;
    private int rightRow;

    public CommonCaller(Method method, int leftRow, int rightRow) {
        this.method = method;
        this.leftRow = leftRow;
        this.rightRow = rightRow;
    }

    public Method getMethod() {
        return method;
    }

    public int getLeftRow() {
        return leftRow;
    }

    public int getRightRow() {
        return rightRow;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ method: ");
        builder.append(method);
        builder.append(", left: ");
        builder.append(leftRow);
        builder.append(", right: ");
        builder.append(rightRow);
        builder.append(" ]");
        return builder.toString();
    }
}
