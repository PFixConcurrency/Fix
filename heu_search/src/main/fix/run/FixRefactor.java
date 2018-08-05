package fix.run;

import fix.analyzefile.*;
import fix.entity.GlobalStaticObject;
import fix.entity.ImportPath;
import fix.entity.LockPObject;
import fix.entity.lock.ExistLock;
import fix.entity.type.AddSyncType;
import fix.io.ExamplesIO;
import fix.io.InsertCode;
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

public class FixRefactor {
    static ExamplesIO examplesIO = ExamplesIO.getInstance();
    static String firstDirPath = ImportPath.examplesRootPath + "/examples/" + ImportPath.projectName;//��һ���޸����ļ�·��
    static String dirPath = "";//��һ���޸����ļ�·��
    //    static String dirPath = ImportPath.examplesRootPath + "/exportExamples/" + ImportPath.projectName;//��һ���޸����ļ�·��
    static String iterateDirPath = ImportPath.examplesRootPath + "/exportExamples/" + ImportPath.projectName;//�����޸����ļ�·��

    static String whichCLassNeedSync = "";//��Ҫ���ͬ�����࣬�˴��費�迼���ڲ�ͬ��֮������������
    static LockAdjust lockAdjust = LockAdjust.getInstance();//��������ʱ�������ϲ���

    static String fixMethods = "";//��¼�޸�������д���ļ���

    static String sourceClassPath = "";//Դ����������࣬��¼�������Ժ���jpf����class

    static String addSyncFilePath = "";//����·��

    //������ʱ
    static long startUnicornTime = 0;
    static long endUnicornTime = 0;
    static long startFixTime = 0;
    static long endFixTime = 0;
    static String patternListTime = "";
    static String fixTime = "";

    //ȫ�־�̬����
    static GlobalStaticObject globalStaticObject = GlobalStaticObject.getInstance();

    //���ڿ����޸�
    static UseSoot useSoot = UseSoot.getInstance();

    //�õ���pattern
    static List<Unicorn.PatternCounter> patternList = new ArrayList<Unicorn.PatternCounter>();

    //�õ���sequence
    static Sequence sequence = null;

    //��֤·��
    static String verifyClasspath = ImportPath.verifyPath + "/generateClass";

    //��������
    static int firstLoc = 0, lastLoc = 0;

    public static void main(String[] args) {

        startUnicornTime = System.currentTimeMillis();
        try {

            //�õ�pattern
            acquirePatternList();

//            System.out.println(patternList);
//            Scanner sc = new Scanner(System.in);
//            int whichToUse = sc.nextInt();//ʹ�õڼ���pattern
            for (int i = 0; i < patternList.size(); i++) {
//            for (int i = 0; i < 1; i++) {
                Unicorn.PatternCounter patternCounter = patternList.get(i);
//                Unicorn.PatternCounter patternCounter = patternList.get(whichToUse);

                //�Ƚ���Ŀ������exportExamples
                dirPath = examplesIO.copyFromOneDirToAnotherAndChangeFilePath("examples", "exportExamples", firstDirPath);

                //fix
                //����pattern֪����Ҫ���ĸ����м���
                String position = patternCounter.getPattern().getNodes()[0].getPosition();
                String[] tempSplit = position.split(":")[0].split("/");
                whichCLassNeedSync = tempSplit[tempSplit.length - 1];

                addSyncFilePath = ImportPath.examplesRootPath + "/exportExamples/" + position.split(":")[0];

                //���õ���patternд���ļ���
                InsertCode.writeLogFile(patternCounter.toString(), "pattern for fix");


                //���pattern�ڻ�ȡcall graph����û��ʧ���г��֣��ᵼ�����д�������Ϊ�Ǵ����pattern����Ϊ���ṩ�˴����λ��
                try {
                    //û����ʧ���г��֣��ͱ�����������û��ϵ��ֱ������
                    if (patternCounter.getFirstFailAppearPlace() == null) {
                        continue;
                    }
                    //�õ���pattern��Ӧ��sequence
                    //��һ����ʧ�������г��ֵ�sequence
                    RecordSequence.display(patternCounter.getFirstFailAppearPlace());

                    //��sequenceд���ļ���
                    InsertCode.writeLogFile(patternCounter.getFirstFailAppearPlace().toString(), "sequence for fix");
                    sequence = patternCounter.getFirstFailAppearPlace();

                    //ʶ���������
                    identifyLockP(patternCounter);

                    //���
                    Propagate.clearSet();

                    //�Կ�������Ŀ�����޸�
                    divideByLength(patternCounter);

                    //����޸���ĳ����Ƿ���ȷ
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
            //���޸�����д���ļ���
            InsertCode.writeLogFile(fixMethods + fixTime, "fix result");
            System.out.println("if still bug ,run again and again");
        }

    }


    public static void acquirePatternList() throws Exception {

        //��������м�������
        if (firstDirPath.contains(".")) {
            firstDirPath = firstDirPath.replaceAll("\\.", "/");
        }

        sourceClassPath = ImportPath.examplesRootPath + "/out/production/Patch";

        //�õ�pattern����
        patternList = Unicorn.getPatternCounterList(sourceClassPath);

        //������patternд���ļ�
        InsertCode.writeLogFile(patternList.toString() + '\n' + patternListTime, "pattern list");
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
            //Ϊ����Ϊ2��pattern����ź���
            fixMethods += "Added Semaphore\n";
            addSignal(patternCounter);
        } else {
            //Ϊ����Ϊ2��pattern���ͬ��,��3��4�ǲ�ͬ�����
            fixMethods += "Add synchronization\n";
            addSyncPatternOneToThree(patternCounter);
        }
    }

    //����Ϊ3��4�����ͬ��
    private static void usePatternToAddSync(Pattern patternCounter) throws Exception {
        //�����߳̽���������Ϊ����list
        List<ReadWriteNode> threadA = new ArrayList<ReadWriteNode>();//�߳�A�Ľ��
        List<ReadWriteNode> threadB = new ArrayList<ReadWriteNode>();//�߳�B�Ľ��
        String threadName = "";
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            ReadWriteNode node = patternCounter.getNodes()[i];
            if (i == 0) {//�ѵ�һ��������A��list
                threadName = node.getThread();
                threadA.add(node);
            } else {
                if (threadName.equals(node.getThread())) {//�߳���ͬ������ͬһ��list
                    threadA.add(node);
                } else {//��ͬ�ͷ�����һ��list
                    threadB.add(node);
                }
            }
        }

        if (patternCounter.getNodes().length == 3) {
            //����3����
            addSynchronized(threadA);
            lockAdjust.setOneLockFinish(true);//��ʾ��һ��ִ����
            addSynchronized(threadB);
            lockAdjust.adjust(addSyncFilePath);//�ϲ���

            Propagate.p(LockPObject.lockName, threadA.get(0), sequence, sourceClassPath);
        } else {
            //����4����
            boolean flagSameA;
            if (!threadA.get(0).getPosition().split(":")[0].equals(threadA.get(1).getPosition().split(":")[0])) {
                flagSameA = false;
            } else {
                //�����ͬһ���ļ��з����ǲ���һ������
                flagSameA = UseASTAnalysisClass.assertSameFunction(threadA, ImportPath.examplesRootPath + "/exportExamples/" + threadA.get(0).getPosition().split(":")[0]);
            }
            boolean flagSameB;
            if (!threadB.get(0).getPosition().split(":")[0].equals(threadB.get(1).getPosition().split(":")[0])) {
                flagSameB = false;
            } else {
                //�����ͬһ���ļ��з����ǲ���һ������
                flagSameB = UseASTAnalysisClass.assertSameFunction(threadB, ImportPath.examplesRootPath + "/exportExamples/" + threadB.get(0).getPosition().split(":")[0]);
            }
            boolean flagSameCross;
            List<ReadWriteNode> threadCross = new ArrayList<ReadWriteNode>();//�߳�B�Ľ��
            threadCross.add(threadA.get(0));
            threadCross.add(threadB.get(0));
            if (!threadA.get(0).getPosition().split(":")[0].equals(threadB.get(0).getPosition().split(":")[0])) {
                flagSameCross = false;
            } else {
                //�����ͬһ���ļ��з����ǲ���һ������
                flagSameCross = UseASTAnalysisClass.assertSameFunction(threadCross, ImportPath.examplesRootPath + "/exportExamples/" + threadCross.get(0).getPosition().split(":")[0]);
            }

            if (flagSameA && flagSameB && flagSameCross) {
                //����һ��������
                addSynchronizedOfFourLengthPattern(patternCounter.getNodes());
            } else {//���Ƴ���3
                addSynchronized(threadA);
                lockAdjust.setOneLockFinish(true);//��ʾ��һ��ִ����
                addSynchronized(threadB);
                lockAdjust.adjust(addSyncFilePath);//�ϲ���
            }

        }
    }

    //��һ���߳��е�node���м���
    private static void addSynchronized(List<ReadWriteNode> rwnList) throws Exception {

        String lockName = "";//������ʾ����������

        String lockFile = "";//������ʾ�����ļ�

        //�ж��м���������
        //��������������ڲ���һ��
        boolean flagInTwoLine = false;

        //�����޸�ʱ�Ƿ�ʹ�þ�̬��
        boolean flagStaticLock = true;

        if (rwnList.size() > 1) {
            String oneField = rwnList.get(0).getField();
            String onePosition = rwnList.get(0).getPosition();

            String twoField = rwnList.get(1).getField();
            String twoPosition = rwnList.get(1).getPosition();

            if (oneField.equals(twoField) && onePosition.equals(twoPosition)) {
                //ͬһ�е�ͬһ�����������Ե���һ�д�������ʱ��Ӱ��
                flagInTwoLine = false;
            } else {
                flagInTwoLine = true;
            }
        } else {//ֻ��һ������,
            flagInTwoLine = false;
        }

        //��ǰ���������ĸ��ļ���
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        //�ж�A���м�������
        if (flagInTwoLine) {//��������,��������
            //�����������������Ҫ����
            //�ж������ڲ���һ��������
            boolean flagSame;
            //������������ļ��п϶�����һ������
            if (!rwnList.get(0).getPosition().split(":")[0].equals(rwnList.get(1).getPosition().split(":")[0])) {
                flagSame = false;
            } else {
                //�����ͬһ���ļ��з����ǲ���һ������
                flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
            }

            if (flagSame) {//��һ��������
                //������ԭ����û����
                boolean varHasLock = false;//��¼��ǰpattern�Ƿ����
                ExistLock existLock = null;

                int existGlobalLockStart = Integer.MAX_VALUE, existGlobalLockEnd = 0;
                //�ж�������û�м�������Ҫ�Ӻ�����������λ��
                //��A��list����
                for (int i = 0; i < rwnList.size(); i++) {
                    ReadWriteNode node = rwnList.get(i);
                    //���ھֲ������ȼ�����Ƿ��Ѿ�������

                    if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath)) {//����Ƿ������
                        if (i == 1 && varHasLock == true) {//��ʾ����������
                            return;//ֱ�ӽ���
                        } else {
                            varHasLock = true;//������Ϊtrue
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

                //�жϼ��������ڲ��ڹ��캯�������߼��������ǲ��ǳ�Ա����
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc, analyseJavaPath)) {

                    //�������ķ�Χ
                    adjustSyncScope(analyseJavaPath);

                    if (LockPObject.lockName.equals("null")) {//������
                        if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                            LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                        } else {
                            LockPObject.lockName = acquireLockName(rwnList.get(0), analyseJavaPath);
                        }
                    } else if (LockPObject.lockName.equals("")) {
                        //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                        LockPObject.lockName = "this";
                    }
                    lockName = LockPObject.lockName;


                    //�����ط���û�м���
                    if (!varHasLock) {
                        //����
                        examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);
                    } else {//�м����ģ�ֱ���޸�ԭ����
                        UseOldSyncToFix.adjustOldSync(existLock.getLockName(), firstLoc, lastLoc + 1, existLock.getStartLine(), existLock.getEndLine(), analyseJavaPath);
                    }

                    lockFile = analyseJavaPath;
                }
                Propagate.addSet(firstLoc, lastLoc);
            } else {//����һ��������
                //��������
                useSoot.getCallGraph(rwnList.get(0), rwnList.get(1));

                Propagate.addSet(Integer.parseInt(rwnList.get(0).getPosition().split(":")[1]));
                Propagate.addSet(Integer.parseInt(rwnList.get(1).getPosition().split(":")[1]));

                //�������û�ҵ�����ֱ�ӽ���
                if (useSoot.getSyncJava().equals(".")) {
                    lockAdjust.setOneLockFile("");//��Ϊ�գ��Ժ�Ͳ���ϲ���
                    return;
                }

                //�õ�����λ��
                firstLoc = useSoot.getMinLine();
                lastLoc = useSoot.getMaxLine();
                String leftMethodName = whichObjectsFunction(firstLoc, GlobalStaticObject.leftMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                String rightMethodName = whichObjectsFunction(lastLoc, GlobalStaticObject.rightMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                //���pattern��λ���ı�����ĳ������ı��������Ӿ�̬��,����stringbuffer�ܵ�����
                if (leftMethodName.equals(rightMethodName) && leftMethodName.length() > 0) {
                    flagStaticLock = false;
                }


                //�жϼ��������ڲ��ڹ��캯�������߼��������ǲ��ǳ�Ա����
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava())) {

                    //�������ķ�Χ
                    adjustSyncScope(analyseJavaPath);

                    //��������stringbuffer���֣�Ҫ���ǵ������ĸ�����ı���
                    if (LockPObject.lockName.equals("null")) {
                        if (flagStaticLock) {
                            LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                        } else {
                            LockPObject.lockName = leftMethodName;
                        }
                    } else if (LockPObject.lockName.equals("this")) {
                        if (!flagStaticLock) {
                            LockPObject.lockName = leftMethodName;
                        }
                    } else if (LockPObject.lockName.equals("")) {
                        //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                        if (!flagStaticLock) {
                            LockPObject.lockName = leftMethodName;
                        } else {
                            LockPObject.lockName = "this";
                        }
                    }
                    lockName = LockPObject.lockName;
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    lockFile = ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava();
                }
                Propagate.addSet(firstLoc, lastLoc);
            }
        } else {
            //����һ��������������Ƿ��Ѿ�������
            //����Ϊ4��һ�㲻���������֧
            ReadWriteNode node = rwnList.get(0);

            if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath) &&
                    !CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath)) {

                //û�������������Ҫ����������
                firstLoc = Integer.parseInt(node.getPosition().split(":")[1]);
                lastLoc = firstLoc;

                if (LockPObject.lockName.equals("null")) {//������
                    if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                        LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                    } else {
                        LockPObject.lockName = acquireLockName(node, analyseJavaPath);
                    }
                } else if (LockPObject.lockName.equals("")) {
                    //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                    LockPObject.lockName = "this";
                }
                lockName = LockPObject.lockName;

                //�������ķ�Χ
                adjustSyncScope(analyseJavaPath);

                //Ȼ�����
                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                lockFile = analyseJavaPath;

                Propagate.addSet(firstLoc);
            }
        }

        //��¼����λ��
        //�����Ժ����
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


        //��ʾ�ܼ���
        if (firstLoc > 0 && lastLoc > 0) {
            fixMethods += "aim at : " + rwnList.get(0) + "Lock start and stop position : " + firstLoc + "->" + lastLoc + '\n';
        }
    }

    //����4,����һ�������е��޸�
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
        //�������ķ�Χ
        adjustSyncScope(analyseJavaPath);

        fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';

        //����
        examplesIO.addLockToOneVar(firstLoc, lastLoc, lockName, analyseJavaPath);

        //������
        for (int i = 0; i <= rwnList.length; i = i + 3) {
            if (CheckWhetherLocked.check(rwnList[i].getPosition(), rwnList[i].getField(), sourceClassPath)) {
                int startLockLine = LockP.acqStartLine(rwnList[i], sequence.getNodes());
                int endLockLine = LockP.acqEndLine(rwnList[i], sequence.getNodes());
                LockP.deleteLock(startLockLine, endLockLine, analyseJavaPath);
            }
        }

    }

    //����ź����޸�˳��Υ��
    private static void addSignal(Pattern patternCounter) throws Exception {
        //�õ�pattern�н�С������
        int flagDefineLocation = Integer.MAX_VALUE;//flagӦ�������ж���
        int flagAssertLocation = Integer.MIN_VALUE;//flagӦ���������ж�
        for (int i = 0; i < 2; i++) {
            String position = patternCounter.getNodes()[i].getPosition();
            String[] positionArg = position.split(":");
            flagDefineLocation = Integer.parseInt(positionArg[1]) < flagDefineLocation ? Integer.parseInt(positionArg[1]) : flagDefineLocation;
            flagAssertLocation = Integer.parseInt(positionArg[1]) > flagAssertLocation ? Integer.parseInt(positionArg[1]) : flagAssertLocation;
        }

        fixMethods += "The location of semaphore defines : " + flagDefineLocation + '\n';
        fixMethods += "The location of semaphore use : " + flagAssertLocation + '\n';

        //���캯�����ܼ��ź���
        if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(flagAssertLocation, flagAssertLocation, addSyncFilePath) &&
                !UseASTAnalysisClass.isConstructOrIsMemberVariable(flagDefineLocation, flagDefineLocation, addSyncFilePath)) {
            //����ź����Ķ���
            examplesIO.addVolatileDefine(flagDefineLocation, "volatile bool flagFix = false;", addSyncFilePath);//���޶�

            //����ź����ж�,
            examplesIO.addVolatileIf(flagAssertLocation, addSyncFilePath);//���޶�

            //����ź�Ϊtrue��������䣬�������Ӧ���ڶ���ĺ�һ��
            examplesIO.addVolatileToTrue(flagDefineLocation + 1, addSyncFilePath);//���޶�
        }
    }

    //�Գ���Ϊ2��pattern���ͬ��
    private static void addSyncPatternOneToThree(Pattern patternCounter) throws Exception {

        //�ж��ڲ���һ�� ������
        List<ReadWriteNode> rwnList = new ArrayList<ReadWriteNode>();
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            rwnList.add(patternCounter.getNodes()[i]);
        }

        //��ǰ���������ĸ��ļ���
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        //����
        String lockName = "";
        boolean flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
        if (flagSame) {//��һ��������
            int oneLoc = Integer.parseInt(patternCounter.getNodes()[0].getPosition().split(":")[1]);
            int twoLoc = Integer.parseInt(patternCounter.getNodes()[1].getPosition().split(":")[1]);
            firstLoc = Math.min(oneLoc, twoLoc);
            lastLoc = Math.max(oneLoc, twoLoc);

            if (LockPObject.lockName.equals("null")) {//������
                if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                    LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                } else {
                    LockPObject.lockName = acquireLockName(rwnList.get(0), analyseJavaPath);
                }
            } else if (LockPObject.lockName.equals("")) {
                //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                LockPObject.lockName = "this";
            }
            lockName = LockPObject.lockName;
//            UseASTAnalysisClass.LockLine lockLine = AdjustSyncScope.result(firstLoc, lastLoc, sourceClassPath,analyseJavaPath,patternCounter.getNodes()[0]);
            if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath)) {
                //�������ķ�Χ
                adjustSyncScope(analyseJavaPath);

                fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';

                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//ֱ�Ӽ���

                //���ϲ�
                len2MergeSync(analyseJavaPath);

            }
            Propagate.addSet(firstLoc, lastLoc);
        } else {//����һ��������
            //����Ϊ2����ʱ�򲻿���,�������д��
            for (int i = 0; i < 2; i++) {
                String position = patternCounter.getNodes()[i].getPosition();
                String[] positionArg = position.split(":");

                //�˴�����һ�м�������������һ��
                firstLoc = Integer.parseInt(positionArg[1]);
                lastLoc = firstLoc;

                if (LockPObject.lockName.equals("null")) {//������
                    if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                        LockPObject.lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                    } else {
                        LockPObject.lockName = acquireLockName(patternCounter.getNodes()[i], analyseJavaPath);
                    }
                } else if (LockPObject.lockName.equals("")) {
                    //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                    LockPObject.lockName = "this";
                }
                lockName = LockPObject.lockName;

                if (!UseASTAnalysisClass.isConstructOrIsMemberVariable(firstLoc, lastLoc + 1, analyseJavaPath) &&
                        !CheckWhetherLocked.check(position, patternCounter.getNodes()[i].getField(), sourceClassPath)) {
                    //�������ķ�Χ
                    adjustSyncScope(analyseJavaPath);

                    //����
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//����

                    //������������Ҫ�ϲ���
                }
                Propagate.addSet(firstLoc, lastLoc);
            }
        }
        Propagate.p(LockPObject.lockName, rwnList.get(0), sequence, sourceClassPath);
    }


    //�������ķ�Χ
    private static void adjustSyncScope(String analyseJavaPath) {

        //�жϼ����᲻���forѭ���Ƚ���
        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();

        //���᲻�ᶨ����������ڣ�ʹ�ñ���������
        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc();
    }

    //���ϲ�
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

    //��ȡ���������ĸ�����
    public static String whichObjectsFunction(int targetLine, String methodName, String filePath) throws Exception {
        BufferedReader br = null;
        String read = "";//������
        String result = "";//��������
        int line = 0;

        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
        while (((read = br.readLine()) != null)) {
            line++;
            if (line == targetLine) {//�ҵ���һ��
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

    //�����������
    private static ExistLock existLockName(ReadWriteNode node) throws Exception {
        ExistLock existLock = UseASTAnalysisClass.useASTCFindLockLine(node, addSyncFilePath);
        existLock = AcquireSyncName.acquireSync(existLock, addSyncFilePath);
        return existLock;
    }

    //��ȡ����Ϊ4��lockp
    private static void acquireLockPForLenFour(Unicorn.PatternCounter patternCounter) throws Exception {
        String lockVar = "";
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
        String lockp1 = LockP.acquireLockP(part1.get(0).getElement(), sequence.getNodes(), part1.get(0));
        String lockp2 = LockP.acquireLockP(part1.get(1).getElement(), sequence.getNodes(), part1.get(1));


        if (lockp1.equals("null") && lockp2.equals("null")) {
            //����µ�������
            lockVar = "null";
        } else if (lockp1.equals("null")) {
            lockVar = lockp2;
        } else if (lockp2.equals("null")) {
            lockVar = lockp1;
        } else {
            lockVar = LockP.acquireLockPOfTwoVar(part1.get(0).getElement(), part1.get(1).getElement(), sequence.getNodes(), part1.get(0));
        }

        LockPObject.lockName = lockVar;
    }

    //������һ�У�Ȼ����ַ�������
    //��ȡ��������
    public static String acquireLockName(ReadWriteNode node, String filePath) throws Exception {
        BufferedReader br = null;
        String read = "";//������
        String result = "";//��������
        int line = 0;
        int poi = Integer.parseInt(node.getPosition().split(":")[1]);

        br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath)), "UTF-8"));
        while (((read = br.readLine()) != null)) {
            line++;
            if (line == poi) {//�ҵ���һ��
                String field = node.getField();//�õı���
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

        //��������static������ֱ�Ӽ�this������
        //��Ϊ���������@���ž��Ǿ�̬����
        if (!node.getElement().contains("@")) {
            result = "this";
        }

        fixMethods += "Lock Name : " + result.trim() + '\n';
        return result.trim();
    }
}
