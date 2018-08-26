package fix.analyzefile;

import fix.entity.ImportPath;

import java.io.*;

public class UseOldSyncToFix {
    public static void adjustOldSync(String lockName, int locStartLine, int locEndLine, int oldStartLine, int oldEndLine, String filePath) {
        String tempFile = ImportPath.tempFile;//Temporary file directory, do not care too much, but delete after use
        FileToTempFile(lockName, locStartLine, locEndLine, oldStartLine, oldEndLine, filePath, tempFile);//Writes the source file to the temporary file
        TempFileToFile(filePath, tempFile);//Writes from a temporary file
        deleteTempFile(tempFile);//Delete temporary file
    }
    //The original file is modified and then written to the temporary file
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
                //Delete the original lock
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

                //Modify lock position
                //The position must be behind the delete lock
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

    //Writes the modified content from the temporary file to the original file
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

    //Delete temporary file
    private static void deleteTempFile(String tempFile) {
        File file = new File(tempFile);
        file.delete();
    }

}
