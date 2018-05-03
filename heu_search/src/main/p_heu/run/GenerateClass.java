package p_heu.run;

import fix.entity.ImportPath;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GenerateClass {
    public static void main(String[] args) {

        compileJava(ImportPath.verifyPath + "\\exportExamples\\" + ImportPath.projectName, ImportPath.verifyPath + "\\generateClass");

    }

    public static void compileJava(String dirPath, String desk) {
        //源代码
        //遍历该目录下的所有java文件
        File file = new File(dirPath);
        File[] fileArr = file.listFiles();
        List<String> fileList = new ArrayList<String>();

        for (File f : fileArr) {
            fileList.add(f.toString());
        }
        compile(fileList, desk);

    }


    public static void compile(List<String> files, String dest){
        com.sun.tools.javac.Main javac = new com.sun.tools.javac.Main();


        String[] cpargs = new String[files.size() + 2];
        cpargs[0] = "-d";
        cpargs[1] = dest;
        for (int i = 2; i < cpargs.length; ++i) {
            cpargs[i] = files.get(i - 2);
        }
        int status = javac.compile(cpargs);

    }
}