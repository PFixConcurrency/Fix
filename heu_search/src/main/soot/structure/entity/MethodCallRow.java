package soot.structure.entity;

import java.util.HashSet;
import java.util.Set;

/*
函数的调用者和调用发生的行
 */
public class MethodCallRow{

    private Method method;
    private int row;
    private boolean extend;

    private static Set<MethodCallRow> callRows = new HashSet<>();

    private MethodCallRow(Method method, int row) {
        this.method = method;
        this.row = row;
        this.extend = false;
    }

    public static MethodCallRow getInstance(Method method, int row) {
        for (MethodCallRow callRow : callRows) {
            if (callRow.getMethod().equals(method) && callRow.getRow() == row) {
                return callRow;
            }
        }
        MethodCallRow instance = new MethodCallRow(method, row);
        callRows.add(instance);
        return instance;
    }

    public Method getMethod() {
        return method;
    }

    public int getRow() {
        return row;
    }

    public boolean isExtend() {
        return extend;
    }

    public void setExtend(boolean extend) {
        this.extend = extend;
    }

    public boolean equals(MethodCallRow callRow) {
        return this.method.equals(callRow.getMethod()) && this.row == callRow.getRow();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[ method: ");
        builder.append(method);
        builder.append(", row: ");
        builder.append(row);
        builder.append(", extend: ");
        builder.append(extend);
        builder.append(" ]");
        return builder.toString();
    }
}
