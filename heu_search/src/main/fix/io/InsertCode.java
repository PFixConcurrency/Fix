package fix.io;

import fix.entity.ImportPath;

import java.io.*;
import java.util.Vector;

public class InsertCode {

    public static void insertLockDeclaration(String filepath){
        BufferedReader br = null;
        String read ="";
        int line = 1;
        Vector<Integer> v = new Vector<Integer>();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filepath)),"GBK"));
            while (((read = br.readLine()) != null)) {
                if(read.contains("class")){
                    v.add(++line);
//	            	   line++;
                }
                line++;
            }
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for(Integer i : v)
            insert(i, "ReentrantLock lock = new ReentrantLock(true);", filepath);
    }
    public static void insert(int line, String code, String filepath) {
        BufferedReader br = null;
        String read = "";
        String readBeforeLine = "";
        String readAfterLine = "";
        int i = 1;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filepath)),"GBK"));
            while (((read = br.readLine()) != null)) {
                if(i < line){
                    readBeforeLine += read;
                    readBeforeLine += '\n';
                }
                else{
                    readAfterLine += read;
                    readAfterLine += '\n';
                }
                i++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
/*        //删除
        File f = new File(filepath);
        f.delete();
        //重写入*/

        writeToFile(readBeforeLine + code  + readAfterLine, filepath);
    }

    public static void writeLogFile(String s, String txtName) {
        String dirPath = ImportPath.examplesRootPath + "\\logFile\\" + ImportPath.projectName;
        File dir = new File(dirPath);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        writeToFile(s, dirPath + "\\" + txtName + ".txt");
    }

    public static void writeToFile(String s, String fileName) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(new File(fileName)));
            bw.write(s);
            bw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
