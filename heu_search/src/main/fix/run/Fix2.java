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

    //��һ�������õ���pattern
//    static List<Unicorn.PatternCounter> firstList = null;

    //�õ���sequence
    static Sequence sequence = null;

    //length 4 lockVar
    static String lenFourLockVar = "";

    public static void main(String[] args) {
        startUnicornTime = System.currentTimeMillis();

        try {
            fix(FixType.firstFix);
//            //Ҫ��
//            while(fixMethods.contains("fix fail")){
//                step++;
//                fix(FixType.iterateFix);
//            }
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

    private static void fix(int type) throws Exception {


        String verifyClasspath = ImportPath.verifyPath + "/generateClass";//Ҫ��֤��class·��

        //��������м�������
        if (firstDirPath.contains(".")) {
            firstDirPath = firstDirPath.replaceAll("\\.", "/");
        }

        sourceClassPath = ImportPath.examplesRootPath + "/out/production/Patch";

        //�õ�pattern����
        List<Unicorn.PatternCounter> firstList = Unicorn.getPatternCounterList(sourceClassPath);


        //������Ϊ2��ɾ��
        //ʵ���϶ԣ������Ͽ��ǲ�ȫ��
        /*for(int i = tempList.size() - 1;i >=0; i--) {
            if(tempList.get(i).getPattern().getNodes().length == 2)
                tempList.remove(i);
        }*/

        endUnicornTime = System.currentTimeMillis();
        patternListTime = "time for getting pattern list : " + (endUnicornTime - startUnicornTime);

        //�����е�pattern��ӡ�����������Ժ�ѡ��
//        System.out.println(firstList);

        //������patternд���ļ�
        InsertCode.writeLogFile(firstList.toString() + '\n' + patternListTime, "pattern list");


       /* System.out.println("if no correct pattern,please restart");
        System.out.print("select correct pattern number(The bottom one is zero):");*/

        //�˴���Ҫ�ֶ�ѡ��
//        Scanner sc = new Scanner(System.in);
//        int whichToUse = sc.nextInt();//ʹ�õڼ���pattern

        startFixTime = System.currentTimeMillis();
       /* int whichToUse = 0;

        Unicorn.PatternCounter patternCounter = tempList.get(tempList.size() - 1 - whichToUse);*/

        int i = 0;
        while (i < firstList.size()) {
//        while (i < 1) {
            Unicorn.PatternCounter patternCounter = firstList.get(i);
//            Unicorn.PatternCounter patternCounter = firstList.get(whichToUse);
            //������¼
//            reorder.r(firstList, i + 1,patternCounter.getPattern().getNodes()[0].getField(),patternCounter.getPattern().getNodes()[0].getPosition());
            i++;
            if (type == FixType.firstFix) {
                //�Ƚ���Ŀ������exportExamples
                dirPath = examplesIO.copyFromOneDirToAnotherAndChangeFilePath("examples", "exportExamples", firstDirPath);

            } else if (type == FixType.iterateFix) {
                dirPath = iterateDirPath;
                sourceClassPath = ImportPath.verifyPath + "/generateClass";
            }

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
//                InsertCode.writeLogFile(patternCounter.getFirstFailAppearPlace().toString(), "sequence for fix");
                sequence = patternCounter.getFirstFailAppearPlace();

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
                System.out.println("Not this pattern");
            }
//sc.nextInt();whichToUse++;
        }
    }

    //����pattern�ĳ���ִ�в�ͬ��fix����
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

    //����Ϊ2���ȷ����
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

        //����Ϊ3�Ӷ�����
        if (patternCounter.getNodes().length == 3) {
            //���ݻ�õ�list�����м���
            addSynchronized(threadA, AddSyncType.localSync);
            lockAdjust.setOneLockFinish(true);//��ʾ��һ��ִ����
            addSynchronized(threadB, AddSyncType.localSync);
            lockAdjust.adjust(addSyncFilePath);//�ϲ���
        } else if (patternCounter.getNodes().length == 4) {//����Ϊ4,��ʱ��Ҫ�Ӿ�̬ȫ����
            boolean flagSame1 = UseASTAnalysisClass.assertSameFunction(threadA, ImportPath.examplesRootPath + "/exportExamples/" + threadA.get(0).getPosition().split(":")[0]);
            boolean flagSame2 = UseASTAnalysisClass.assertSameFunction(threadB, ImportPath.examplesRootPath + "/exportExamples/" + threadB.get(0).getPosition().split(":")[0]);

            if(!flagSame1 && !flagSame2) {

                //acquire lockP
                String lockp1 = LockP.acquireLockP(threadA.get(0).getElement(), sequence.getNodes(), threadA.get(0));
                String lockp2 = LockP.acquireLockP(threadB.get(0).getElement(), sequence.getNodes(), threadB.get(0));


                String lockVar = "";
                //compare them
                if (lockp1.equals("null") && lockp2.equals("null")) {
                    //add new lock
                    lockVar = "null";
                }
                else if (lockp1.equals("null")) {
                    lockVar = lockp2;
                }
                else if (lockp2.equals("null")) {
                    lockVar = lockp1;
                }
                else {
                    lockVar = LockP.acquireLockPOfTwoVar(threadA.get(0).getElement(), threadA.get(0).getElement(), sequence.getNodes(), threadA.get(0));
                }


                lenFourLockVar = lockVar;
                //���ݻ�õ�list�����м���
                addSynchronized(threadA, AddSyncType.globalStaticSync);
                lockAdjust.setOneLockFinish(true);//��ʾ��һ��ִ����
                addSynchronized(threadB, AddSyncType.globalStaticSync);
                lockAdjust.adjust(addSyncFilePath);//�ϲ���
            }else {
                addSynchronizedOfFourLengthPattern(patternCounter.getNodes());
            }
        }

        //������������
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

    //��һ���߳��е�node���м���
    private static void addSynchronized(List<ReadWriteNode> rwnList, int type) throws Exception {
        int firstLoc = 0, lastLoc = 0;

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
                //�����ͬһ���ļ��з���
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
                    if (type == AddSyncType.localSync) {
                        if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath, analyseJavaPath)) {//����Ƿ������
                            if (i == 1 && varHasLock == true) {//��ʾ����������
                                return;//ֱ�ӽ���
                            } else {
                                varHasLock = true;//������Ϊtrue
                                existLock = existLockName(rwnList.get(i));
                            }
                        }
                    } else {
                        //������Ϊ4��һ���Դ���ʱ����ʹԭ�ȱ���������������Ҫ����
                        //������
                        if (CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath, analyseJavaPath)) {
                            existLock = existLockName(rwnList.get(i));
                            existGlobalLockStart = Math.min(existGlobalLockStart, existLock.getStartLine());
                            existGlobalLockEnd = Math.max(existGlobalLockEnd, existLock.getEndLine());
                        }
                    }
                    //Ӧ��Ҫ��ʲô��
                    if (type == AddSyncType.localSync) {//��Ҫ��Ӿֲ���,�˴���ʾ����Ϊ3
                        //���Ż�
//                        lockName = acquireLockName(node, analyseJavaPath);
//                        System.out.println("lockp==========================");
                        lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(),rwnList.get(0));
                        if(lockName.equals("null")) {
                            lockName = acquireLockName(node, analyseJavaPath);
                        }
//                        System.out.println(lockName + "====================");
//                        System.exit(-1);


                    } else if (type == AddSyncType.globalStaticSync) {//leng 4
                        if (!globalStaticObject.isDefineObject) {
                            if(lenFourLockVar.equals("null")) {
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

                //�жϼ��������ڲ��ڹ��캯�������߼��������ǲ��ǳ�Ա����
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc, analyseJavaPath)) {

                    //�жϼ����᲻���forѭ���Ƚ���
                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //���᲻�ᶨ����������ڣ�ʹ�ñ���������
                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //�����Ǽ�this�������ܻ�����¼ӵ�ͬ����ԭ���غϵ��������Ҫ�ϲ�
                    if (!lockName.equals("this")) {
                        lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                        firstLoc = lockLine.getFirstLoc();
                        lastLoc = lockLine.getLastLoc();
                    }

                    //�����ط���û�м���
                    if (!varHasLock) {
                        if (type == AddSyncType.globalStaticSync) {
                            firstLoc = Math.min(firstLoc, existGlobalLockStart);
                            lastLoc = Math.max(lastLoc, existGlobalLockEnd);
                        }
                        /*//����run()������Ҫȫ�־�̬��
                        if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                            if (!globalStaticObject.isDefineObject) {
                                lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                                globalStaticObject.objectName = lockName;
                                globalStaticObject.isDefineObject = true;
                            } else {
                                lockName = globalStaticObject.objectName;
                            }
                        }*/
                        //����
                        examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);
                    } else {//�м����ģ�ֱ���޸�ԭ����
                        UseOldSyncToFix.adjustOldSync(existLock.getLockName(), firstLoc, lastLoc + 1, existLock.getStartLine(), existLock.getEndLine(), analyseJavaPath);
                    }

                    lockFile = analyseJavaPath;
                }
            } else {//����һ��������
                //��������
                useSoot.getCallGraph(rwnList.get(0), rwnList.get(1));

                //�������û�ҵ�����ֱ�ӽ���
                if (useSoot.getSyncJava().equals(".")) {
                    lockAdjust.setOneLockFile("");//��Ϊ�գ��Ժ�Ͳ���ϲ���
                    return;
                }

                //�õ�����λ��
                firstLoc = useSoot.getMinLine();
                lastLoc = useSoot.getMaxLine();
//System.out.println(firstLoc + ',' + lastLoc + ',' + useSoot.getSyncJava());System.exit(-1);
                String leftMethodName = whichObjectsFunction(firstLoc, GlobalStaticObject.leftMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                String rightMethodName = whichObjectsFunction(lastLoc, GlobalStaticObject.rightMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                //���pattern��λ���ı�����ĳ������ı��������Ӿ�̬��,����stringbuffer�ܵ�����
                if (leftMethodName.equals(rightMethodName) && leftMethodName.length() > 0) {
                    flagStaticLock = false;
                }
                /*//���pattern����ͬһ���࣬��ô����֮��ӵ���this��
                String classNameOne = rwnList.get(0).getPosition().split("\\.")[0].replaceAll("/", ".");
                String classNameTwo = rwnList.get(1).getPosition().split("\\.")[0].replaceAll("/", ".");
                if (classNameOne.equals(classNameTwo)) {
                    examplesIO.addLockToOneVar(useSoot.getMinLine(), useSoot.getMaxLine() + 1, "this", ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                } else {
                    examplesIO.addLockToOneVar(useSoot.getMinLine(), useSoot.getMaxLine() + 1, "obj", ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                }*/


                //�жϼ��������ڲ��ڹ��캯�������߼��������ǲ��ǳ�Ա����
                if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava())) {
                    //�жϼ����᲻���forѭ���Ƚ���
                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //���᲻�ᶨ����������ڣ�ʹ�ñ���������
                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //�����Ǽ�this�������ܻ�����¼ӵ�ͬ����ԭ���غϵ��������Ҫ�ϲ�
                    lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();
                    //�ݶ�Ϊ���Ӿ�̬����this����һ����
                    //��������stringbuffer���֣��Ӿ�̬��Ҳ��һ���ԣ�Ҫ���ǵ������ĸ�����ı���

//                    System.out.println("lockp==========================");
                    if (type == AddSyncType.localSync) {//��Ҫ��Ӿֲ���,�˴���ʾ����Ϊ3)
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
                            //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                            if (!flagStaticLock) {
                                lockName = leftMethodName;
                            } else {
                                lockName = "this";
                            }
                        }
                    } else {
                        if(lenFourLockVar.equals("null")) {
                            lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                        } else {
                            lockName = lenFourLockVar;
                        }
                    }
//                    System.out.println(lockName + "====================");
//                    System.exit(-1);

                    /*//����run()������Ҫȫ�־�̬��
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
            //����һ��������������Ƿ��Ѿ�������
            //����Ϊ4��һ�㲻���������֧
            ReadWriteNode node = rwnList.get(0);

            if (!CheckWhetherLocked.check(node.getPosition(), node.getField(), sourceClassPath, analyseJavaPath)) {

                //û�������������Ҫ����������
                firstLoc = Integer.parseInt(node.getPosition().split(":")[1]);
                lastLoc = firstLoc;
                //Ȼ������Ҫ�Ӻ�����
                //������ȡ���Ĳ���
//                System.out.println("lockp==========================");
                lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(),rwnList.get(0));
                if(lockName.equals("null")) {
                    lockName = acquireLockName(node, analyseJavaPath);
                }else if(lockName.equals("")){
                    //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                    lockName = "this";

                }
//                System.out.println(lockName + "====================");
//                System.exit(-1);


                //�жϼ����᲻���forѭ���Ƚ���
                UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                //���᲻�ᶨ����������ڣ�ʹ�ñ���������
                lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                //�����Ǽ�this�������ܻ�����¼ӵ�ͬ����ԭ���غϵ��������Ҫ�ϲ�
                if (!lockName.equals("this")) {
                    lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();
                }


                //Ȼ�����
                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);

                lockFile = analyseJavaPath;
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
            }
            else {
                part2.add(rwnList[i]);
            }
        }
        int firstLoc = 0, lastLoc = 0;
        boolean flagSame = true;
        if(!part1.get(0).getPosition().split(":")[0].equals(part1.get(1).getPosition().split("0")[0])){
            flagSame = false;
        }

        if(!flagSame){
            //��������
            useSoot.getCallGraph(part1.get(0), part1.get(1));

            //�������û�ҵ�����ֱ�ӽ���
            if (useSoot.getSyncJava().equals(".")) {
                lockAdjust.setOneLockFile("");//��Ϊ�գ��Ժ�Ͳ���ϲ���
                return;
            }

            //�õ�����λ��
            firstLoc = useSoot.getMinLine();
            lastLoc = useSoot.getMaxLine();
        }
        int num1, num2;
//        System.out.println(part1.get(0).getPosition().substring(part1.get(0).getPosition().indexOf(':') + 1));
//        System.out.println(part1.get(1).getPosition().substring(part1.get(1).getPosition().indexOf(':') + 1));
        num1 = Integer.parseInt(part1.get(0).getPosition().substring(part1.get(0).getPosition().indexOf(':') + 1));
        num2 = Integer.parseInt(part1.get(1).getPosition().substring(part1.get(1).getPosition().indexOf(':') + 1));
        int part1Start = num1 < num2 ? num1 : num2;
        int part1End = num1 < num2 ? num2 + 1 : num1 + 1;
//        System.out.println(part1Start + "||||" + part1End);
        num1 = Integer.parseInt(part2.get(0).getPosition().substring(part2.get(0).getPosition().indexOf(':') + 1));
        num2 = Integer.parseInt(part2.get(1).getPosition().substring(part2.get(1).getPosition().indexOf(':') + 1));
        int part2Start = num1 < num2 ? num1 : num2;
        int part2End = num1 < num2 ? num2 + 1 : num1 + 1;
//        System.out.println(part2Start + "||||" + part2End);

        String lockp1 = LockP.acquireLockP(part1.get(0).getElement(), sequence.getNodes(), part1.get(0));
        String lockp2 = LockP.acquireLockP(part1.get(1).getElement(), sequence.getNodes(), part1.get(1));
//        System.out.println("LockP1: " + lockp1);
//        System.out.println("LockP2: " + lockp2);

        String lockVar;
        if (lockp1.equals("null") && lockp2.equals("null")) {
            //����µ�������
            lockVar = "null";
        }
        else if (lockp1.equals("null")) {
            lockVar = lockp2;
        }
        else if (lockp2.equals("null")) {
            lockVar = lockp1;
        }
        else {
            lockVar = LockP.acquireLockPOfTwoVar(part1.get(0).getElement(), part1.get(1).getElement(), sequence.getNodes(), part1.get(0));
        }

        firstLoc = Math.min(part1Start,part1Start);
        lastLoc = Math.max(part1End, part2End);
//        System.out.println(lockVar + firstLoc + lastLoc + "==============");
        if(lockVar.equals("null")){
            lockVar = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
        }
        //����Ƿ�������ټ���
//        if (!CheckWhetherLocked.check(rwnList[0].getPosition(), rwnList[0].getField(), sourceClassPath, analyseJavaPath)) {
        //�жϼ����᲻���forѭ���Ƚ���
        UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc() + 1;

        //���᲻�ᶨ����������ڣ�ʹ�ñ���������
        lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
        firstLoc = lockLine.getFirstLoc();
        lastLoc = lockLine.getLastLoc() + 1;

        fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';
        //����wronglock2��������������run()������Ҫȫ�־�̬��
        if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
            lockVar = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
//                        System.out.println(lockName);
        }
        examplesIO.addLockToOneVar(firstLoc, lastLoc, lockVar, analyseJavaPath);//����

        //������
        String temVar = "";
        int count = 0;
        for(int i = 0; i <= rwnList.length; i=i+3) {
            if (CheckWhetherLocked.check(rwnList[i].getPosition(), rwnList[i].getField(), sourceClassPath, analyseJavaPath)) {
                int startLockLine = LockP.acqStartLine(rwnList[i], sequence.getNodes());
                int endLockLine = LockP.acqEndLine(rwnList[i], sequence.getNodes());
//                System.out.println(startLockLine + "," + endLockLine);
                LockP.deleteLock(startLockLine,endLockLine,analyseJavaPath);
            }
        }
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

    //�����������
    private static ExistLock existLockName(ReadWriteNode node) throws Exception {
        ExistLock existLock = UseASTAnalysisClass.useASTCFindLockLine(node, addSyncFilePath);
        existLock = AcquireSyncName.acquireSync(existLock, addSyncFilePath);
        return existLock;
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

    //�Գ���Ϊ2��pattern���ͬ��
    private static void addSyncPatternOneToThree(Pattern patternCounter) throws Exception {

        int firstLoc = 0, lastLoc = 0;

        //�ж��ڲ���һ�� ������
        List<ReadWriteNode> rwnList = new ArrayList<ReadWriteNode>();
        for (int i = 0; i < patternCounter.getNodes().length; i++) {
            rwnList.add(patternCounter.getNodes()[i]);
        }

        //��ǰ���������ĸ��ļ���
        String analyseJavaPath = ImportPath.examplesRootPath + "/exportExamples/" + rwnList.get(0).getPosition().split(":")[0];

        boolean flagSame = UseASTAnalysisClass.assertSameFunction(rwnList, analyseJavaPath);
        if (flagSame) {//��һ��������
            int oneLoc = Integer.parseInt(patternCounter.getNodes()[0].getPosition().split(":")[1]);
            int twoLoc = Integer.parseInt(patternCounter.getNodes()[1].getPosition().split(":")[1]);
            firstLoc = Math.min(oneLoc, twoLoc);
            lastLoc = Math.max(oneLoc, twoLoc);
//            String lockName = acquireLockName(patternCounter.getNodes()[0], analyseJavaPath);
            String lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(),rwnList.get(0));
            if(lockName.equals("null")) {
                lockName = acquireLockName(rwnList.get(0), analyseJavaPath);
            }
            if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc + 1, analyseJavaPath)) {
                //����
                //����Ƿ�������ټ���
                if (!CheckWhetherLocked.check(patternCounter.getNodes()[0].getPosition(), patternCounter.getNodes()[0].getField(), sourceClassPath, analyseJavaPath)) {
                    //�жϼ����᲻���forѭ���Ƚ���
                    UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    //���᲻�ᶨ����������ڣ�ʹ�ñ���������
                    lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, analyseJavaPath);
                    firstLoc = lockLine.getFirstLoc();
                    lastLoc = lockLine.getLastLoc();

                    fixMethods += "Locked position : " + firstLoc + "->" + (lastLoc + 1) + '\n';

                    //����wronglock2��������������run()������Ҫȫ�־�̬��
                    if (UseASTAnalysisClass.checkInRun(firstLoc, lastLoc, analyseJavaPath)) {
                        lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
//                        System.out.println(lockName);
                    }
                    examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//����
                    LockPolicyPopularize.firstLoc = firstLoc;
                    LockPolicyPopularize.lastLoc = lastLoc;
                    LockPolicyPopularize.lockName = lockName;
                    LockPolicyPopularize.fixRelevantVar(analyseJavaPath);
                }
            }
        } else {//����һ��������
            //����Ϊ2����ʱ�򲻿���

            //���ǿ纯���޸��ȽϺ�

            //���������룬�÷��ڲ��ڹ��캯����ڵĻ����������޸�
            boolean needCross = true;
            for (int i = 0; i < 2; i++) {
                String position = patternCounter.getNodes()[i].getPosition();
                String[] positionArg = position.split(":");
                if (UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(Integer.parseInt(positionArg[1]), Integer.parseInt(positionArg[1]) + 1, analyseJavaPath)) {
                    //�ڹ��캯��,��Ҫ�����޸�
                    needCross = false;
                }
            }
//            System.out.println("zaibbuzai" + needCross);
            if(!needCross){
                for (int i = 0; i < 2; i++) {
                    String position = patternCounter.getNodes()[i].getPosition();
                    String[] positionArg = position.split(":");

                    //��ȡҪ����������
                    //�����������ֱ�������е���
                    //���û�У���Ѱ������
//                    String lockName = acquireLockName(patternCounter.getNodes()[i], analyseJavaPath);
                    String lockName = "";

                    lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(),rwnList.get(0));
                    if(lockName.equals("null")) {
                        lockName = acquireLockName(patternCounter.getNodes()[i], analyseJavaPath);
                    }else if(lockName.equals("")){
                        //����buggyprogram������������һ���ļ��У����õ����ַ���������thisһ������
                        lockName = "this";

                    }

                    //�˴�����һ�м�������������һ��
                    firstLoc = Integer.parseInt(positionArg[1]);
                    lastLoc = firstLoc;

                    if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(Integer.parseInt(positionArg[1]), Integer.parseInt(positionArg[1]) + 1, analyseJavaPath)) {
                        //����
                        //����Ƿ�������ټ���
                        if (!CheckWhetherLocked.check(position, patternCounter.getNodes()[i].getField(), sourceClassPath, analyseJavaPath)) {
                            fixMethods += "Locked position" + Integer.parseInt(positionArg[1]) + '\n';
                            //�ж�һ���ܲ����õ�ǰ����ֱ�ӽ����޸�

                            //�жϼ����᲻���forѭ���Ƚ���
                            UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, analyseJavaPath);
                            firstLoc = lockLine.getFirstLoc();
                            lastLoc = lockLine.getLastLoc();

                            //���᲻�ᶨ����������ڣ�ʹ�ñ���������
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
                            //����
                            examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, analyseJavaPath);//����

                            lockAdjust.adjust(analyseJavaPath);
                            LockPolicyPopularize.fixRelevantVar(analyseJavaPath);
                        }
                    }
                }
                return;
            }

            //��������
            String lockName = "";//������ʾ����������

            String lockFile = "";//������ʾ�����ļ�

            //�����޸�ʱ�Ƿ�ʹ�þ�̬��
            boolean flagStaticLock = true;

            useSoot.getCallGraph(rwnList.get(0), rwnList.get(1));

            //�������û�ҵ�����ֱ�ӽ���
            if (useSoot.getSyncJava().equals(".")) {
                lockAdjust.setOneLockFile("");//��Ϊ�գ��Ժ�Ͳ���ϲ���
                return;
            }

            //�õ�����λ��
            firstLoc = useSoot.getMinLine();
            lastLoc = useSoot.getMaxLine();
//System.out.println(firstLoc + ',' + lastLoc + ',' + useSoot.getSyncJava());System.exit(-1);
            String leftMethodName = whichObjectsFunction(firstLoc, GlobalStaticObject.leftMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
            String rightMethodName = whichObjectsFunction(lastLoc, GlobalStaticObject.rightMethodName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
            //���pattern��λ���ı�����ĳ������ı��������Ӿ�̬��,����stringbuffer�ܵ�����
            if (leftMethodName.equals(rightMethodName) && leftMethodName.length() > 0) {
                flagStaticLock = false;
            }


            //�жϼ��������ڲ��ڹ��캯�������߼��������ǲ��ǳ�Ա����
            if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava())) {
                //�жϼ����᲻���forѭ���Ƚ���
                UseASTAnalysisClass.LockLine lockLine = UseASTAnalysisClass.changeLockLine(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                //���᲻�ᶨ����������ڣ�ʹ�ñ���������
                lockLine = UseASTAnalysisClass.useASTCheckVariableInLock(firstLoc, lastLoc, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();

                //�����Ǽ�this�������ܻ�����¼ӵ�ͬ����ԭ���غϵ��������Ҫ�ϲ�
                lockLine = UseASTAnalysisClass.useASTAdjustThisLock(firstLoc, lastLoc, analyseJavaPath);
                firstLoc = lockLine.getFirstLoc();
                lastLoc = lockLine.getLastLoc();
                //�ݶ�Ϊ���Ӿ�̬����this����һ����
                //��������stringbuffer���֣��Ӿ�̬��Ҳ��һ���ԣ�Ҫ���ǵ������ĸ�����ı���

                lockName = LockP.acquireLockP(rwnList.get(0).getElement(), sequence.getNodes(),rwnList.get(0));
                if(lockName.equals("null")) {
                    if (flagStaticLock) {
                        lockName = UseASTAnalysisClass.useASTToaddStaticObject(analyseJavaPath);
                    } else {
                        lockName = leftMethodName;
                    }
                } else if(lockName.equals("this")){
                    if(!flagStaticLock){
                        lockName = leftMethodName;
                    }
                }

                examplesIO.addLockToOneVar(firstLoc, lastLoc + 1, lockName, ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava());
                lockFile = ImportPath.examplesRootPath + "/exportExamples/" + useSoot.getSyncJava();
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
        if (!UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(flagAssertLocation, flagAssertLocation, addSyncFilePath) &&
                !UseASTAnalysisClass.isConstructOrIsMemberVariableOrReturn(flagAssertLocation, flagAssertLocation, addSyncFilePath)) {
            //����ź����Ķ���
            examplesIO.addVolatileDefine(flagDefineLocation, "volatile bool flagFix = false;", addSyncFilePath);//���޶�

            //����ź����ж�,
            examplesIO.addVolatileIf(flagAssertLocation, addSyncFilePath);//���޶�

            //����ź�Ϊtrue��������䣬�������Ӧ���ڶ���ĺ�һ��
            examplesIO.addVolatileToTrue(flagDefineLocation + 1, addSyncFilePath);//���޶�
        }
    }
}
