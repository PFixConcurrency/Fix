package soot.structure.entity;
/*
记录一个函数
 */
public class Method {

    private String className;
    private String signature;

    public Method(String className, String signature) {
        this.className = className;
        this.signature = signature;
    }

    public String getClassName() {
        return className;
    }

    public String getSignature() {
        return signature;
    }

    public boolean equals(Method method) {
        return this.className.equals(method.getClassName())
                && this.getSignature().equals(method.getSignature());
    }

    public String toString() {
        return "[ " + className + ", " + signature + " ]";
    }
}
