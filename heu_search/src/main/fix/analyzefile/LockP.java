package fix.analyzefile;

import fix.entity.ImportPath;
import fix.entity.LockNodeAndNum;
import fix.entity.LockPObject;
import fix.run.Fix;
import fix.run.FixRefactor;
import p_heu.entity.LockNode;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;

public class LockP {

    static List<LockNode> list = new ArrayList();

    static List<LockNodeAndNum> lockNodeAndNumList = new ArrayList<LockNodeAndNum>();

    static Map<String, Integer> mapLock = new HashMap<String, Integer>();

    public static void main(String[] args) {

    }


    public static String acquireLockP(String className, List<Node> nodes, ReadWriteNode readWriteNode) throws Exception {

        String targetFile = readWriteNode.getPosition().split(":")[0];
        int targetLine = 0;
        for (Node n : nodes) {

            if (n instanceof LockNode) {
                LockNode ln = (LockNode) n;
                if (ln.getAcqOrRel().equals("acq")) {
                    list.add(ln);
                } else {
                    for (int i = list.size() - 1; i >= 0; i--) {
                        LockNode o = (LockNode) list.get(i);
                        if (o.getLockElement().equals(ln.getLockElement()) && o.getThread().equals(ln.getThread()) && o.getAcqOrRel().equals("acq")) {
                            list.remove(i);
                        }
                    }
                }
            } else if (n instanceof ReadWriteNode) {
                ReadWriteNode rwn = (ReadWriteNode) n;

                if (/*rwn.getField().equals(varName) &&*/ rwn.getElement().equals(className)) {
//                    System.out.println(n + "====================");
                    for (LockNode l : list) {
                        if (l.getThread().equals(rwn.getThread())) {
//                            System.out.println(l);
                            recorder(mapLock, l);
                        }
                    }
                }
                /*Set set = mapLock.entrySet();
                Iterator it = set.iterator();
                while (it.hasNext()) {
                    Map.Entry mapEntry = (Map.Entry) it.next();
                    System.out.println(mapEntry.getKey() + "," + mapEntry.getValue());

                }*/
            }
        }


        int max = 0;
        String ln = null;
        for (String lockElement : mapLock.keySet()) {
            if (mapLock.get(lockElement) > max) {
                max = mapLock.get(lockElement);
                ln = lockElement;
            }
        }

//        System.out.println(ln + ":" + max);

        String lockName = "";
        if (ln == null) {
            return "null";//
        } else {
            for (Node n : nodes) {
                if (n instanceof ReadWriteNode) {
                    ReadWriteNode rwn = (ReadWriteNode) n;
                    String javaFile = rwn.getPosition().split(":")[0];
                    if (rwn.getElement().equals(ln) && javaFile.equals(targetFile) && rwn.getThread().equals(readWriteNode.getThread())) {
                        lockName = FixRefactor.acquireLockName(rwn, ImportPath.examplesRootPath + "/exportExamples/" + javaFile);
                    }
                }
            }

            return lockName;
        }
    }

    public static void acquireLockP(List<Node> nodes, ReadWriteNode readWriteNode) throws Exception {

        String elementName = readWriteNode.getElement();
        String targetFile = readWriteNode.getPosition().split(":")[0];
        int targetLine = 0;
        for (Node n : nodes) {

            if (n instanceof LockNode) {
                LockNode ln = (LockNode) n;
                if (ln.getAcqOrRel().equals("acq")) {
                    list.add(ln);
                } else {
                    for (int i = list.size() - 1; i >= 0; i--) {
                        LockNode o = (LockNode) list.get(i);
                        if (o.getLockElement().equals(ln.getLockElement()) && o.getThread().equals(ln.getThread()) && o.getAcqOrRel().equals("acq")) {
                            list.remove(i);
                        }
                    }
                }
            } else if (n instanceof ReadWriteNode) {
                ReadWriteNode rwn = (ReadWriteNode) n;

                if (rwn.getElement().equals(elementName)) {
                    for (LockNode l : list) {
                        if (l.getThread().equals(rwn.getThread())) {
                            recorder(mapLock, l);
                        }
                    }
                }
            }
        }


        int max = 0;
        String ln = null;
        for (String lockElement : mapLock.keySet()) {
            if (mapLock.get(lockElement) > max) {
                max = mapLock.get(lockElement);
                ln = lockElement;
            }
        }

//        System.out.println(ln + ":" + max);

        //得到lockP
        String lockName = "";
        if (ln == null) {
            LockPObject.element = "null";
            LockPObject.lockName = "null";
        } else {
            LockPObject.element = ln;
            for (Node n : nodes) {
                if (n instanceof ReadWriteNode) {
                    ReadWriteNode rwn = (ReadWriteNode) n;
                    String javaFile = rwn.getPosition().split(":")[0];
                    if (rwn.getElement().equals(ln) && javaFile.equals(targetFile) && rwn.getThread().equals(readWriteNode.getThread())) {
                        lockName = FixRefactor.acquireLockName(rwn, ImportPath.examplesRootPath + "/exportExamples/" + javaFile);
                    }
                }
            }

            LockPObject.lockName = lockName;
        }
    }

    private static void recorder(Map<String, Integer> mapLock, LockNode l) {
        Set set = mapLock.entrySet();
        Iterator it = set.iterator();
        boolean flagHas = false;
        while (it.hasNext()) {
            Map.Entry mapEntry = (Map.Entry) it.next();
            if (mapEntry.getKey().equals(l.getLockElement())) {
                mapLock.put(l.getLockElement(), (int) mapEntry.getValue() + 1);
                flagHas = true;
            }
        }
        if (!flagHas) {
            mapLock.put(l.getLockElement(), 1);
        }
    }

    public static String acquireLockPOfTwoVar(String className1, String className2, List<Node> nodes, ReadWriteNode readWriteNode) throws Exception {
        String targetFile = readWriteNode.getPosition().split(":")[0];
        int targetLine = 0;
        for (Node n : nodes) {
            if (n instanceof LockNode) {
                LockNode ln = (LockNode) n;
                if (ln.getAcqOrRel().equals("acq")) {
                    list.add(ln);
                } else {
                    for (int i = list.size() - 1; i >= 0; i--) {
                        LockNode o = (LockNode) list.get(i);
                        if (o.getLockElement().equals(ln.getLockElement()) && o.getThread().equals(ln.getThread()) && o.getAcqOrRel().equals("acq")) {
                            list.remove(i);
                        }
                    }
                }
            } else if (n instanceof ReadWriteNode) {
                ReadWriteNode rwn = (ReadWriteNode) n;

                if (/*rwn.getField().equals(varName) &&*/ rwn.getElement().contains(className1) || rwn.getElement().contains(className2)) {
//                    System.out.println(n + "====================");
                    for (LockNode l : list) {
                        if (l.getThread().equals(rwn.getThread())) {
//                            System.out.println(l);
                            recorder(mapLock, l);
                        }
                    }
                }
            }
        }


        int max = 0;
        String ln = null;
        for (String lockElement : mapLock.keySet()) {
            if (mapLock.get(lockElement) > max) {
                max = mapLock.get(lockElement);
                ln = lockElement;
            }
        }

//        System.out.println(ln + ":" + max);

        String lockName = "";
        if (ln == null) {
            LockPObject.element = "null";
            return "null";
        } else {
            LockPObject.element = ln;
            for (Node n : nodes) {
                if (n instanceof ReadWriteNode) {
                    ReadWriteNode rwn = (ReadWriteNode) n;
                    String javaFile = rwn.getPosition().split(":")[0];
                    if (rwn.getElement().equals(ln) && javaFile.equals(targetFile) && rwn.getThread().equals(readWriteNode.getThread())) {
                        lockName = FixRefactor.acquireLockName(rwn, ImportPath.examplesRootPath + "/exportExamples/" + javaFile);
                    }
                }
            }

            return lockName;
        }
    }

    public static int acqStartLine(ReadWriteNode rwn, List<Node> nodes) throws Exception {
        String targetFile = rwn.getPosition().split(":")[0];
        int targetLine = Integer.parseInt(rwn.getPosition().split(":")[1]);
        int index = 0;
        for (Node n : nodes) {
            if (n instanceof LockNode) {
                LockNode ln = (LockNode) n;
                String fileP = ln.getLocation().split(":")[0];
                if (ln.getAcqOrRel().equals("acq")) {
                    if (ln.getLockElement().equals(rwn.getElement()) && targetFile.equals(fileP) && rwn.getThread().equals(ln.getThread())) {
                        int fileLine = Integer.parseInt(ln.getLocation().split(":")[1]);
                        if (fileLine <= targetLine)
                            index = fileLine;
                    }
                }

            }
        }
        return index;
    }

    public static int acqEndLine(ReadWriteNode rwn, List<Node> nodes) throws Exception {
        int index = 0;
        String targetFile = rwn.getPosition().split(":")[0];
        int targetLine = Integer.parseInt(rwn.getPosition().split(":")[1]);
        for (Node n : nodes) {
            if (n instanceof LockNode) {
                LockNode ln = (LockNode) n;
//                System.out.println(ln);
                if (ln.getAcqOrRel().equals("rel")) {
//                    System.out.println(ln);
                    String fileP = ln.getLocation().split(":")[0];
                    if (ln.getLockElement().equals(rwn.getElement()) && targetFile.equals(fileP) && rwn.getThread().equals(ln.getThread())) {
                        int fileLine = Integer.parseInt(ln.getLocation().split(":")[1]);
                        if (fileLine > targetLine) {
                            index = fileLine;
                            break;
                        }
                    }
                }
            }
        }
        return index;
    }

    public static void deleteLock(int startLockLine, int endLockLine, String filePath) throws Exception {
        String tempFile = ImportPath.tempFile;//临时文件的目录，不用太在意，反正用完就删
        FileToTempFile(filePath, tempFile, startLockLine, endLockLine);//将源文件修改后写入临时文件
        TempFileToFile(filePath, tempFile);//从临时文件写入
        deleteTempFile(tempFile);//删除临时文件
    }

    //原文件修改后写入临时文件
    private static void FileToTempFile(String filePath, String tempFile, int startLockLine, int endLockLine) {
        BufferedReader br = null;
        BufferedWriter bw = null;
        int line = 0;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            bw = new BufferedWriter(new FileWriter(new File(tempFile)));
            String read = "";
            while (((read = br.readLine()) != null)) {
                line++;
                if (line == startLockLine) {
                    int index = read.indexOf('{');
                    read = read.substring(index + 1);
                } else if (line == endLockLine) {
                    int index = read.indexOf('}');
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
