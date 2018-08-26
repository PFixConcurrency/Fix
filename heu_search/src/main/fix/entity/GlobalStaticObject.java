package fix.entity;

import fix.io.ExamplesIO;

//This class represents the added global Shared variable

public class GlobalStaticObject {
    //singleton
    private static GlobalStaticObject globalStaticObject = new GlobalStaticObject();
    private GlobalStaticObject() {
    }

    public static GlobalStaticObject getInstance() {
        return globalStaticObject;
    }
    public static String objectName = "";

    public static boolean isDefineObject = false;

    //Record the method name in cross function search
    public static String leftMethodName = "";//function one name

    public static String rightMethodName = "";//function two name
}
