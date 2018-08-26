package fix.io;

import java.io.*;

public class ExamplesIO {

    //singleton
    private static ExamplesIO examplesIO = new ExamplesIO();

    private ExamplesIO() {
    }

    public static ExamplesIO getInstance() {
        return examplesIO;
    }

    //Add static variables
    public void addStaticObject(int line, String objectName, String filePath){
        String context = "static Object " + objectName + " = new Object();";
        InsertCode.insert(line, context, filePath);
    }

    //add sync to a variable
    public void addLockToOneVar(int startLine, int endLine, String lockName, String filePath) {
        InsertCode.insert(startLine, "synchronized (" + lockName + "){ ", filePath);
        InsertCode.insert(endLine, "}", filePath);
    }

    //Add signal definition
    public void addVolatileDefine(int line, String volatileName, String filePath) {
        InsertCode.insert(line, volatileName, filePath);
    }

    //Add signal to true
    public void addVolatileToTrue(int line, String filePath) {
        InsertCode.insert(line, "flagFix = true;", filePath);
    }

    //Add signal judgment
    public void addVolatileIf(int line, String filePath) {
        InsertCode.insert(line, "while(!flagFix);", filePath);
    }

    //Copy the file to another location and change the path value
    //Copy the source program to another project directory
    public String copyFromOneDirToAnotherAndChangeFilePath(String dir, String targetDir, String dirPath) {
        String changeFilePath = "";
        File file = new File(dirPath);
//        File[] fileArr = file.listFiles();
        //Start by creating a copy directory
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
                // recursive copy
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
