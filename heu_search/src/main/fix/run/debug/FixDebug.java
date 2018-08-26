package fix.run.debug;

import fix.analyzefile.*;
import fix.entity.GlobalStaticObject;
import fix.entity.ImportPath;
import fix.entity.LockPObject;
import fix.entity.lock.ExistLock;
import fix.io.ExamplesIO;
import fix.io.InsertCode;
import fix.run.Propagate;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.pattern.Pattern;
import p_heu.entity.sequence.Sequence;
import p_heu.run.Unicorn;
import soot.UseSoot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;

public class FixDebug {
    static ExamplesIO examplesIO = ExamplesIO.getInstance();
    static String firstDirPath = ImportPath.examplesRootPath + "/examples/" + ImportPath.projectName;//examples path
    static String dirPath = "";//exportExamples path

    static String whichCLassNeedSync = "";//the calss to add syn
    static LockAdjust lockAdjust = LockAdjust.getInstance();//merge syn

    static String fixMethods = "";//log fix method

    static String sourceClassPath = "";//Source code generation class, need to be recorded, because JPF analysis class

    static String addSyncFilePath = "";//the path to add syn

    //log time
    static long startUnicornTime = 0;
    static long endUnicornTime = 0;
    static long startFixTime = 0;
    static long endFixTime = 0;
    static String patternListTime = "";
    static String fixTime = "";

    //Global static variable
    static GlobalStaticObject globalStaticObject = GlobalStaticObject.getInstance();

    //fix cross methods
    static UseSoot useSoot = UseSoot.getInstance();

    //pattern list
    static List<Unicorn.PatternCounter> patternList = new ArrayList<Unicorn.PatternCounter>();

    //sequence
    static Sequence sequence = null;

    //the path to test
    static String verifyClasspath = ImportPath.verifyPath + "/generateClass";

    //syn location
    static int firstLoc = 0, lastLoc = 0;

    public static void main(String[] args) {
        try {

            startUnicornTime = System.currentTimeMillis();
            //identify pattern list
            acquirePatternList();

            endUnicornTime = System.currentTimeMillis();
            patternListTime = "time for getting pattern list : " + (endUnicornTime - startUnicornTime);
            startFixTime = System.currentTimeMillis();


            System.out.println(patternList);
            Scanner sc = new Scanner(System.in);
            int whichToUse = sc.nextInt();
            for (int i = 0; i < 1; i++) {
                Unicorn.PatternCounter patternCounter = patternList.get(whichToUse);

                //Copy the project to exportExamples
                dirPath = examplesIO.copyFromOneDirToAnotherAndChangeFilePath("examples", "exportExamples", firstDirPath);

                //According to pattern, we know which class to add syn
                String position = patternCounter.getPattern().getNodes()[0].getPosition();
                String[] tempSplit = position.split(":")[0].split("/");
                whichCLassNeedSync = tempSplit[tempSplit.length - 1];

                addSyncFilePath = ImportPath.examplesRootPath + "/exportExamples/" + position.split(":")[0];

                //Write the pattern to  file
//                InsertCode.writeLogFile(patternCounter.toString(), "pattern for fix");
                fixMethods = "pattern for fix : " + '\n' + patternCounter.toString() + '\n';


                //If pattern occurs in obtaining a call graph or does not appear in a failure, it results in an error, which is considered an error pattern because it provides the wrong location
                try {
                    //If it doesn't show up in a failure, it has nothing to do with the error. Skip it
                    if (patternCounter.getFirstFailAppearPlace() == null) {
                        continue;
                    }
                    //Get the sequence corresponding to the pattern
                    //Sequence that appears the first time during a failed run
                    RecordSequence.display(patternCounter.getFirstFailAppearPlace());

                    //write sequence to file
//                    InsertCode.writeLogFile(patternCounter.getFirstFailAppearPlace().toString(), "sequence for fix");

                    sequence = patternCounter.getFirstFailAppearPlace();

                    //identify locking policy
                    identifyLockP(patternCounter);

                    //clear
                    Propagate.clearSet();

                    //fix bugs
                    divideByLength(patternCounter);

                    //test the fixed program
                    fixMethods += "result : ";
                    if (Unicorn.verifyFixSuccessful(verifyClasspath)) {
                        fixMethods += "fix success\n";
                        break;
                    } else {
                        fixMethods += "fix fail\n";
                    }
                } catch (Exception e) {
                    System.out.println("not this");
                }

            }

        } catch (Exception e) {
            System.out.println("running error,please restart");
        } finally {
            endFixTime = System.currentTimeMillis();
            fixTime = "time for fixing:" + (endFixTime - startFixTime);
            //write fix method to file
            InsertCode.writeLogFile(fixMethods + patternListTime + '\n' + fixTime, "fix result");
            System.out.println("if still bug ,run again and again");
        }

    }


    public static void acquirePatternList() throws Exception {

        //package name has too many ".",need to be handled
        if (firstDirPath.contains(".")) {
            firstDirPath = firstDirPath.replaceAll("\\.", "/");
        }

        sourceClassPath = ImportPath.examplesRootPath + "/out/production/Patch";

        //get pattern list
        patternList = Unicorn.getPatternCounterList(sourceClassPath);

        //write pattern list to file
//        InsertCode.writeLogFile(patternList.toString() + '\n' + patternListTime, "pattern list");
    }

    public static void identifyLockP(Unicorn.PatternCounter patternCounter) throws Exception {
        if (patternCounter.getPattern().getNodes().length < 4) {
            ReadWriteNode readWriteNode = patternCounter.getPattern().getNodes()[0];
            LockP.acquireLockP(sequence.getNodes(), readWriteNode);
        } else {
            acquireLockPForLenFour(patternCounter);
        }

    }

    private static void divideByLength(Unicorn.PatternCounter patternCounter) throws Exception {
        int length = patternCounter.getPattern().getNodes().length;
        if (length == 2) {
//            fixMethods += "Fix one\n";
            usePatternToDistinguish(patternCounter.getPattern());
        } else if (length == 3) {
//            fixMethods += "Fix two\n";
            usePatternToAddSync(patternCounter.getPattern());
        } else if (length == 4) {
//            fixMethods += "Fix three\n";
            usePatternToAddSync(patternCounter.getPattern());
        }
    }

    private static void usePatternToDistinguish(Pattern patternCounter) throws Exception {
        if (RecordSequence.isLast(patternCounter.getNodes()[0]) || RecordSequence.isFirst(patternCounter.getNodes()[1])) {
            //length 2 add signal
            fixMethods += "Added signal\n";
            addSignal(patternCounter);
        } else {
            //length 2 add syn
            fixMethods += "Add synchronization\n";
            addSyncPatternOneToThree(patternCounter);
        }
    }

    //length 3 or 4, add syn
    private static void usePatternToAddSync(Pattern patternCounter) throws Exception {
        //The nodes are divided into two lists according to the thread
        List<ReadWriteNode> threadA = new ArrayList<ReadWriteNode>();//thread A
        List<ReadWriteNode> threadB = new ArrayList<ReadWriteNode>();//thread B
        String threadName = "";
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            ReadWriteNode node = patternCounter.getNodes()[i];
            if (i == 0) {//I'm going to put the first node in the list of A
                threadName = node.getThread();
                threadA.add(node);
            } else {
                if (threadName.equals(node.getThread())) {//Same thread, same list
                    threadA.add(node);
                } else {//put it in another
                    threadB.add(node);
                }
            }
        }

        if (patternCounter.getNodes().length == 3) {
            //add sync len 3
            addSynchronized(threadA);
            lockAdjust.setOneLockFinish(true);//The first execution is done
            addSynchronized(threadB);
            lockAdjust.adjust(addSyncFilePath);//merge syn
            Propagate.p(LockPObject.lockName, threadA.get(0), sequence, sourceClassPath);
        } else {
            //add sync len 4
            boolean flagSameA;
            if (!threadA.get(0).getPosition().split(":")[0].equals(threadA.get(1).getPosition().split(":")[0])) {
                flagSameA = false;
            } else {
                //same file,analyze whether same function
                flagSameA = UseASTAnalysisClass.assertSameFunction(threadA, ImportPath.examplesRootPath + "/exportExamples/" + threadA.get(0).getPosition().split(":")[0]);
            }
            boolean flagSameB;
            if (!threadB.get(0).getPosition().split(":")[0].equals(threadB.get(1).getPosition().split(":")[0])) {
                flagSameB = false;
            } else {
                //same file,analyze whether same function
                flagSameB = UseASTAnalysisClass.assertSameFunction(threadB, ImportPath.examplesRootPath + "/exportExamples/" + threadB.get(0).getPosition().split(":")[0]);
            }
            boolean flagSameCross;
            List<ReadWriteNode> threadCross = new ArrayList<ReadWriteNode>();//nodes in thread B
            threadCross.add(threadA.get(0));
            threadCross.add(threadB.get(0));
            if (!threadA.get(0).getPosition().split(":")[0].equals(threadB.get(0).getPosition().split(":")[0])) {
                flagSameCross = false;
            } else {
                //same file,analyze whether same function
                flagSameCross = UseASTAnalysisClass.assertSameFunction(threadCross, ImportPath.examplesRootPath + "/exportExamples/" + threadCross.get(0).getPosition().split(":")[0]);
            }

            if (flagSameA && flagSameB && flagSameCross) {
                //same function
                addSynchronizedOfFourLengthPattern(patternCounter.getNodes());
            } else {
                addSynchronized(threadA);
                lockAdjust.setOneLockFinish(true);//The first execution is done
                addSynchronized(threadB);
                lockAdjust.adjust(addSyncFilePath);//merge syn
                Propagate.p(LockPObject.lockName, threadA.get(0), sequence, sourceClassPath);
            }

        }
    }

    //Lock the node in a thread
    private static void addSynchronized(List<ReadWriteNode> rwnList) throws Exception {

        String lockName = "";//lock name

        String lockFile = "";//lock file

        //Determine how many variables exist£¬
        //If there are two variables,check whether on same line
        boolean flagInTwoLine = false;

        //Whether static locks are used when fixing across functions
        boolean flagStaticLock = true;

        if (rwnList.size() > 1) {
            String oneField = rwnList.get(0).getField();
            String onePosition = rwnList.get(0).getPosition();

            String twoField = rwnList.get(1).getField();
            String twoPosition = rwnList.get(1).getPosition();

            if (oneField.equals(twoField) && onePosition.equals(twoPosition)) {
                //Same row, same variable.We can treat it as a row.
                flagInTwoLine = false;
            } else {
                flagInTwoLine = true;
            }
        } else {//only one variable
            flagInTwoLine = false;
        }

        //Which files are currently being analyzed?£¿
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        //how many variables in list A
        if (flagInTwoLine) {//Two variables, and in different lines
            //If we have two variables, we need to analyze whether they are in same function
            boolean flagSame;
            //different file means different function
            if (!rwnList.get(0).getPosition().split(":")[0].equals(rwnList.get(1).getPosition().split(":")[0])) {
                flagSame = false;
            } else {
                //same file, check whether same function
                flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
            }

            if (flagSame) {//same function
                //whether has old syn
                boolean varHasLock = false;//reorder pattern whether has sync
                ExistLock existLock = null;

                int existGlobalLockStart = Integer.MAX_VALUE, existGlobalLockEnd = 0;
                //whether has sync£¬which syn, sync location
                //analyse list A
                for (int i = 0; i < rwnList.size(); i++) {
                    ReadWriteNode node = rwnList.get(i);
                    //For a local lock, check whether it has been locked
//                    if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath)) {
                    if (CheckWhetherLocked.checkHasSync(node, sequence)) {//whether exist sync
                        if (i == 1 && varHasLock == true) {//Both have locks
                            return;
                        } else {
                            varHasLock = true;//Lock marked true
                            existLock = existLockName(rwnList.get(i));
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

                //Determines if the sync region is in the constructor or if the variable to be locked is a member variable
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc, analyseJavaPath)) {

                    //adjust sync
                    adjustSyncScope(analyseJavaPath);

                    if (LockPObject.lockName.equals("null")) {//introduce lock
                        if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                            LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                        } else {
                            LockPObject.lockName = acquireLockName(rwnList.get(0), analyseJavaPath);
                        }
                    }
                    lockName = LockPObject.lockName;


                    //There are no locks in either place
                    if (!varHasLock) {
                        //add sync
                        examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);
                    } else {//Add the lock, directly modify the original lock
                        UseOldSyncToFix.adjustOldSync(existLock.getLockName(), firstLoc, lastLoc + 1, existLock.getStartLine(), existLock.getEndLine(), analyseJavaPath);
                    }

                    lockFile = analyseJavaPath;
                }
                Propagate.addSet(firstLoc, lastLoc);
            } else {//different function
                //search cross function
                useSoot.getCallGraph(rwnList.get(0), rwnList.get(1));

                //Put two lines in set
                UseASTAnalysisClass.addPropagateSet(Integer.parseInt(rwnList.get(0).getPosition().split(":")[1]), ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0]);
                UseASTAnalysisClass.addPropagateSet(Integer.parseInt(rwnList.get(1).getPosition().split(":")[1]), ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(1).getPosition().split(":")[0]);
//                Propagate.addSet(Integer.parseInt(rwnList.get(0).getPosition().split(":")[1]));
//                Propagate.addSet(Integer.parseInt(rwnList.get(1).getPosition().split(":")[1]));

                //If we don't find it, end it
                if (useSoot.getSyncJava().equals(".")) {
                    lockAdjust.setOneLockFile("");//Set it to null and it won't merge in the future
                    return;
                }

                //get location of syn
                firstLoc = useSoot.getMinLine();
                lastLoc = useSoot.getMaxLine();
                String leftMethodName = whichObjectsFunction(firstLoc, GlobalStaticObject.leftMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                String rightMethodName = whichObjectsFunction(lastLoc, GlobalStaticObject.rightMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                //If the variable that pattern locates is the variable of an object, without static lock, (stringbuffer)
                if (leftMethodName.equals(rightMethodName) && leftMethodName.length() > 0) {
                    flagStaticLock = false;
                }


                //This may not be locked in the original file
                analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava();

                //Determines if the sync region is in the constructor or if the variable to be locked is a member variable
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava())) {

                    //adjust sync
                    adjustSyncScope(analyseJavaPath);

                    //Consider variable from which object(stringbuffer)
                    if (LockPObject.lockName.equals("null")) {
                        if (flagStaticLock) {
                            LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                        } else {
                            LockPObject.lockName = leftMethodName;
                        }
                    } else if (LockPObject.lockName.equals("this")) {
                        if (!flagStaticLock) {
                            LockPObject.lockName = leftMethodName;
                        } else {
                            LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                        }
                    }
                    lockName = LockPObject.lockName;
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    lockFile = ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava();
                }
                Propagate.addSet(firstLoc, lastLoc);
            }
        } else {
            //For a variable, check that it has been locked
            //length 4, don't go in here
            ReadWriteNode node = rwnList.get(0);
            firstLoc = Integer.parseInt(node.getPosition().split(":")[1]);
            lastLoc = firstLoc;
            if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath) &&
//                    !CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath)) {
                    !CheckWhetherLocked.checkHasSync(node, sequence)) {

                //Unlocked, gets the number of rows that need to be locked


                if (LockPObject.lockName.equals("null")) {//introduce lock
                    if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                        LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                    } else {
                        LockPObject.lockName = acquireLockName(node, analyseJavaPath);
                    }
                }
                lockName = LockPObject.lockName;

                //adjust sync
                adjustSyncScope(analyseJavaPath);

                //add sync
                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                lockFile = analyseJavaPath;

                Propagate.addSet(firstLoc);
            }
        }

        //reorder sync location,for merge in the future
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


        //can be locked
        if (firstLoc > 0 && lastLoc > 0) {
            fixMethods += "Locked position : " + firstLoc + "->" + lastLoc + '\n';
        }
    }

    //len 4 and in the same function
    private static void addSynchronizedOfFourLengthPattern(ReadWriteNode[] rwnList) throws Exception {

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

        String lockName = "";

        int num1, num2;
        num1 = Integer.parseInt(part1.get(0).getPosition().substring(part1.get(0).getPosition().indexOf(':') + 1));
        num2 = Integer.parseInt(part1.get(1).getPosition().substring(part1.get(1).getPosition().indexOf(':') + 1));
        int part1Start = num1 < num2 ? num1 : num2;
        int part1End = num1 < num2 ? num2 + 1 : num1 + 1;
        num1 = Integer.parseInt(part2.get(0).getPosition().substring(part2.get(0).getPosition().indexOf(':') + 1));
        num2 = Integer.parseInt(part2.get(1).getPosition().substring(part2.get(1).getPosition().indexOf(':') + 1));
        int part2Start = num1 < num2 ? num1 : num2;
        int part2End = num1 < num2 ? num2 + 1 : num1 + 1;


        firstLoc = Math.min(part1Start, part1Start);
        lastLoc = Math.max(part1End, part2End);
        if (LockPObject.lockName.equals("null")) {
            LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
        }

        lockName = LockPObject.lockName;
        //adjust sync
        adjustSyncScope(analyseJavaPath);

        fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';

        //add sync
        examplesIO.addLockToOneVar(firstLoc, lastLoc, lockName, analyseJavaPath);

        //reduce sync
        for (int i = 0; i <= rwnList.length; i = i + 3) {
//            if (CheckWhetherLocked.check(rwnList[i].getPosition(), rwnList[i].getField(), sourceClassPath)) {
            if (CheckWhetherLocked.checkHasSync(rwnList[i], sequence)) {
                int startLockLine = LockP.acqStartLine(rwnList[i], sequence.getNodes());
                int endLockLine = LockP.acqEndLine(rwnList[i], sequence.getNodes());
                LockP.deleteLock(startLockLine, endLockLine, analyseJavaPath);
            }
        }

        lockPolicyPopularize(analyseJavaPath, lockName, rwnList[0].getField(), rwnList[3].getField());
    }


    //add signal to fix order violation
    private static void addSignal(Pattern patternCounter) throws Exception {
        //Get the smaller number of rows in pattern
        int flagDefineLocation = Integer.MAX_VALUE;//flag defined
        int flagAssertLocation = Integer.MIN_VALUE;//flag assert(wait)
        for (int i = 0; i < 2; i++) {
            String position = patternCounter.getNodes()[i].getPosition();
            String[] positionArg = position.split(":");
            flagDefineLocation = Integer.parseInt(positionArg[1]) < flagDefineLocation ? Integer.parseInt(positionArg[1]) : flagDefineLocation;
            flagAssertLocation = Integer.parseInt(positionArg[1]) > flagAssertLocation ? Integer.parseInt(positionArg[1]) : flagAssertLocation;
        }

        fixMethods += "The location of semaphore defines : " + flagDefineLocation + '\n';
        fixMethods += "The location of semaphore use : " + flagAssertLocation + '\n';

        //not in Construct Or MemberVariable
        if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(flagAssertLocation, flagAssertLocation, addSyncFilePath) &&
                !UseASTAnalysisClass.isConstructOrIsMemberVariable(flagDefineLocation, flagDefineLocation, addSyncFilePath)) {
            //add flag defined
//            examplesIO.addVolatileDefine(flagDefineLocation, "volatile bool flagFix = false;", addSyncFilePath);
            UseASTAnalysisClass.useASTToaddVoli(addSyncFilePath);
            //add flag assert(wait)
            examplesIO.addVolatileIf(flagAssertLocation, addSyncFilePath);

            //flag to be true, line after defined
            examplesIO.addVolatileToTrue(flagDefineLocation + 1, addSyncFilePath);
        }
    }

    //len 2 add syn
    private static void addSyncPatternOneToThree(Pattern patternCounter) throws Exception {

        //whether in same function
        List<ReadWriteNode> rwnList = new ArrayList<ReadWriteNode>();
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            rwnList.add(patternCounter.getNodes()[i]);
        }

        //Which files are currently being analyzed?
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        //lokc name
        String lockName = "";
        boolean flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
        if (flagSame) {//same function
            int oneLoc = Integer.parseInt(patternCounter.getNodes()[0].getPosition().split(":")[1]);
            int twoLoc = Integer.parseInt(patternCounter.getNodes()[1].getPosition().split(":")[1]);
            firstLoc = Math.min(oneLoc, twoLoc);
            lastLoc = Math.max(oneLoc, twoLoc);

            if (LockPObject.lockName.equals("null")) {//introduce lock
                if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                    LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                } else {
                    LockPObject.lockName = acquireLockName(rwnList.get(0), analyseJavaPath);
                }
            }
            lockName = LockPObject.lockName;
//            UseASTAnalysisClass.LockLine lockLine = AdjustSyncScope.result(firstLoc, lastLoc, sourceClassPath,analyseJavaPath,patternCounter.getNodes()[0]);
            if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath)) {
                //adjust sync
                adjustSyncScope(analyseJavaPath);

                fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';

                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                //merge sync
                len2MergeSync(analyseJavaPath);

            }
            Propagate.addSet(firstLoc, lastLoc);
        } else {//different functioin
            //not cross function
            for (int i = 0; i < 2; i++) {
                String position = patternCounter.getNodes()[i].getPosition();
                String[] positionArg = position.split(":");

                //Lock the same row here, so the number of rows is same
                firstLoc = Integer.parseInt(positionArg[1]);
                lastLoc = firstLoc;

                analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + position.split(":")[0];

                if (LockPObject.lockName.equals("null")) {//introduce
                    //When a lock is introduced in two functions, use a static lock
                    LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);

                }
                lockName = LockPObject.lockName;

                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath) &&
//                        !CheckWhetherLocked.check(position, patternCounter.getNodes()[i].getField(), sourceClassPath)) {
                        !CheckWhetherLocked.checkHasSync(patternCounter.getNodes()[i], sequence)) {
                    //adjust sync
                    adjustSyncScope(analyseJavaPath);

                    //add sync
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                    //The two functions do not need to merge locks
                }
                Propagate.addSet(firstLoc, lastLoc);
            }
        }
        Propagate.p(LockPObject.lockName, rwnList.get(0), sequence, sourceClassPath);
    }


    //adjust sync
    private static void adjustSyncScope(String analyseJavaPath) {

        //Determine if the lock will cross the for loop, etc
        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();

        //Check to see if the variable is defined inside the lock, and useed outside the lock
        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();
    }

    //merge sync
    private static void len2MergeSync(String analyseJavaPath) {
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

        lockAdjust.adjust(analyseJavaPath);
    }

    //which object that function belongs to
    public static String whichObjectsFunction(int targetLine, String methodName, String filePath) throws Exception {
        BufferedReader br = null;
        String read = "";//read
        String result = "";//deal with
        int line = 0;

        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
        while (((read = br.readLine()) != null)) {
            line++;
            if (line == targetLine) {//find that line
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

    //lock name
    private static ExistLock existLockName(ReadWriteNode node) throws Exception {
        ExistLock existLock = UseASTAnalysisClass.useASTCFindLockLine(node, addSyncFilePath);
        existLock = AcquireSyncName.acquireSync(existLock, addSyncFilePath);
        return existLock;
    }

    //acquire lockp len 4
    private static void acquireLockPForLenFour(Unicorn.PatternCounter patternCounter) throws Exception {
        String lockVar = "", lockEle = "";
        List<ReadWriteNode> part1 = new ArrayList<>();
        List<ReadWriteNode> part2 = new ArrayList<>();
        ReadWriteNode[] nodes = patternCounter.getPattern().getNodes();
        part1.add(nodes[0]);
        for (int i = 1; i < 4; ++i) {
            if (nodes[0].getThread().equals(nodes[i].getThread())) {
                part1.add(nodes[i]);
            } else {
                part2.add(nodes[i]);
            }
        }

//        String lockp1 = LockP.acquireLockP(part1.get(0).getElement(), sequence.getNodes(), part1.get(0));
//        String lockp2 = LockP.acquireLockP(part1.get(1).getElement(), sequence.getNodes(), part1.get(1));
        String lockp1 = "", lockp2 = "", lockp1Ele = "", lockp2Ele = "";
        LockP.acquireLockP(sequence.getNodes(), part1.get(0));
        lockp1 = LockPObject.lockName;
        lockp1Ele = LockPObject.element;
        LockP.acquireLockP(sequence.getNodes(), part1.get(1));
        lockp2 = LockPObject.lockName;
        lockp2Ele = LockPObject.element;

        if (lockp1.equals("null") && lockp2.equals("null")) {
            //Add a new lock variable
            lockVar = "null";
            lockEle = "null";
        } else if (lockp1.equals("null")) {
            lockVar = lockp2;
            lockEle = lockp2Ele;
        } else if (lockp2.equals("null")) {
            lockVar = lockp1;
            lockEle = lockp1Ele;
        } else {
            lockVar = LockP.acquireLockPOfTwoVar(part1.get(0).getElement(), part1.get(1).getElement(), sequence.getNodes(), part1.get(0));
            lockEle = LockPObject.element;
        }

        LockPObject.lockName = lockVar;
        LockPObject.element = lockEle;
    }

    //Read that line and then process the string
    //Gets the name of the lock
    public static String acquireLockName(ReadWriteNode node, String filePath) throws Exception {
        BufferedReader br = null;
        String read = "";//read
        String result = "";//deal with
        int line = 0;
        int poi = Integer.parseInt(node.getPosition().split(":")[1]);

        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
        while (((read = br.readLine()) != null)) {
            line++;
            if (line == poi) {//find that line
                String field = node.getField();//get variable
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

        //static variable
        //not contains @ is static
        if (!node.getElement().contains("@")) {
            result = node.getField();
        }

//        fixMethods += "Lock Name : " + result.trim() + '\n';
        return result.trim();
    }

    //relevent variable
    private static void lockPolicyPopularize(String analyseJavaPath, String lockName, String variableName1, String variableName2) {
        LockPolicyPopularize.firstLoc = firstLoc;
        LockPolicyPopularize.lastLoc = lastLoc;
        LockPolicyPopularize.lockName = lockName;
//        LockPolicyPopularize.relevantVariableSet.add(variableName1);
//        LockPolicyPopularize.relevantVariableSet.add(variableName2);
        LockPolicyPopularize.fixRelevantVar(analyseJavaPath);
//        MergePro.mergeLen4(analyseJavaPath, lockName);
    }
}
