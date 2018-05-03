package fix.entity.type;

public class AddSyncType {
    //加this锁
    public static int localSync = 0;
    //加全局静态锁
    public static int globalStaticSync = 1;
}
