package fix.io;

import fix.entity.ImportPath;

import java.io.*;

public class MergePro {
    static boolean flagTer = false;

    public static void merge(int firstLoc, int lastLoc, int oldStart, int oldEnd, String lockName, String analyseJavaPath) {
        String tempFile = ImportPath.tempFile;//��ʱ�ļ���Ŀ¼������̫���⣬���������ɾ
        FileToTempFile(analyseJavaPath, tempFile, lockName, firstLoc, lastLoc, oldStart, oldEnd);//��Դ�ļ��޸ĺ�д����ʱ�ļ�
        TempFileToFile(analyseJavaPath, tempFile);//����ʱ�ļ�д��
        deleteTempFile(tempFile);//ɾ����ʱ�ļ�
    }


    //ԭ�ļ��޸ĺ�д����ʱ�ļ�
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
                //��ֹfirstloc��oldstart��lastloc��oldEnd��ͬ
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

    //����ʱ�ļ����޸ĺ��������д��ԭ�ļ�
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

    //ɾ����ʱ�ļ�
    private static void deleteTempFile(String tempFile) {
        File file = new File(tempFile);
        file.delete();
    }

    public static void mergeLen4(String analyseJavaPath, String lockName) {
        String tempFile = ImportPath.tempFile;
        FileToTempFile(analyseJavaPath, tempFile, lockName);//��Դ�ļ��޸ĺ�д����ʱ�ļ�
        if(flagTer) {//�����н�����������д����
            TempFileToFile(analyseJavaPath, tempFile);//����ʱ�ļ�д��
        }
        deleteTempFile(tempFile);//ɾ����ʱ�ļ�
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
