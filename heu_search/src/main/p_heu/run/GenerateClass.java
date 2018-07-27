package p_heu.run;

import fix.entity.ImportPath;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GenerateClass {

    static List<String> globalFileList = new ArrayList<String>();

    public static void main(String[] args) {

        String projectPath = "./sourceProject/log4j";
        String generateClassPath = "./GenerateClass";

//        List<String> localFileList = new ArrayList<String>();
//        localFileList = getAllJavaFile(projectPath);

//        System.out.println(localFileList.size());
//        for (String f : localFileList){
//            System.out.println(f);
//        }

//        compileJava(projectPath, generateClassPath);
        compileJava(ImportPath.verifyPath + "\\exportExamples\\" + ImportPath.projectName, ImportPath.verifyPath + "\\generateClass");
    }

    public static int compileJava(String dirPath, String desk) {

        if(dirPath.contains(".")){
            dirPath = dirPath.replaceAll("\\.","/");
        }
//        System.out.println(dirPath);
        List<String> localFileList = new ArrayList<String>();
        localFileList = getAllJavaFile(dirPath);

        return compile(localFileList, desk);

    }


    public static List<String> getAllJavaFile(String strPath) {

        File dir = new File(strPath);
        File[] files = dir.listFiles();

        if (files != null) {

            for (int i = 0; i < files.length; i++) {

                String fileName = files[i].getName();
                if (files[i].isDirectory()) {

                    getAllJavaFile(files[i].getAbsolutePath());
                } else if (fileName.endsWith(".java")) {

                    globalFileList.add(files[i].getAbsolutePath());
                } else {
                    continue;
                }
            }

        }
        return globalFileList;
    }



    public static int compile(List<String> files, String dest){
        com.sun.tools.javac.Main javac = new com.sun.tools.javac.Main();


        String[] cpargs = new String[files.size() + 2];
        cpargs[0] = "-d";
        cpargs[1] = dest;
        for (int i = 2; i < cpargs.length; ++i) {
            cpargs[i] = files.get(i - 2);
        }
        int status = javac.compile(cpargs);
        return status;

    }
}