package fix.io;

import fix.entity.ImportPath;

import java.io.*;

public class MergePro {
    static boolean flagTer = false;

    public static void merge(int firstLoc, int lastLoc, int oldStart, int oldEnd, String lockName, String analyseJavaPath) {
        String tempFile = ImportPath.tempFile;//临时文件的目录，不用太在意，反正用完就删
        FileToTempFile(analyseJavaPath, tempFile, lockName, firstLoc, lastLoc, oldStart, oldEnd);//将源文件修改后写入临时文件
        TempFileToFile(analyseJavaPath, tempFile);//从临时文件写入
        deleteTempFile(tempFile);//删除临时文件
    }


    //原文件修改后写入临时文件
    private static void FileToTempFile(String filePath, String tempFile, String lockName, int firstLoc, int lastLoc, int oldStart, int oldEnd) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        int line = 0;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            bw = new BufferedWriter(new FileWriter(new File(tempFile)));
            String read = "";
            while (((read = br.readLine()) != null)) {
                line++;
                if (line == oldStart) {
                    int index = read.indexOf('{');
                    read = read.substring(index + 1);
                } else if (line == (oldEnd + 1)) {
                    int index = read.indexOf('}');
                    read = read.substring(index + 1);
                }
                //防止firstloc和oldstart，lastloc和oldEnd相同
                if(line == firstLoc){
                    read = "synchronized (" + lockName + "){" + read;
                }
                if(line == (lastLoc + 1)){
                    read = "}" + read;
                }
                bw.write(read);
                bw.write('\n');
                bw.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //从临时文件将修改后的内容再写入原文件
    private static void TempFileToFile(String filePath, String tempFile) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(tempFile)), "UTF-8"));
            bw = new BufferedWriter(new FileWriter(new File(filePath)));
            String read = "";
            while (((read = br.readLine()) != null)) {
                bw.write(read);
                bw.write('\n');
                bw.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //删除临时文件
    private static void deleteTempFile(String tempFile) {
        File file = new File(tempFile);
        file.delete();
    }

    public static void mergeLen4(String analyseJavaPath, String lockName) {
        String tempFile = ImportPath.tempFile;
        FileToTempFile(analyseJavaPath, tempFile, lockName);//将源文件修改后写入临时文件
        if(flagTer) {//不含有交叉的情况，不写入了
            TempFileToFile(analyseJavaPath, tempFile);//从临时文件写入
        }
        deleteTempFile(tempFile);//删除临时文件
    }

    private static void FileToTempFile(String filePath, String tempFile, String lockName) {
        BufferedReader br = null;
        BufferedWriter bw = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            bw = new BufferedWriter(new FileWriter(new File(tempFile)));
            String read = "";
            String target = "synchronized (" + lockName + "){  }";
            while (((read = br.readLine()) != null)) {
                if(read.contains(target)){
                    int index = read.indexOf("}");
                    read = read.substring(index + 1);
                    flagTer = true;
                }
                bw.write(read);
                bw.write('\n');
                bw.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
