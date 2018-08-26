package fix.entity;



/**
 * Store various paths
 */
public class ImportPath {
    //The parameter name to look for
    public static String parametersName = "amount";
    //examples path
    public static String examplesRootPath = "C:\\Users\\lhr\\Desktop\\pfix\\FixExamples";
//    public static String examplesRootPath = "C:\\Users\\lhr\\Desktop\\test\\Grail\\grail-jstamp-AJ\\bin";

    //package name
//    public static String projectName = "account";
    public static String projectName = "account";
    //run class name
    public static String mainClassName = "Main";
    //temp file path
    public static String tempFile = examplesRootPath + "\\temp.java";
    //test path for program
    public static String verifyPath = examplesRootPath;

    //unicorn's lib path
    public static String unicornLibPath = examplesRootPath + "\\lib\\additionalJAR\\unicornLib";

}
