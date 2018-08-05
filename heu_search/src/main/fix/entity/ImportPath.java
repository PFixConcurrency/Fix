package fix.entity;



/**
 * 存放要引入的项目的路径
 */
public class ImportPath {
    //要寻找的参数名称
    public static String parametersName = "amount";
    //要加载的examples工程根路径
    public static String examplesRootPath = "C:\\Users\\lhr\\Desktop\\pfix\\FixExamples";
//    public static String examplesRootPath = "C:\\Users\\lhr\\Desktop\\test\\Grail\\grail-jstamp-AJ\\bin";

    //具体到某个项目的包名称
//    public static String projectName = "account";
    public static String projectName = "accountsubtype";
    //这个项目的主类名称
    public static String mainClassName = "Main";
    //临时文件的目录，不用太在意，反正用完就删，但是要保证路径是对的
    public static String tempFile = examplesRootPath + "\\temp.java";
    //验证程序的路径
    public static String verifyPath = examplesRootPath;

    //unicorn使用lib路径
    public static String unicornLibPath = examplesRootPath + "\\lib\\additionalJAR\\unicornLib";

    //修复多少次不成功停止
    public static int stopCount = 5;

}
