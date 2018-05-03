package fix.analyzefile;

import fix.entity.ImportPath;

import java.io.*;

public class UseOldSyncToFix {
    public static void adjustOldSync(String lockName, int locStartLine, int locEndLine, int oldStartLine, int oldEndLine, String filePath) {
        String tempFile = ImportPath.tempFile;//临时文件的目录，不用太在意，反正用完就删
        FileToTempFile(lockName, locStartLine, locEndLine, oldStartLine, oldEndLine, filePath, tempFile);//将源文件修改后写入临时文件
        TempFileToFile(filePath, tempFile);//从临时文件写入
        deleteTempFile(tempFile);//删除临时文件
    }
    //原文件修改后写入临时文件
    private static void FileToTempFile(String lockName, int locStartLine, int locEndLine, int oldStartLine, int oldEndLine, String filePath, String tempFile) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        int line = 0;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            bw = new BufferedWriter(new FileWriter(new File(tempFile)));
            String read = "";
            while (((read = br.readLine()) != null)) {
                line++;
                //删除原有锁
                if (line == oldStartLine) {
                    int index = read.indexOf('{');
                    index++;
                    read = read.substring(index);
                }
                if (line == oldEndLine) {
                    int index = read.indexOf('}');
                    index++;
                    read = read.substring(index);
                }

                //修改锁位置锁
                //位置一定要在删除锁后面
                if (line == locStartLine) {
                    StringBuilder sb = new StringBuilder(read);
                    sb.insert(0, "synchronized (" + lockName + "){ ");
                    read = sb.toString();
                }
                if (line == locEndLine) {
                    StringBuilder sb = new StringBuilder(read);
                    sb.insert(0, "}");
                    read = sb.toString();
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

}
