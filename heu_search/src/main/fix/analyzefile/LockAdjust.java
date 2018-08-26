package fix.analyzefile;

import fix.entity.ImportPath;

import java.io.*;

//When adding two synchronization, if corss,need to merge
public class LockAdjust {
    //singleton
    private static LockAdjust instance = new LockAdjust();

    private LockAdjust() {

    }

    public static LockAdjust getInstance() {
        return instance;
    }

    String oneLockFile = "";
    String oneLockName = "";
    int oneFirstLoc = 0;
    int oneLastLoc = 0;

    String twoLockFile = "";
    String twoLockName = "";
    int twoFirstLoc = 0;
    int twoLastLoc = 0;

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

    boolean oneLockFinish = false;//Is the first lock finished

    //location after merging
    int finalFirstLoc = 0;
    int finalLastLoc = 0;

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
//        System.out.println(oneFirstLoc + "," + oneLastLoc + "," + twoFirstLoc +"," + twoLastLoc + cross() +  "=================================");
        //Merge regardless of whether the lock is the same or not
        //Only lock in the same file can be merged
            if (cross() && oneLockFile.equals(twoLockFile)) {//If cross, needs to merge
                if (lastEqualFirst()) {//If one lock stop row is the same as another lock start row
                    adjustOldSync(filePath, 1);//Modify the old lock
                } else {
                    finalFirstLoc = Math.min(oneFirstLoc, twoFirstLoc);
                    finalLastLoc = Math.max(oneLastLoc, twoLastLoc);
                    //Remove the original sync and then add a new merge sync
                    adjustOldSync(filePath, 0);//0 merge sync，1 move sync
                }
                LockPolicyPopularize.firstLoc = finalFirstLoc;
                LockPolicyPopularize.lastLoc = finalLastLoc;
                LockPolicyPopularize.lockName = oneLockName;
                LockPolicyPopularize.flagCross = true;
            }
//        }
    }

    //delete old sync
    //operation type
    private void adjustOldSync(String filePath, int type) {
        String tempFile = ImportPath.tempFile;//Temporary file directory, do not care too much, delete after we use.
        FileToTempFile(filePath, tempFile, type);//Writes the source file to the temporary file
        TempFileToFile(filePath, tempFile);//Writes from a temporary file
        deleteTempFile(tempFile);//Delete temporary file
    }

    //The original file is modified and then written to the temporary file
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
                //Perform the merge lock operation
                if (type == 0) {
                    //Remove the first lock
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

                    //Remove the second lock
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

                    //Add the combined lock
                    //The position must be behind the delete lock
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
                } else if (type == 1) {//Perform the move sync operation
                    int equalLineNumber = 0;//equal line numbers
                    if (oneLastLoc == twoFirstLoc) {
                        equalLineNumber = twoFirstLoc;
                    } else if (twoLastLoc == oneFirstLoc) {
                        equalLineNumber = oneFirstLoc;
                    }
                    if (line == equalLineNumber) {
                        //The original logic of this code looks like this
                        //If the two adjacent lines are locked, modify them to avoid the overlap between the open bracket of the second lock and the right bracket of the first lock
                        /*int index = read.indexOf('}');//Find the last locked close bracket
                        read = read.substring(0, index) + read.substring(index + 1);//Remove the close bracket
                        StringBuilder sb = new StringBuilder(read);
                        sb.insert(0, '}');//Add a close bracket at the top of the line
                        read = sb.toString();*/

                        //However, it turns out that merging two adjacent locked rows is a good idea(even)
                        //So I just delete it
                        String syncStr = "synchronized (" + oneLockName + "){ ";
                        read = read.replace(syncStr, "");
                        int index = read.indexOf('}');//Find the last locked close bracket
                        read = read.substring(index + 1);//delete
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

    //Writes the modified content from the temporary file to the original file
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

    private void deleteTempFile(String tempFile) {
        File file = new File(tempFile);
        file.delete();
    }

    //Decide if it's a cross
    private boolean cross() {
        //Find the uncrossed case
        if (oneFirstLoc > twoLastLoc) {
            return false;
        }
        if (oneLastLoc < twoFirstLoc) {
            return false;
        }
        return true;
    }

    //Determines whether the locked end row is equal to the start row of another lock
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

