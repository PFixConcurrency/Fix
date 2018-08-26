/**
 * This file has been discarded
 */
package fix.run.abandon;

import fix.analyzefile.*;
import fix.entity.GlobalStaticObject;
import fix.entity.ImportPath;
import fix.entity.lock.ExistLock;
import fix.entity.type.AddSyncType;
import fix.entity.type.FixType;
import fix.io.ExamplesIO;
import fix.io.InsertCode;
import fix.run.Propagate;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.pattern.Pattern;
import p_heu.entity.sequence.Sequence;
import p_heu.run.Unicorn;
import soot.UseSoot;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

public class Fix2 {
    static ExamplesIO examplesIO = ExamplesIO.getInstance();
    static String firstDirPath = ImportPath.examplesRootPath + "/examples/" + ImportPath.projectName;
    static String dirPath = "";
    //    static String dirPath = ImportPath.examplesRootPath + "/exportExamples/" + ImportPath.projectName;
    static String iterateDirPath = ImportPath.examplesRootPath + "/exportExamples/" + ImportPath.projectName;

    static String whichCLassNeedSync = "";
    static LockAdjust lockAdjust = LockAdjust.getInstance();

    static String fixMethods = "";

    static String sourceClassPath = "";

    static String addSyncFilePath = "";


    static long startUnicornTime = 0;
    static long endUnicornTime = 0;
    static long startFixTime = 0;
    static long endFixTime = 0;
    static String patternListTime = "";
    static String fixTime = "";


    static GlobalStaticObject globalStaticObject = GlobalStaticObject.getInstance();


    static UseSoot useSoot = UseSoot.getInstance();


//    static List<Unicorn.PatternCounter> firstList = null;


    static Sequence sequence = null;

    //length 4 lockVar
    static String lenFourLockVar = "";

    public static void main(String[] args) {
        startUnicornTime = System.currentTimeMillis();

        try {
            fix(FixType.firstFix);
//
//            while(fixMethods.contains("fix fail")){
//                step++;
//                fix(FixType.iterateFix);
//            }
        } catch (Exception e) {
            System.out.println("running error,please restart");
        } finally {

            endFixTime = System.currentTimeMillis();
            fixTime = "time for fixing:" + (endFixTime - startFixTime);

            InsertCode.writeLogFile(fixMethods + fixTime, "fix result");
            System.out.println("if still bug ,run again and again");
        }

    }

    private static void fix(int type) throws Exception {


        String verifyClasspath = ImportPath.verifyPath + "/generateClass";


        if (firstDirPath.contains(".")) {
            firstDirPath = firstDirPath.replaceAll("\\.", "/");
        }

        sourceClassPath = ImportPath.examplesRootPath + "/out/production/Patch";


        List<Unicorn.PatternCounter> firstList = Unicorn.getPatternCounterList(sourceClassPath);



        /*for(int i = tempList.size() - 1;i >=0; i--) {
            if(tempList.get(i).getPattern().getNodes().length == 2)
                tempList.remove(i);
        }*/

        endUnicornTime = System.currentTimeMillis();
        patternListTime = "time for getting pattern list : " + (endUnicornTime - startUnicornTime);


        System.out.println(firstList);


        InsertCode.writeLogFile(firstList.toString() + '\n' + patternListTime, "pattern list");


       /* System.out.println("if no correct pattern,please restart");
        System.out.print("select correct pattern number(The bottom one is zero):");*/


        Scanner sc = new Scanner(System.in);
        int whichToUse = sc.nextInt();

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

                dirPath = examplesIO.copyFromOneDirToAnotherAndChangeFilePath("examples", "exportExamples", firstDirPath);

            } else if (type == FixType.iterateFix) {
                dirPath = iterateDirPath;
                sourceClassPath = ImportPath.verifyPath + "/generateClass";
            }


            String position = patternCounter.getPattern().getNodes()[0].getPosition();
            String[] tempSplit = position.split(":")[0].split("/");
            whichCLassNeedSync = tempSplit[tempSplit.length - 1];

            addSyncFilePath = ImportPath.examplesRootPath + "/exportExamples/" + position.split(":")[0];


            InsertCode.writeLogFile(patternCounter.toString(), "pattern for fix");



            try {

                if (patternCounter.getFirstFailAppearPlace() == null) {
                    continue;
                }

                RecordSequence.display(patternCounter.getFirstFailAppearPlace());


                InsertCode.writeLogFile(patternCounter.getFirstFailAppearPlace().toString(), "sequence for fix");
                sequence = patternCounter.getFirstFailAppearPlace();


                divideByLength(patternCounter);


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


    private static void divideByLength(Unicorn.PatternCounter patternCounter) throws Exception {
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


    private static void usePatternToDistinguish(Pattern patternCounter) throws Exception {

        if (RecordSequence.isLast(patternCounter.getNodes()[0]) || RecordSequence.isFirst(patternCounter.getNodes()[1])) {

            fixMethods += "Added Semaphore\n";
            addSignal(patternCounter);
        } else {

            fixMethods += "Add synchronization\n";
            addSyncPatternOneToThree(patternCounter);
        }
    }


    private static void usePatternToAddSync(Pattern patternCounter) throws Exception {

        List<ReadWriteNode> threadA = new ArrayList<ReadWriteNode>();
        List<ReadWriteNode> threadB = new ArrayList<ReadWriteNode>();
        String threadName = "";
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            ReadWriteNode node = patternCounter.getNodes()[i];
            if (i == 0) {
                threadName = node.getThread();
                threadA.add(node);
            } else {
                if (threadName.equals(node.getThread())) {
                    threadA.add(node);
                } else {
                    threadB.add(node);
                }
            }
        }


        if (patternCounter.getNodes().length == 3) {

            addSynchronized(threadA, AddSyncType.len3);
            lockAdjust.setOneLockFinish(true);
            addSynchronized(threadB, AddSyncType.len3);
            lockAdjust.adjust(addSyncFilePath);
            String lockName = LockP.acquireLockP(patternCounter.getNodes()[0].getElement(), sequence.getNodes(), patternCounter.getNodes()[0]);
            String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + patternCounter.getNodes()[0].getPosition().split(":")[0];

            if (lockName.equals("null")) {
                lockName = acquireLockName(patternCounter.getNodes()[0], analyseJavaPath);
            }
            Propagate.p(lockName, patternCounter.getNodes()[0], sequence, sourceClassPath);
        } else if (patternCounter.getNodes().length == 4) {
            boolean flagSame1 = UseASTAnalysisClass.assertSameFunction(threadA, ImportPath.examplesRootPath + "/exportExamples/" + threadA.get(0).getPosition().split(":")[0]);
            boolean flagSame2 = UseASTAnalysisClass.assertSameFunction(threadB, ImportPath.examplesRootPath + "/exportExamples/" + threadB.get(0).getPosition().split(":")[0]);

            if (!flagSame1 && !flagSame2) {

                //acquire lockP
                String lockp1 = LockP.acquireLockP(threadA.get(0).getElement(), sequence.getNodes(), threadA.get(0));
                String lockp2 = LockP.acquireLockP(threadB.get(0).getElement(), sequence.getNodes(), threadB.get(0));


                String lockVar = "";
                //compare them
                if (lockp1.equals("null") && lockp2.equals("null")) {
                    //add new lock
                    lockVar = "null";
                } else if (lockp1.equals("null")) {
                    lockVar = lockp2;
                } else if (lockp2.equals("null")) {
                    lockVar = lockp1;
                } else {
                    lockVar = LockP.acquireLockPOfTwoVar(threadA.get(0).getElement(), threadA.get(0).getElement(), sequence.getNodes(), threadA.get(0));
                }


                lenFourLockVar = lockVar;

                addSynchronized(threadA, AddSyncType.len4);
                lockAdjust.setOneLockFinish(true);
                addSynchronized(threadB, AddSyncType.len4);
                lockAdjust.adjust(addSyncFilePath);
            } else {
                addSynchronizedOfFourLengthPattern(patternCounter.getNodes());
                return;
            }
        }


        if (LockPolicyPopularize.flagCross) {
            LockPolicyPopularize.fixRelevantVar(addSyncFilePath);
        } else {
            LockPolicyPopularize.firstLoc = lockAdjust.getOneFirstLoc();
            LockPolicyPopularize.lastLoc = lockAdjust.getOneLastLoc();
            LockPolicyPopularize.fixRelevantVar(addSyncFilePath);
            LockPolicyPopularize.firstLoc = lockAdjust.getTwoFirstLoc();
            LockPolicyPopularize.lastLoc = lockAdjust.getTwoLastLoc();
            LockPolicyPopularize.fixRelevantVar(addSyncFilePath);
        }
    }


    private static void addSynchronized(List<ReadWriteNode> rwnList, int type) throws Exception {
        int firstLoc = 0, lastLoc = 0;

        String lockName = "";

        String lockFile = "";


        boolean flagInTwoLine = false;


        boolean flagStaticLock = true;

        if (rwnList.size() > 1) {
            String oneField = rwnList.get(0).getField();
            String onePosition = rwnList.get(0).getPosition();

            String twoField = rwnList.get(1).getField();
            String twoPosition = rwnList.get(1).getPosition();

            if (oneField.equals(twoField) && onePosition.equals(twoPosition)) {

                flagInTwoLine = false;
            } else {
                flagInTwoLine = true;
            }
        } else {
            flagInTwoLine = false;
        }


        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];



        if (flagInTwoLine) {

            boolean flagSame;

            if (!rwnList.get(0).getPosition().split(":")[0].equals(rwnList.get(1).getPosition().split(":")[0])) {
                flagSame = false;
            } else {

                flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
            }

            if (flagSame) {

                boolean varHasLock = false;
                ExistLock existLock = null;

                int existGlobalLockStart = Integer.MAX_VALUE, existGlobalLockEnd = 0;

                for (int i = 0; i < rwnList.size(); i++) {
                    ReadWriteNode node = rwnList.get(i);

                    if (type == AddSyncType.len3) {
                        if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath)) {
                            if (i == 1 && varHasLock == true) {
                                return;
                            } else {
                                varHasLock = true;
                                existLock = existLockName(rwnList.get(i));
                            }
                        }
                    } else {

                        if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath)) {
                            existLock = existLockName(rwnList.get(i));
                            existGlobalLockStart = Math.min(existGlobalLockStart, existLock.getStartLine());
                            existGlobalLockEnd = Math.max(existGlobalLockEnd, existLock.getEndLine());
                        }
                    }

                    if (type == AddSyncType.len3) {

//                        lockName = acquireLockName(node, analyseJavaPath);
//                        System.out.println("lockp==========================");
                        lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(), rwnList.get(0));
                        if (lockName.equals("null")) {
                            lockName = acquireLockName(node, analyseJavaPath);
                        }
//                        System.out.println(lockName + "====================");
//                        System.exit(-1);


                    } else if (type == AddSyncType.len4) {//leng 4
                        if (!globalStaticObject.isDefineObject) {
                            if (lenFourLockVar.equals("null")) {
                                lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                            } else {
                                lockName = lenFourLockVar;
                            }
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


                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc, analyseJavaPath)) {


                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();


                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();


                    if (!lockName.equals("this")) {
                        lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                        firstLoc = lockLine.getFirstLoc();
                        lastLoc = lockLine.getLastLoc();
                    }


                    if (!varHasLock) {
                        if (type == AddSyncType.len4) {
                            firstLoc = Math.min(firstLoc, existGlobalLockStart);
                            lastLoc = Math.max(lastLoc, existGlobalLockEnd);
                        }
                        /*
                        if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                            if (!globalStaticObject.isDefineObject) {
                                lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                                globalStaticObject.objectName = lockName;
                                globalStaticObject.isDefineObject = true;
                            } else {
                                lockName = globalStaticObject.objectName;
                            }
                        }*/

                        examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);
                    } else {
                        UseOldSyncToFix.adjustOldSync(existLock.getLockName(), firstLoc, lastLoc + 1, existLock.getStartLine(), existLock.getEndLine(), analyseJavaPath);
                    }

                    lockFile = analyseJavaPath;
                }
            } else {
                useSoot.getCallGraph(rwnList.get(0), rwnList.get(1));

                Propagate.numSet.add(Integer.parseInt(rwnList.get(0).getPosition().split(":")[1]));
                Propagate.numSet.add(Integer.parseInt(rwnList.get(1).getPosition().split(":")[1]));


                if (useSoot.getSyncJava().equals(".")) {
                    lockAdjust.setOneLockFile("");
                    return;
                }


                firstLoc = useSoot.getMinLine();
                lastLoc = useSoot.getMaxLine();
//System.out.println(firstLoc + ',' + lastLoc + ',' + useSoot.getSyncJava());System.exit(-1);
                String leftMethodName = whichObjectsFunction(firstLoc, GlobalStaticObject.leftMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                String rightMethodName = whichObjectsFunction(lastLoc, GlobalStaticObject.rightMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());

                if (leftMethodName.equals(rightMethodName) && leftMethodName.length() > 0) {
                    flagStaticLock = false;
                }
                /*
                String classNameOne = rwnList.get(0).getPosition().split("\\.")[0].replaceAll("/", ".");
                String classNameTwo = rwnList.get(1).getPosition().split("\\.")[0].replaceAll("/", ".");
                if (classNameOne.equals(classNameTwo)) {
                    examplesIO.addLockToOneVar(useSoot.getMinLine(), useSoot.getMaxLine() + 1, "this", ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                } else {
                    examplesIO.addLockToOneVar(useSoot.getMinLine(), useSoot.getMaxLine() + 1, "obj", ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                }*/



                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava())) {

                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();


                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();


                    lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();


//                    System.out.println("lockp==========================");
                    if (type == AddSyncType.len3) {
                        lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(), rwnList.get(0));
                        if (lockName.equals("null")) {
                            if (flagStaticLock) {
                                lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                            } else {
                                lockName = leftMethodName;
                            }
                        } else if (lockName.equals("this")) {
                            if (!flagStaticLock) {
                                lockName = leftMethodName;
                            }
                        } else if (lockName.equals("")) {
                            if (!flagStaticLock) {
                                lockName = leftMethodName;
                            } else {
                                lockName = "this";
                            }
                        }
                    } else {
                        if (lenFourLockVar.equals("null")) {
                            lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                        } else {
                            lockName = lenFourLockVar;
                        }
                    }
//                    System.out.println(lockName + "====================");
//                    System.exit(-1);

                    /*
                    if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                        if (!globalStaticObject.isDefineObject) {
                            lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                            globalStaticObject.objectName = lockName;
                            globalStaticObject.isDefineObject = true;
                        } else {
                            lockName = globalStaticObject.objectName;
                        }
                    }*/
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    lockFile = ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava();
                }
            }
        } else {
            ReadWriteNode node = rwnList.get(0);

            if (!CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath)) {

                firstLoc = Integer.parseInt(node.getPosition().split(":")[1]);
                lastLoc = firstLoc;

//                System.out.println("lockp==========================");
                lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(), rwnList.get(0));
                if (lockName.equals("null")) {
                    lockName = acquireLockName(node, analyseJavaPath);
                } else if (lockName.equals("")) {
                    lockName = "this";

                }
//                System.out.println(lockName + "====================");
//                System.exit(-1);


                UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                if (!lockName.equals("this")) {
                    lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();
                }



                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                lockFile = analyseJavaPath;
            }
        }


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



        if (firstLoc > 0 && lastLoc > 0) {
            fixMethods += "aim at : " + rwnList.get(0) + "Lock start and stop position : " + firstLoc + "->" + lastLoc + '\n';

        }
        for (int index = firstLoc; index <= lastLoc + 1; index++) {
            Propagate.numSet.add(index);
        }
    }


    private static void addSynchronizedOfFourLengthPattern(ReadWriteNode[] rwnList) throws Exception {
        if (rwnList.length != 4) {
            throw new RuntimeException("the length of pattern should be 4 in this method");
        }
//        System.out.println(rwnList.toString());
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList[0].getPosition().split(":")[0];
        List<ReadWriteNode> part1 = new ArrayList<>();
        List<ReadWriteNode> part2 = new ArrayList<>();
        part1.add(rwnList[0]);
        for (int i = 1; i < 4; ++i) {
            if (rwnList[0].getThread().equals(rwnList[i].getThread())) {
                part1.add(rwnList[i]);
            } else {
                part2.add(rwnList[i]);
            }
        }
        int firstLoc = 0, lastLoc = 0;
        boolean flagSame = true;
        if (!part1.get(0).getPosition().split(":")[0].equals(part1.get(1).getPosition().split("0")[0])) {
            flagSame = false;
        }

        if (!flagSame) {

            useSoot.getCallGraph(part1.get(0), part1.get(1));


            if (useSoot.getSyncJava().equals(".")) {
                lockAdjust.setOneLockFile("");
                return;
            }


            firstLoc = useSoot.getMinLine();
            lastLoc = useSoot.getMaxLine();
        }
        int num1, num2;
        num1 = Integer.parseInt(part1.get(0).getPosition().substring(part1.get(0).getPosition().indexOf(':') + 1));
        num2 = Integer.parseInt(part1.get(1).getPosition().substring(part1.get(1).getPosition().indexOf(':') + 1));
        int part1Start = num1 < num2 ? num1 : num2;
        int part1End = num1 < num2 ? num2 + 1 : num1 + 1;
        num1 = Integer.parseInt(part2.get(0).getPosition().substring(part2.get(0).getPosition().indexOf(':') + 1));
        num2 = Integer.parseInt(part2.get(1).getPosition().substring(part2.get(1).getPosition().indexOf(':') + 1));
        int part2Start = num1 < num2 ? num1 : num2;
        int part2End = num1 < num2 ? num2 + 1 : num1 + 1;

        String lockp1 = LockP.acquireLockP(part1.get(0).getElement(), sequence.getNodes(), part1.get(0));
        String lockp2 = LockP.acquireLockP(part1.get(1).getElement(), sequence.getNodes(), part1.get(1));

        String lockVar;
        if (lockp1.equals("null") && lockp2.equals("null")) {

            lockVar = "null";
        } else if (lockp1.equals("null")) {
            lockVar = lockp2;
        } else if (lockp2.equals("null")) {
            lockVar = lockp1;
        } else {
            lockVar = LockP.acquireLockPOfTwoVar(part1.get(0).getElement(), part1.get(1).getElement(), sequence.getNodes(), part1.get(0));
        }

        firstLoc = Math.min(part1Start, part1Start);
        lastLoc = Math.max(part1End, part2End);
//        System.out.println(lockVar + firstLoc + lastLoc + "==============");
        if (lockVar.equals("null")) {
            lockVar = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
        }

//        if (!CheckWhetherLocked.check(rwnList[0].getPosition(), rwnList[0].getField(), sourceClassPath, analyseJavaPath)) {

        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc() + 1;


        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc() + 1;

        fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';

        if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
            lockVar = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
//                        System.out.println(lockName);
        }
        examplesIO.addLockToOneVar(firstLoc, lastLoc, lockVar, analyseJavaPath);

        LockPolicyPopularize.firstLoc = firstLoc;
        LockPolicyPopularize.lastLoc = lastLoc;
        LockPolicyPopularize.lockName = lockVar;
        LockPolicyPopularize.fixRelevantVar(analyseJavaPath);


        for (int i = 0; i <= rwnList.length; i = i + 3) {
            if (CheckWhetherLocked.check(rwnList[i].getPosition(), rwnList[i].getField(), sourceClassPath)) {
                int startLockLine = LockP.acqStartLine(rwnList[i], sequence.getNodes());
                int endLockLine = LockP.acqEndLine(rwnList[i], sequence.getNodes());
                LockP.deleteLock(startLockLine, endLockLine, analyseJavaPath);
            }
        }

    }


    public static String acquireLockName(ReadWriteNode node, String filePath) throws Exception {
        BufferedReader br = null;
        String read = "";
        String result = "";
        int line = 0;
        int poi = Integer.parseInt(node.getPosition().split(":")[1]);

        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
        while (((read = br.readLine()) != null)) {
            line++;
            if (line == poi) {
                String field = node.getField();
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


        if (!node.getElement().contains("@")) {
            result = "this";
        }

        fixMethods += "Lock Name : " + result.trim() + '\n';
        return result.trim();
    }


    private static ExistLock existLockName(ReadWriteNode node) throws Exception {
        ExistLock existLock = UseASTAnalysisClass.useASTCFindLockLine(node, addSyncFilePath);
        existLock = AcquireSyncName.acquireSync(existLock, addSyncFilePath);
        return existLock;
    }


    public static String whichObjectsFunction(int targetLine, String methodName, String filePath) throws Exception {
        BufferedReader br = null;
        String read = "";
        String result = "";
        int line = 0;

        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
        while (((read = br.readLine()) != null)) {
            line++;
            if (line == targetLine) {
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

        return result.trim();
    }


    private static void addSyncPatternOneToThree(Pattern patternCounter) throws Exception {

        int firstLoc = 0, lastLoc = 0;


        List<ReadWriteNode> rwnList = new ArrayList<ReadWriteNode>();
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            rwnList.add(patternCounter.getNodes()[i]);
        }


        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        boolean flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
        if (flagSame) {
            int oneLoc = Integer.parseInt(patternCounter.getNodes()[0].getPosition().split(":")[1]);
            int twoLoc = Integer.parseInt(patternCounter.getNodes()[1].getPosition().split(":")[1]);
            firstLoc = Math.min(oneLoc, twoLoc);
            lastLoc = Math.max(oneLoc, twoLoc);
//            String lockName = acquireLockName(patternCounter.getNodes()[0], analyseJavaPath);
            String lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(), rwnList.get(0));
            if (lockName.equals("null")) {
                lockName = acquireLockName(rwnList.get(0), analyseJavaPath);
            }
            if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath)) {

                if (!CheckWhetherLocked.check(patternCounter.getNodes()[0].getPosition(), patternCounter.getNodes()[0].getField(), sourceClassPath)) {

                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';


                    if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                        lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
//                        System.out.println(lockName);
                    }
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);
                    LockPolicyPopularize.firstLoc = firstLoc;
                    LockPolicyPopularize.lastLoc = lastLoc;
                    LockPolicyPopularize.lockName = lockName;
                    LockPolicyPopularize.fixRelevantVar(analyseJavaPath);
                    for(int index = firstLoc; index <= lastLoc + 1;index++){
                        Propagate.numSet.add(index);
                    }
                }
            }
            Propagate.p(lockName, patternCounter.getNodes()[0], sequence, sourceClassPath);
        } else {
            boolean needCross = true;
            for (int i = 0; i < 2; i++) {
                String position = patternCounter.getNodes()[i].getPosition();
                String[] positionArg = position.split(":");
                if (UseASTAnalysisClass.isConstructOrIsMemberVariable(Integer.parseInt(positionArg[1]), Integer.parseInt(positionArg[1]) + 1, analyseJavaPath)) {

                    needCross = false;
                }
            }
            if (!needCross) {
                String lockName = "";
                for (int i = 0; i < 2; i++) {
                    String position = patternCounter.getNodes()[i].getPosition();
                    String[] positionArg = position.split(":");


//                    String lockName = acquireLockName(patternCounter.getNodes()[i], analyseJavaPath);


                    lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(), rwnList.get(0));
                    if (lockName.equals("null")) {
                        lockName = acquireLockName(patternCounter.getNodes()[i], analyseJavaPath);
                    } else if (lockName.equals("")) {

                        lockName = "this";

                    }

                    firstLoc = Integer.parseInt(positionArg[1]);
                    lastLoc = firstLoc;

                    if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(Integer.parseInt(positionArg[1]), Integer.parseInt(positionArg[1]) + 1, analyseJavaPath)) {

                        if (!CheckWhetherLocked.check(position, patternCounter.getNodes()[i].getField(), sourceClassPath)) {
                            fixMethods += "Locked position" + Integer.parseInt(positionArg[1]) + '\n';

                            UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                            firstLoc = lockLine.getFirstLoc();
                            lastLoc = lockLine.getLastLoc();


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

                            examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                            lockAdjust.adjust(analyseJavaPath);
                            LockPolicyPopularize.fixRelevantVar(analyseJavaPath);

                            for (int index = firstLoc; index <= lastLoc + 1; index++) {
                                Propagate.numSet.add(index);
                            }
                        }
                    }
                }
                Propagate.p(lockName, patternCounter.getNodes()[0], sequence, sourceClassPath);
                return;
            }


            String lockName = "";

            String lockFile = "";


            boolean flagStaticLock = true;

            useSoot.getCallGraph(rwnList.get(0), rwnList.get(1));


            if (useSoot.getSyncJava().equals(".")) {
                lockAdjust.setOneLockFile("");
                return;
            }


            firstLoc = useSoot.getMinLine();
            lastLoc = useSoot.getMaxLine();
//System.out.println(firstLoc + ',' + lastLoc + ',' + useSoot.getSyncJava());System.exit(-1);
            String leftMethodName = whichObjectsFunction(firstLoc, GlobalStaticObject.leftMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
            String rightMethodName = whichObjectsFunction(lastLoc, GlobalStaticObject.rightMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());

            if (leftMethodName.equals(rightMethodName) && leftMethodName.length() > 0) {
                flagStaticLock = false;
            }



            if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava())) {

                UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();


                lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(), rwnList.get(0));
                if (lockName.equals("null")) {
                    if (flagStaticLock) {
                        lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                    } else {
                        lockName = leftMethodName;
                    }
                } else if (lockName.equals("this")) {
                    if (!flagStaticLock) {
                        lockName = leftMethodName;
                    }
                }

                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                lockFile = ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava();

                for (int index = firstLoc; index <= lastLoc + 1; index++) {
                    Propagate.numSet.add(index);
                }
                Propagate.numSet.add(Integer.parseInt(rwnList.get(0).getPosition().split(":")[1]));
                Propagate.numSet.add(Integer.parseInt(rwnList.get(1).getPosition().split(":")[1]));
                Propagate.p(lockName, rwnList.get(0), sequence, sourceClassPath);
            }
        }
    }


    private static void addSignal(Pattern patternCounter) throws Exception {

        int flagDefineLocation = Integer.MAX_VALUE;
        int flagAssertLocation = Integer.MIN_VALUE;
        for (int i = 0; i < 2; i++) {
            String position = patternCounter.getNodes()[i].getPosition();
            String[] positionArg = position.split(":");
            flagDefineLocation = Integer.parseInt(positionArg[1]) < flagDefineLocation ? Integer.parseInt(positionArg[1]) : flagDefineLocation;
            flagAssertLocation = Integer.parseInt(positionArg[1]) > flagAssertLocation ? Integer.parseInt(positionArg[1]) : flagAssertLocation;
        }

        fixMethods += "The location of semaphore defines : " + flagDefineLocation + '\n';
        fixMethods += "The location of semaphore use : " + flagAssertLocation + '\n';


        if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(flagAssertLocation, flagAssertLocation, addSyncFilePath) &&
                !UseASTAnalysisClass.isConstructOrIsMemberVariable(flagAssertLocation, flagAssertLocation, addSyncFilePath)) {

            examplesIO.addVolatileDefine(flagDefineLocation, "volatile bool flagFix = false;", addSyncFilePath);


            examplesIO.addVolatileIf(flagAssertLocation, addSyncFilePath);


            examplesIO.addVolatileToTrue(flagDefineLocation + 1, addSyncFilePath);
        }
    }
}
