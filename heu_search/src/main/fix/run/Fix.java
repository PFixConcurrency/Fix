package fix.run;

import fix.analyzefile.*;
import fix.entity.GlobalStaticObject;
import fix.entity.ImportPath;
import fix.entity.lock.ExistLock;
import fix.entity.type.AddSyncType;
import fix.entity.type.FixType;
import fix.io.ExamplesIO;
import fix.io.InsertCode;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.pattern.Pattern;
import p_heu.run.Unicorn;
import soot.UseSoot;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

public class Fix {
    static ExamplesIO examplesIO = ExamplesIO.getInstance();
    static String firstDirPath = ImportPath.examplesRootPath + "/examples/" + ImportPath.projectName;//第一次修复的文件路径
    static String dirPath = "";//第一次修复的文件路径
    //    static String dirPath = ImportPath.examplesRootPath + "/exportExamples/" + ImportPath.projectName;//第一次修复的文件路径
    static String iterateDirPath = ImportPath.examplesRootPath + "/exportExamples/" + ImportPath.projectName;//迭代修复的文件路径

    static String whichCLassNeedSync = "";//需要添加同步的类，此处需不需考虑在不同类之间加锁的情况？
    static LockAdjust lockAdjust = LockAdjust.getInstance();//当锁交叉时，用来合并锁

    static String fixMethods = "";//记录修复方法，写入文件中

    static String sourceClassPath = "";//源代码的生成类，记录下来，以后用jpf分析class

    static String addSyncFilePath = "";//加锁路径

    //用来计时
    static long startUnicornTime = 0;
    static long endUnicornTime = 0;
    static long startFixTime = 0;
    static long endFixTime = 0;
    static String patternListTime = "";
    static String fixTime = "";

    //全局静态变量
    static GlobalStaticObject globalStaticObject = GlobalStaticObject.getInstance();

    //用于跨类修复
    static UseSoot useSoot = UseSoot.getInstance();

    //第一次运行拿到的pattern
//    static List<Unicorn.PatternCounter> firstList = null;


    public static void main(String[] args) {
        startUnicornTime = System.currentTimeMillis();

        try {
            fix(FixType.firstFix);
//            //要测
//            while(fixMethods.contains("fix fail")){
//                step++;
//                fix(FixType.iterateFix);
//            }
        } catch (Exception e) {
            System.out.println("running error,please restart");
        } finally {

            endFixTime = System.currentTimeMillis();
            fixTime = "time for fixing:" + (endFixTime - startFixTime);
            //将修复方法写入文件中
            InsertCode.writeLogFile(fixMethods + fixTime, "fix result");
        }

    }

    private static void fix(int type) {


        String verifyClasspath = ImportPath.verifyPath + "/generateClass";//要验证的class路径

        //处理包名有几层的情况
        if (firstDirPath.contains(".")) {
            firstDirPath = firstDirPath.replaceAll("\\.", "/");
        }

        sourceClassPath = ImportPath.examplesRootPath + "/out/production/Patch";

        //拿到pattern集合
        List<Unicorn.PatternCounter> firstList = Unicorn.getPatternCounterList(sourceClassPath);


        //将长度为2的删除
        //实际上对，理论上考虑不全面
        /*for(int i = tempList.size() - 1;i >=0; i--) {
            if(tempList.get(i).getPattern().getNodes().length == 2)
                tempList.remove(i);
        }*/

        endUnicornTime = System.currentTimeMillis();
        patternListTime = "time for getting pattern list : " + (endUnicornTime - startUnicornTime);

        //将所有的pattern打印出来，方便以后选择
        System.out.println(firstList);


        //将所有pattern写入文件0
        InsertCode.writeLogFile(firstList.toString() + '\n' + patternListTime, "pattern list");


       /* System.out.println("if no correct pattern,please restart");
        System.out.print("select correct pattern number(The bottom one is zero):");*/

        //此处需要手动选择
        Scanner sc = new Scanner(System.in);
        int whichToUse = sc.nextInt();//使用第几个pattern

        startFixTime = System.currentTimeMillis();
       /* int whichToUse = 0;

        Unicorn.PatternCounter patternCounter = tempList.get(tempList.size() - 1 - whichToUse);*/

        int i = 0;
//        while (i < firstList.size()) {
        while (i < 1) {
//            Unicorn.PatternCounter patternCounter = firstList.get(i);
            Unicorn.PatternCounter patternCounter = firstList.get(whichToUse);
            i++;
            if (type == FixType.firstFix) {
                //先将项目拷贝到exportExamples
                dirPath = examplesIO.copyFromOneDirToAnotherAndChangeFilePath("examples", "exportExamples", firstDirPath);

            } else if (type == FixType.iterateFix) {
                dirPath = iterateDirPath;
                sourceClassPath = ImportPath.verifyPath + "/generateClass";
            }

            //根据pattern知道需要在哪个类中加锁
            String position = patternCounter.getPattern().getNodes()[0].getPosition();
            String[] tempSplit = position.split(":")[0].split("/");
            whichCLassNeedSync = tempSplit[tempSplit.length - 1];

            addSyncFilePath = ImportPath.examplesRootPath + "/exportExamples/" + position.split(":")[0];

            //将拿到的pattern写入文件中
            InsertCode.writeLogFile(patternCounter.toString(), "pattern for fix");


            //如果pattern在获取call graph或者没在失败中出现，会导致运行错误，则认为是错误的pattern，因为它提供了错误的位置
            try {
                //没有在失败中出现，就表明它跟错误没关系，直接跳过
                if (patternCounter.getFirstFailAppearPlace() == null) {
                    continue;
                }
                //拿到该pattern对应的sequence
                //第一次在失败运行中出现的sequence
                RecordSequence.display(patternCounter.getFirstFailAppearPlace());

                //将sequence写入文件中
//                InsertCode.writeLogFile(patternCounter.getFirstFailAppearPlace().toString(), "sequence for fix");

                //对拷贝的项目进行修复
                divideByLength(patternCounter);

                //检测修复完的程序是否正确
                fixMethods += "result : ";
                if (Unicorn.verifyFixSuccessful(verifyClasspath)) {
                    fixMethods += "fix success\n";
                    break;
                } else {
                    fixMethods += "fix fail\n";
                }
            } catch (Exception e) {
                System.out.println("Not this pattern");
            }

        }
    }

    //根据pattern的长度执行不同的fix策略
    private static void divideByLength(Unicorn.PatternCounter patternCounter) {
        int length = patternCounter.getPattern().getNodes().length;
        if (length == 2) {
            fixMethods += "Fix one\n";
            usePatternToDistinguish(patternCounter.getPattern());
        } else if (length == 3) {
            fixMethods += "Fix two\n";
            usePatternToAddSync(patternCounter.getPattern());
        } else if (length == 4) {
            fixMethods += "Fix three\n";
            usePatternToAddSync(patternCounter.getPattern());
        }
    }

    //长度为2，先分情况
    private static void usePatternToDistinguish(Pattern patternCounter) {

        if (RecordSequence.isLast(patternCounter.getNodes()[0]) || RecordSequence.isFirst(patternCounter.getNodes()[1])) {
            //为长度为2的pattern添加同步
            fixMethods += "Added Semaphore\n";
            addSignal(patternCounter);
        } else {
            //为长度为2的pattern添加同步,与3和4是不同的情况
            fixMethods += "Add synchronization\n";
            addSyncPatternOneToThree(patternCounter);
        }
    }

    //长度为3或4，添加同步
    private static void usePatternToAddSync(Pattern patternCounter) {
        //根据线程将三个结点分为两个list
        List<ReadWriteNode> threadA = new ArrayList<ReadWriteNode>();//线程A的结点
        List<ReadWriteNode> threadB = new ArrayList<ReadWriteNode>();//线程B的结点
        String threadName = "";
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            ReadWriteNode node = patternCounter.getNodes()[i];
            if (i == 0) {//把第一个结点放入A的list
                threadName = node.getThread();
                threadA.add(node);
            } else {
                if (threadName.equals(node.getThread())) {//线程相同，放入同一个list
                    threadA.add(node);
                } else {//不同就放入另一个list
                    threadB.add(node);
                }
            }
        }

        //长度为3加对象锁
        if (patternCounter.getNodes().length == 3) {
            //根据获得的list，进行加锁
            addSynchronized(threadA, AddSyncType.localSync);
            lockAdjust.setOneLockFinish(true);//表示第一次执行完
            addSynchronized(threadB, AddSyncType.localSync);
            lockAdjust.adjust(addSyncFilePath);//合并锁
        } else if (patternCounter.getNodes().length == 4) {//长度为4加静态锁？
            //根据获得的list，进行加锁
            addSynchronized(threadA, AddSyncType.globalStaticSync);
            lockAdjust.setOneLockFinish(true);//表示第一次执行完
            addSynchronized(threadB, AddSyncType.globalStaticSync);
            lockAdjust.adjust(addSyncFilePath);//合并锁
        }

    }

    //对一个线程中的node进行加锁
    private static void addSynchronized(List<ReadWriteNode> rwnList, int type) {
        int firstLoc = 0, lastLoc = 0;

        String lockName = "";//用来表示加锁的名称

        String lockFile = "";//用来表示加锁文件

        //判断有几个变量，
        //如果有两个变量在不在一行
        boolean flagInTwoLine = false;

        //跨类修复时是否使用静态锁
        boolean flagStaticLock = true;

        if (rwnList.size() > 1) {
            String oneField = rwnList.get(0).getField();
            String onePosition = rwnList.get(0).getPosition();

            String twoField = rwnList.get(1).getField();
            String twoPosition = rwnList.get(1).getPosition();

            if (oneField.equals(twoField) && onePosition.equals(twoPosition)) {
                //同一行的同一个变量。可以当成一行处理，加锁时不影响
                flagInTwoLine = false;
            } else {
                flagInTwoLine = true;
            }
        } else {//只有一个变量,
            flagInTwoLine = false;
        }

        //当前分析的是哪个文件？
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        //判断A中有几个变量
        if (flagInTwoLine) {//两个变量,且在两行
            //如果有两个变量，需要分析
            //判断它们在不在一个函数中
            boolean flagSame;
            //如果不在两个文件中肯定不是一个函数
            if (!rwnList.get(0).getPosition().split(":")[0].equals(rwnList.get(1).getPosition().split(":")[0])) {
                flagSame = false;
            } else {
                //如果在同一个文件中分析
                flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
            }

            if (flagSame) {//在一个函数中
                //先找找原来有没有锁
                boolean varHasLock = false;//记录当前pattern是否加锁
                ExistLock existLock = null;

                int existGlobalLockStart = Integer.MAX_VALUE, existGlobalLockEnd = 0;
                //判断它们有没有加锁，需要加何种锁，加锁位置
                //对A的list分析
                for (int i = 0; i < rwnList.size(); i++) {
                    ReadWriteNode node = rwnList.get(i);
                    //对于局部锁，先检查它是否已经被加锁
                    if (type == AddSyncType.localSync) {
                        if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath, analyseJavaPath)) {//检查是否存在锁
                            if (i == 1 && varHasLock == true) {//表示两个都有锁
                                return;//直接结束
                            } else {
                                varHasLock = true;//有锁标为true
                                existLock = existLockName(rwnList.get(i));
                            }
                        }
                    } else {
                        //当长度为4，一致性错误时，即使原先变量被加锁，还是要加锁
                        //存在锁
                        if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath, analyseJavaPath)) {
                            existLock = existLockName(rwnList.get(i));
                            existGlobalLockStart = Math.min(existGlobalLockStart, existLock.getStartLine());
                            existGlobalLockEnd = Math.max(existGlobalLockEnd, existLock.getEndLine());
                        }
                    }
                    //应该要加什么锁
                    if (type == AddSyncType.localSync) {//需要添加局部锁
                        //这个步骤实际是用分析字符串来完成的
                        //实际上是不严格的
                        lockName = acquireLockName(node, analyseJavaPath);
                    } else if (type == AddSyncType.globalStaticSync) {//需要添加全局锁
                        if (!globalStaticObject.isDefineObject) {
                            lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                            globalStaticObject.objectName = lockName;
                            globalStaticObject.isDefineObject = true;
                        } else {
                            lockName = globalStaticObject.objectName;
                        }
                    }
                    int poi = Integer.parseInt(node.getPosition().split(":")[1]);
                    if (i == 0) {
                        firstLoc = poi;
                        lastLoc = firstLoc;
                    } else {
                        if (poi < firstLoc) {
                            firstLoc = poi;
                        } else {
                            lastLoc = poi;
                        }
                    }
                }

                //判断加锁区域在不在构造函数，或者加锁变量是不是成员变量
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc, analyseJavaPath)) {

                    //判断加锁会不会和for循环等交叉
                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //检查会不会定义变量在锁内，使用变量在锁外
                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //并不是加this锁，可能会出现新加的同步与原来重合的情况，需要合并
                    if (!lockName.equals("this")) {
                        lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                        firstLoc = lockLine.getFirstLoc();
                        lastLoc = lockLine.getLastLoc();
                    }

                    //两个地方都没有加锁
                    if (!varHasLock) {
                        if (type == AddSyncType.globalStaticSync) {
                            firstLoc = Math.min(firstLoc, existGlobalLockStart);
                            lastLoc = Math.max(lastLoc, existGlobalLockEnd);
                        }
                        //加锁
                        examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);
                    } else {//有加锁的，直接修改原有锁
                        UseOldSyncToFix.adjustOldSync(existLock.getLockName(), firstLoc, lastLoc + 1, existLock.getStartLine(), existLock.getEndLine(), analyseJavaPath);
                    }

                    lockFile = analyseJavaPath;
                }
            } else {//不在一个函数中
                //跨类搜索
                useSoot.getCallGraph(rwnList.get(0), rwnList.get(1));

                //如果跨类没找到，就直接结束
                if (useSoot.getSyncJava().equals(".")) {
                    lockAdjust.setOneLockFile("");//设为空，以后就不会合并了
                    return;
                }

                //得到加锁位置
                firstLoc = useSoot.getMinLine();
                lastLoc = useSoot.getMaxLine();

                String leftMethodName = whichObjectsFunction(firstLoc,GlobalStaticObject.leftMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                String rightMethodName = whichObjectsFunction(lastLoc,GlobalStaticObject.rightMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                //如果pattern定位到的变量是某个对象的变量，不加静态锁,根据stringbuffer受到启发
                if(leftMethodName.equals(rightMethodName) && leftMethodName.length() > 0){
                    flagStaticLock = false;
                }
                /*//如果pattern来自同一个类，那么跨类之后加的是this锁
                String classNameOne = rwnList.get(0).getPosition().split("\\.")[0].replaceAll("/", ".");
                String classNameTwo = rwnList.get(1).getPosition().split("\\.")[0].replaceAll("/", ".");
                if (classNameOne.equals(classNameTwo)) {
                    examplesIO.addLockToOneVar(useSoot.getMinLine(), useSoot.getMaxLine() + 1, "this", ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                } else {
                    examplesIO.addLockToOneVar(useSoot.getMinLine(), useSoot.getMaxLine() + 1, "obj", ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                }*/


                //判断加锁区域在不在构造函数，或者加锁变量是不是成员变量
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava())) {
                    //判断加锁会不会和for循环等交叉
                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //检查会不会定义变量在锁内，使用变量在锁外
                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //并不是加this锁，可能会出现新加的同步与原来重合的情况，需要合并
                    lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();
                    //暂定为都加静态锁，this锁不一定对
                    //后来根据stringbuffer发现，加静态锁也不一定对，要考虑到底是哪个对象的变量
                    if(flagStaticLock) {
                        lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                    }else {
                        lockName = leftMethodName;
                    }

                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    lockFile = ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava();
                }
            }
        } else {
            //对于一个变量，检查它是否已经被加锁
            ReadWriteNode node = rwnList.get(0);

            if (!CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath, analyseJavaPath)) {

                //没被加锁，获得需要加锁的行数
                firstLoc = Integer.parseInt(node.getPosition().split(":")[1]);
                lastLoc = firstLoc;
                //然后获得需要加何种锁
                lockName = acquireLockName(node, analyseJavaPath);
//                System.out.println(lockName + "lockname");

                //判断加锁会不会和for循环等交叉
                UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                //检查会不会定义变量在锁内，使用变量在锁外
                lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                //并不是加this锁，可能会出现新加的同步与原来重合的情况，需要合并
                if (!lockName.equals("this")) {
                    lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();
                }

                //然后加锁
                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                lockFile = analyseJavaPath;
            }
        }

        //记录加锁位置
        //便于以后调整
        if (!lockAdjust.isOneLockFinish()) {
            lockAdjust.setOneLockFile(lockFile);
            lockAdjust.setOneLockName(lockName);
            lockAdjust.setOneFirstLoc(firstLoc);
            lockAdjust.setOneLastLoc(lastLoc + 1);
        } else {
            lockAdjust.setTwoLockFile(lockFile);
            lockAdjust.setOneLockName(lockName);
            lockAdjust.setTwoFirstLoc(firstLoc);
            lockAdjust.setTwoLastLoc(lastLoc + 1);
        }


        //表示能加锁
        if (firstLoc > 0 && lastLoc > 0) {
            fixMethods += "aim at : " + rwnList.get(0) + "Lock start and stop position : " + firstLoc + "->" + lastLoc + '\n';

        }

        //关联变量处理
        if(lockName.equals("this") || lockName.equals("objectFix")){
            LockPolicyPopularize.fixRelevantVar(firstLoc, lastLoc, rwnList.get(0).getThread(), whichCLassNeedSync, lockName, addSyncFilePath);//优化
        }
    }

    //读到那一行，然后对字符串处理
    //获取锁的名称
    public static String acquireLockName(ReadWriteNode node, String filePath) {
        BufferedReader br = null;
        String read = "";//用来读
        String result = "";//用来处理
        int line = 0;
        int poi = Integer.parseInt(node.getPosition().split(":")[1]);
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            while (((read = br.readLine()) != null)) {
                line++;
                if (line == poi) {//找到哪一行
                    String field = node.getField();//得的变量
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("^.*?(((\\w+\\.)+)" + field + ").*$");
                    Matcher m = p.matcher(read);
                    if (m.matches()) {
                        result = m.group(1);
                        int indexTemp = result.lastIndexOf('.');
                        if (indexTemp == -1) {
                            result = "this";
                        } else {
                            result = result.substring(0, indexTemp);
                        }
                    } else {
                        result = "this";
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //如果是类的static变量，直接加this锁？？
        //认为如果不含有@符号就是静态变量
        if (!node.getElement().contains("@")) {
            result = "this";
        }

        fixMethods += "Lock Name : " + result.trim() + '\n';
        return result.trim();
    }

    //输出锁的名称
    private static ExistLock existLockName(ReadWriteNode node) {
        ExistLock existLock = UseASTAnalysisClass.useASTCFindLockLine(node, addSyncFilePath);
        existLock = AcquireSyncName.acquireSync(existLock, addSyncFilePath);
        return existLock;
    }

    //获取方法属于哪个对象
    public static String whichObjectsFunction(int targetLine, String methodName, String filePath) {
        BufferedReader br = null;
        String read = "";//用来读
        String result = "";//用来处理
        int line = 0;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
            while (((read = br.readLine()) != null)) {
                line++;
                if (line == targetLine) {//找到哪一行
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("^.*?(((\\w+\\.)+)" + methodName + ").*$");
                    Matcher m = p.matcher(read);
                    if (m.matches()) {
                        result = m.group(1);
                        int indexTemp = result.lastIndexOf('.');
                        if (indexTemp == -1) {
                            result = "";
                        } else {
                            result = result.substring(0, indexTemp);
                        }
                    } else {
                        result = "";
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.trim();
    }

    //对长度为2的pattern添加同步
    private static void addSyncPatternOneToThree(Pattern patternCounter) {

        int firstLoc = 0, lastLoc = 0;

        //判断在不在一个 函数中
        List<ReadWriteNode> rwnList = new ArrayList<ReadWriteNode>();
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            rwnList.add(patternCounter.getNodes()[i]);
        }

        //当前分析的是哪个文件？
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        boolean flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
        if (flagSame) {//在一个函数中
            int oneLoc = Integer.parseInt(patternCounter.getNodes()[0].getPosition().split(":")[1]);
            int twoLoc = Integer.parseInt(patternCounter.getNodes()[1].getPosition().split(":")[1]);
            firstLoc = Math.min(oneLoc, twoLoc);
            lastLoc = Math.max(oneLoc, twoLoc);
            String lockName = acquireLockName(patternCounter.getNodes()[0], analyseJavaPath);
            if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc + 1, analyseJavaPath)) {
                //加锁
                //检查是否存在锁再加锁
                if (!CheckWhetherLocked.check(patternCounter.getNodes()[0].getPosition(), patternCounter.getNodes()[0].getField(), sourceClassPath, analyseJavaPath)) {
                    //判断加锁会不会和for循环等交叉
                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //检查会不会定义变量在锁内，使用变量在锁外
                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';
                    //根据wronglock2来的启发，都在run()里面需要全局静态锁
                    if (UseASTAnalysisClass.checkInRun(firstLoc,lastLoc,analyseJavaPath)) {
                        lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
//                        System.out.println(lockName);
                    }
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//待定
                }
            }
        } else {
            //长度为2加锁时候不跨类
            for (int i = 0; i < 2; i++) {
                String position = patternCounter.getNodes()[i].getPosition();
                String[] positionArg = position.split(":");

                //获取要加锁的锁名
                //如果已有锁，直接用现有的锁
                //如果没有，再寻找新锁
                String lockName = acquireLockName(patternCounter.getNodes()[i], analyseJavaPath);

                //此处就在一行加锁，所以行数一样
                firstLoc = Integer.parseInt(positionArg[1]);
                lastLoc = firstLoc;

                if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(Integer.parseInt(positionArg[1]), Integer.parseInt(positionArg[1]) + 1, analyseJavaPath)) {
                    //加锁
                    //检查是否存在锁再加锁
                    if (!CheckWhetherLocked.check(position, patternCounter.getNodes()[i].getField(), sourceClassPath, analyseJavaPath)) {
                        fixMethods += "Locked position" + Integer.parseInt(positionArg[1]) + '\n';
                        //判断一下能不能用当前的锁直接进行修复

                        //判断加锁会不会和for循环等交叉
                        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                        firstLoc = lockLine.getFirstLoc();
                        lastLoc = lockLine.getLastLoc();

                        //检查会不会定义变量在锁内，使用变量在锁外
                        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                        firstLoc = lockLine.getFirstLoc();
                        lastLoc = lockLine.getLastLoc();

                        if (!lockAdjust.isOneLockFinish()) {
                            lockAdjust.setOneLockFile(analyseJavaPath);
                            lockAdjust.setOneFirstLoc(firstLoc);
                            lockAdjust.setOneLastLoc(lastLoc + 1);
                            lockAdjust.setOneLockFinish(true);
                        } else {
                            lockAdjust.setTwoLockFile(analyseJavaPath);
                            lockAdjust.setTwoFirstLoc(firstLoc);
                            lockAdjust.setTwoLastLoc(lastLoc + 1);
                        }
                        //加锁
                        examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//待定

                        lockAdjust.adjust(analyseJavaPath);
                    }
                }
            }
        }
    }

    //添加信号量修复顺序违背
    private static void addSignal(Pattern patternCounter) {
        //得到pattern中较小的行数
        int flagDefineLocation = Integer.MAX_VALUE;//flag应该在哪行定义
        int flagAssertLocation = Integer.MIN_VALUE;//flag应该在那行判断
        for (int i = 0; i < 2; i++) {
            String position = patternCounter.getNodes()[i].getPosition();
            String[] positionArg = position.split(":");
            flagDefineLocation = Integer.parseInt(positionArg[1]) < flagDefineLocation ? Integer.parseInt(positionArg[1]) : flagDefineLocation;
            flagAssertLocation = Integer.parseInt(positionArg[1]) > flagAssertLocation ? Integer.parseInt(positionArg[1]) : flagAssertLocation;
        }

        fixMethods += "The location of semaphore defines : " + flagDefineLocation + '\n';
        fixMethods += "The location of semaphore use : " + flagAssertLocation + '\n';

        //构造函数不能加信号量
        if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(flagAssertLocation, flagAssertLocation, addSyncFilePath) &&
                !UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(flagAssertLocation, flagAssertLocation, addSyncFilePath)) {
            //添加信号量的定义
            examplesIO.addVolatileDefine(flagDefineLocation, "volatile bool flagFix = false;", addSyncFilePath);//待修订

            //添加信号量判断,
            examplesIO.addVolatileIf(flagAssertLocation, addSyncFilePath);//待修订

            //添加信号为true的那条语句，那条语句应该在定义的后一行
            examplesIO.addVolatileToTrue(flagDefineLocation + 1, addSyncFilePath);//待修订
        }
    }
}
