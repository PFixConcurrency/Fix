package fix.entity;

import fix.io.ExamplesIO;

//������ʾ������ӵ�ȫ�ֹ������
//���������޸�����Ϊ4��pattern
public class GlobalStaticObject {
    //����
    private static GlobalStaticObject globalStaticObject = new GlobalStaticObject();
    private GlobalStaticObject() {
    }

    public static GlobalStaticObject getInstance() {
        return globalStaticObject;
    }
    public static String objectName = "";

    public static boolean isDefineObject = false;

    //�ڿ��������м�¼��������
    public static String leftMethodName = "";//����һ������

    public static String rightMethodName = "";//������������
}
