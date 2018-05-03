package fix.io;

import java.io.*;

public class ExamplesIO {

    //单例模式
    private static ExamplesIO examplesIO = new ExamplesIO();

    private ExamplesIO() {
    }

    public static ExamplesIO getInstance() {
        return examplesIO;
    }

    //添加静态变量
    public void addStaticObject(int line, String objectName, String filePath){
        String context = "static Object " + objectName + " = new Object();";
        InsertCode.insert(line, context, filePath);
    }

    //对一个变量加锁
    public void addLockToOneVar(int startLine, int endLine, String lockName, String filePath) {
        InsertCode.insert(startLine, "synchronized (" + lockName + "){ ", filePath);
        InsertCode.insert(endLine, "}", filePath);
    }

    //添加信号量的定义
    public void addVolatileDefine(int line, String volatileName, String filePath) {
        InsertCode.insert(line, volatileName, filePath);
    }

    //添加信号量为true
    public void addVolatileToTrue(int line, String filePath) {
        InsertCode.insert(line, "flagFix = true;", filePath);
    }

    //添加信号量判断
    public void addVolatileIf(int line, String filePath) {
        InsertCode.insert(line, "if(flagFix){", filePath);
        InsertCode.insert(line + 1, "}", filePath);
    }

    //将文件拷贝到另一个位置，并且修改path值
    //将源程序拷贝到另一个工程目录下
    public String copyFromOneDirToAnotherAndChangeFilePath(String dir, String targetDir, String dirPath) {
        String changeFilePath = "";
        File file = new File(dirPath);
//        File[] fileArr = file.listFiles();
        //先创建一个copy目录
        changeFilePath = dirPath.replaceAll(dir, targetDir);
        File target = createDirectory(changeFilePath);

        try {
            copyFolder(file, target);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return changeFilePath;
    }

    private static void copyFolder(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }
            String files[] = src.list();
            for (String file : files) {
                File srcFile = new File(src, file);
                File destFile = new File(dest, file);
                // 递归复制
                copyFolder(srcFile, destFile);
            }
        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;

            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.close();
        }
    }

    private File createDirectory(String dirpath) {
        File dir = new File(dirpath);
        dir.mkdirs();
        return dir;
    }
}
