package fix.io;

import fix.entity.ImportPath;

import java.io.*;

public class MergePro {
    static boolean flagTer = false;
    static String tempFile = ImportPath.tempFile;//Directory of temporary files

    public static void merge(int firstLoc, int lastLoc, int oldStart, int oldEnd, String lockName, String analyseJavaPath) {
        FileToTempFile(analyseJavaPath, tempFile, lockName, firstLoc, lastLoc, oldStart, oldEnd);//Writes the source file to the temporary file
        TempFileToFile(analyseJavaPath, tempFile);//Writes from a temporary file
        deleteTempFile(tempFile);//Delete temporary file
    }


    //The original file is modified and then written to the temporary file
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
                //Prevent firstloc and oldstart, and lastloc and oldEnd from being the same
                if (line == firstLoc) {
                    read = "synchronized (" + lockName + "){" + read;
                }
                if (line == (lastLoc + 1)) {
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

    public static void mergeFirstOrLast(String analyseJavaPath, int line, int type) {
        FileToTempFile(analyseJavaPath, line, type);//Writes the source file to the temporary file
        TempFileToFile(analyseJavaPath, tempFile);//Writes from a temporary file
        deleteTempFile(tempFile);//Delete temporary file
    }

    private static void FileToTempFile(String filePath, int line, int type) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        int lineCount = 0;
        String target = "";
        if(type == 0){
            target = "}";
        } else if(type == 1){
            target = "{";
        }
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            bw = new BufferedWriter(new FileWriter(new File(tempFile)));
            String read = "";
            while (((read = br.readLine()) != null)) {
                lineCount++;
                if (lineCount == line) {
                    int index = read.indexOf(target);
                    read = read.substring(index + 1);
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

    public static void mergeLen4(String analyseJavaPath, String lockName) {
        FileToTempFile(analyseJavaPath, tempFile, lockName);
        if (flagTer) {
            TempFileToFile(analyseJavaPath, tempFile);
        }
        deleteTempFile(tempFile);
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
                if (read.contains(target)) {
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
