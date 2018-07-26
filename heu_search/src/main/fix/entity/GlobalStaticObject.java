package fix.entity;

import fix.io.ExamplesIO;

//这个类表示的是添加的全局共享变量
//借助它来修复长度为4的pattern
public class GlobalStaticObject {
    //单例
    private static GlobalStaticObject globalStaticObject = new GlobalStaticObject();
    private GlobalStaticObject() {
    }

    public static GlobalStaticObject getInstance() {
        return globalStaticObject;
    }
    public static String objectName = "";

    public static boolean isDefineObject = false;

    //在跨类搜索中记录方法名称
    public static String leftMethodName = "";//方法一的名称

    public static String rightMethodName = "";//方法二的名称
}
