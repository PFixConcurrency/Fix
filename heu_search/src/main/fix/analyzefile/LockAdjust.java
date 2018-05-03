package fix.analyzefile;

import fix.entity.ImportPath;

import java.io.*;

//在添加两个同步的时候，万一两个同步交叉，要合并
public class LockAdjust {
    //单例
    private static LockAdjust instance = new LockAdjust();

    private LockAdjust() {

    }

    public static LockAdjust getInstance() {
        return instance;
    }

    String oneLockFile = "";//第一次加锁的文件
    String oneLockName = "";//第一次加锁的名称
    int oneFirstLoc = 0;//第一次加锁位置
    int oneLastLoc = 0;//第一次加锁位置

    String twoLockFile = "";//第二次加锁的文件
    String twoLockName = "";//第二次加锁名称
    int twoFirstLoc = 0;//第二次加锁位置
    int twoLastLoc = 0;//第二次加锁位置

    public String getOneLockName() {
        return oneLockName;
    }

    public void setOneLockName(String oneLockName) {
        this.oneLockName = oneLockName;
    }

    public String getTwoLockName() {
        return twoLockName;
    }

    public void setTwoLockName(String twoLockName) {
        this.twoLockName = twoLockName;
    }

    boolean oneLockFinish = false;//第一次加锁是否完成

    int finalFirstLoc = 0;//合并后的位置
    int finalLastLoc = 0;//合并后的位置

    public String getOneLockFile() {
        return oneLockFile;
    }

    public void setOneLockFile(String oneLockFile) {
        this.oneLockFile = oneLockFile;
    }

    public int getOneFirstLoc() {
        return oneFirstLoc;
    }

    public void setOneFirstLoc(int oneFirstLoc) {
        this.oneFirstLoc = oneFirstLoc;
    }

    public int getOneLastLoc() {
        return oneLastLoc;
    }

    public void setOneLastLoc(int oneLastLoc) {
        this.oneLastLoc = oneLastLoc;
    }

    public String getTwoLockFile() {
        return twoLockFile;
    }

    public void setTwoLockFile(String twoLockFile) {
        this.twoLockFile = twoLockFile;
    }

    public int getTwoFirstLoc() {
        return twoFirstLoc;
    }

    public void setTwoFirstLoc(int twoFirstLoc) {
        this.twoFirstLoc = twoFirstLoc;
    }

    public int getTwoLastLoc() {
        return twoLastLoc;
    }

    public void setTwoLastLoc(int twoLastLoc) {
        this.twoLastLoc = twoLastLoc;
    }

    public boolean isOneLockFinish() {
        return oneLockFinish;
    }

    public void setOneLockFinish(boolean oneLockFinish) {
        this.oneLockFinish = oneLockFinish;
    }

    public int getFinalFirstLoc() {
        return finalFirstLoc;
    }

    public void setFinalFirstLoc(int finalFirstLoc) {
        this.finalFirstLoc = finalFirstLoc;
    }

    public int getFinalLastLoc() {
        return finalLastLoc;
    }

    public void setFinalLastLoc(int finalLastLoc) {
        this.finalLastLoc = finalLastLoc;
    }

    public static void main(String[] args) {
        LockAdjust la = new LockAdjust();
        la.setOneFirstLoc(12);
        la.setOneLastLoc(13);
        la.setTwoFirstLoc(12);
        la.setTwoLastLoc(13);
        la.setOneLockFile("this");
        la.setTwoLockFile("this");
        la.adjust("C:\\Users\\lhr\\Desktop\\a.java");
    }

    public void adjust(String filePath) {
        //后来我仔细想了想，不管锁是不是相同都要合并
        //只有在同一个文件里面加锁才能合并，都不同类了也不需要合并了
            if (cross() && oneLockFile.equals(twoLockFile)) {//如果交叉需要合并
                if (lastEqualFirst()) {//判断某次加锁终止行是不是和另一次加锁起始行相等
                    adjustOldSync(filePath, 1);//修改原有的锁
                } else {
                    finalFirstLoc = Math.min(oneFirstLoc, twoFirstLoc);
                    finalLastLoc = Math.max(oneLastLoc, twoLastLoc);
                    //删除原有锁，然后添加新的合并锁
                    adjustOldSync(filePath, 0);//0表示合并锁，1表示移动锁
                }
            }
//        }
    }

    //删除原有锁
    //第二个参数表示执行哪个操作
    //0表示合并锁
    //1表示移动锁
    private void adjustOldSync(String filePath, int type) {
        String tempFile = ImportPath.tempFile;//临时文件的目录，不用太在意，反正用完就删
        FileToTempFile(filePath, tempFile, type);//将源文件修改后写入临时文件
        TempFileToFile(filePath, tempFile);//从临时文件写入
        deleteTempFile(tempFile);//删除临时文件
    }

    //原文件修改后写入临时文件
    private void FileToTempFile(String filePath, String tempFile, int type) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        int line = 0;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            bw = new BufferedWriter(new FileWriter(new File(tempFile)));
            String read = "";
            while (((read = br.readLine()) != null)) {
                line++;
                //执行合并锁操作
                if (type == 0) {
                    //删除第一个锁
                    if (line == oneFirstLoc) {
                        int index = read.indexOf('{');
                        index++;
                        read = read.substring(index);
                    }
                    if (line == oneLastLoc) {
                        int index = read.indexOf('}');
                        index++;
                        read = read.substring(index);
                    }

                    //删除第二个锁
                    if (line == twoFirstLoc) {
                        int index = read.indexOf('{');
                        index++;
                        read = read.substring(index);
                    }
                    if (line == twoLastLoc) {
                        int index = read.indexOf('}');
                        index++;
                        read = read.substring(index);
                    }

                    //添加合并后的锁
                    //位置一定要在删除锁后面
                    if (line == finalFirstLoc) {
                        StringBuilder sb = new StringBuilder(read);
                        sb.insert(0, "synchronized (" + oneLockName + "){ ");
                        read = sb.toString();
                    }
                    if (line == finalLastLoc) {
                        StringBuilder sb = new StringBuilder(read);
                        sb.insert(0, "}");
                        read = sb.toString();
                    }
                } else if (type == 1) {//执行移动锁操作
                    int equalLineNumber = 0;//相等行号
                    if (oneLastLoc == twoFirstLoc) {//1和2靠着
                        equalLineNumber = twoFirstLoc;
                    } else if (twoLastLoc == oneFirstLoc) {//2和1靠着
                        equalLineNumber = oneFirstLoc;
                    }
                    if (line == equalLineNumber) {
                        //这段代码原来的逻辑是这样
                        //如果相邻两行加锁，则加它们修改一下，避免出现第二次加锁的左括号和第一次加锁的右括号重合
                        /*int index = read.indexOf('}');//找到上个加锁的右括号
                        read = read.substring(0, index) + read.substring(index + 1);//删除右括号
                        StringBuilder sb = new StringBuilder(read);
                        sb.insert(0, '}');//在行首添加一个右括号
                        read = sb.toString();*/

                        //但是现在，在运行even程序的时候，发现将相邻的加锁两行合并才好
                        //所以改成了这样，直接删除
                        int index = read.indexOf('{');//找到上个加锁的右括号
                        read = read.substring(index + 1);//直接把这部分都删了
                    }
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
    private void TempFileToFile(String filePath, String tempFile) {
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
    private void deleteTempFile(String tempFile) {
        File file = new File(tempFile);
        file.delete();
    }

    //判断是不是交叉
    private boolean cross() {
        //先找不交叉的情况
        if (oneFirstLoc > twoLastLoc) {
            return false;
        }
        if (oneLastLoc < twoFirstLoc) {
            return false;
        }
        return true;
    }

    //判断加锁终止行是不是和另一次加锁起始行相等
    private boolean lastEqualFirst() {
        if (oneLastLoc == twoFirstLoc) {
            return true;
        }
        if (twoLastLoc == oneFirstLoc) {
            return true;
        }
        return false;
    }
}

