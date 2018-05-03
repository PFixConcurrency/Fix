package fix.analyzefile;

import fix.entity.ImportPath;
import fix.entity.lock.ExistLock;
import p_heu.entity.ReadWriteNode;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//获取已有同步的名字
public class AcquireSyncName {


    public static void main(String[] args) {
        ReadWriteNode readWriteNode = new ReadWriteNode(2, "Account@1a7", "amount", "WRITE", "Thread-4", "account/Account.java:35");

        String filepath = ImportPath.examplesRootPath + "\\exportExamples\\" + ImportPath.projectName + "\\Account.java";
        ExistLock existLock = UseASTAnalysisClass.useASTCFindLockLine(readWriteNode, ImportPath.examplesRootPath + "\\exportExamples\\" + ImportPath.projectName + "\\Account.java");
        existLock = acquireSync(existLock,filepath);
        System.out.println(existLock.getLockName());
        System.out.println(existLock.getStartLine());
        System.out.println(existLock.getEndLine());
    }

    public static ExistLock acquireSync(ExistLock existLock, String filepath) {
        FileReader fileReader = null;
        LineNumberReader reader = null;
        String result = "";
        try {
            fileReader = new FileReader(new File(filepath));
            reader = new LineNumberReader(fileReader);
//        number = 32;//设置指定行数
            String txt = "";
            int lines = 0;
            //锁
            int syncLine = 0;
            String syncName = "";
            while (txt != null) {
                lines++;
                txt = reader.readLine();
                String s = txt;
                if(txt.contains("synchronized") && lines > syncLine){
                    syncLine = lines;
                    syncName = txt;
                }
                if (lines == existLock.getStartLine()) {
//                System.out.println("第" + reader.getLineNumber() + "的内容是：" + txt + "\n");
//                System.out.println("锁行数" + syncLine + ",锁信息" + syncName + "\n");
                    String lockName =  acquireName(syncName);
                    existLock.setLockName(lockName);
                    return existLock;
//                long timeEnd = System.currentTimeMillis();
//                System.out.println("总共花费：" + (timeEnd - timeStart) + "ms");

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static String acquireName(String syncName) {
        String name = "";
        Pattern p = Pattern.compile("^\\s+synchronized\\s*\\((\\w+)\\)\\s*\\{");
        Matcher m = p.matcher(syncName);
        if (m.matches()) {
            name = m.group(1);
        } else {
            name = "this";
        }
        return name;

    }
}
