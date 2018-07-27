package p_heu.run;

import fix.entity.ImportPath;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.pattern.Pattern;
import p_heu.entity.sequence.Sequence;
import p_heu.listener.SequenceProduceListener;

import java.util.*;

public class Unicorn {


    //修复所依赖的pattern
    static List<PatternCounter> patternCountersList = new ArrayList<>();


    //测试用的main函数
    public static void main(String[] args) {

//        System.out.println(System.getProperty("user.dir"));

        /*System.setProperty("user.dir", "C:\\Users\\lhr\\Desktop\\verify\\HashSet");
        System.setProperty("projectName", "HashSet");data

        System.out.println(System.getProperty("projectName"));*/

//        System.out.println(Unicorn.getPatternCounterList(ImportPath.examplesRootPath + "/out/production/Patch"));
//        System.out.println(Unicorn.getPatternCounterList("C:\\Users\\lhr\\Desktop\\myt\\WrongLock\\outputClasses"));
//        System.out.println(Unicorn.getPatternCounterList(ImportPath.verifyPath + "\\generateClass"));
//        System.out.println(Unicorn.verifyFixSuccessful(ImportPath.verifyPath + "\\generateClass"));
        System.out.println(Unicorn.verifyFixSuccessful(ImportPath.examplesRootPath + "\\out\\production\\Patch"));
//        System.out.println(Unicorn.getPatternCounterList("C:\\Users\\lhr\\Desktop\\verify\\WrongLock\\outputClasses"));
//        System.out.println(Unicorn.getPatternCounterList());
//        System.out.println(Unicorn.getPatternCounterList());
        /*List<PatternCounter> patternCounters = Unicorn.getPatternCounterList(ImportPath.examplesRootPath + "/out/production/Patch");
        for (PatternCounter p : patternCounters) {
            System.out.println(p);
        }

        //获取sequence信息
        System.out.println(patternCounters.get(0).getFirstFailAppearPlace() + "sequence");*/
    }

    //获取pattern
    public static List<PatternCounter> getPatternCounterList(String classpath) {
        useUnicorn(classpath);
        return patternCountersList;
    }

    //获取验证结果
    public static boolean verifyFixSuccessful(String classpath) {
        /*useUnicorn(UnicornType.verify, classpath);
        //将拿到的pattern写入文件中
        InsertCode.writeToFile(patternCountersList.toString(), ImportPath.examplesRootPath + "\\logFile\\" + ImportPath.projectName + "\\verify pattern.txt");
        */


//        Examin中的验证，但是在这不成功

        //先将生成补丁后的程序编译成class文件
        //因为jpf文件要对class文件处理
        //架包路径一般可以为空，原来文件路径，目标路径
        /*Set<String> files = GenerateClass.getAllFiles(new File(ImportPath.verifyPath + "\\exportExamples\\" + ImportPath.projectName), ".java");
        Set<String> jars = new HashSet<String>();
        GenerateClass.compile(jars.toArray(new String[jars.size()]),
                files.toArray(new String[files.size()]),
                classpath);*/
        int status = GenerateClass.compileJava(ImportPath.verifyPath + "\\exportExamples\\" + ImportPath.projectName, classpath);
        if (status == 0) {
            return FixVerification.verifyByUnicorn(classpath);
        } else {
         return false;//没编译成功，直接算错
        }


    }

    private static void useUnicorn(String classpath) {
        Pattern.setPatternSet("unicorn");
        //将原来的清空
        patternCountersList.clear();

        for (int i = 0; i < 100; ++i) {
            String[] str = new String[]{
                    "+classpath=" + classpath + ";"
                            + ImportPath.unicornLibPath + "/javaee.jar;"
                            + ImportPath.unicornLibPath + "/jacontebe-1.0.jar;"
                            + ImportPath.unicornLibPath + "/jdmkrt.jar;"
                            + ImportPath.unicornLibPath + "/coring-1.4.jar;"
                            + ImportPath.unicornLibPath + "/commons-collections-2.1.jar;"
                            + ImportPath.unicornLibPath + "/commons-pool-1.2.jar;"
                            + ImportPath.unicornLibPath + "/mockito-all-1.9.5.jar",
                    "+search.class=p_heu.search.SingleExecutionSearch",
                    ImportPath.projectName + "." + ImportPath.mainClassName
            };
            Config config = new Config(str);
            JPF jpf = new JPF(config);
            SequenceProduceListener listener = new SequenceProduceListener();

            /*Filter filter = Filter.createFilePathFilter();
            listener.setPositionFilter(filter);*/

            jpf.addListener(listener);
            jpf.run();


            Sequence seq = listener.getSequence();
            //sequence中有时候会出现同一个线程对某个地方重复执行两次的情况
            //我们只记录第二次，放弃第一次
            //因为实际产生效果的是第二次
            //jpf中产生这种情况的原因不明
            seq = reduceSeq(seq);


            outer:
            for (Pattern pattern : seq.getPatterns()) {
                for (PatternCounter p : patternCountersList) {
                    if (p.getPattern().isSameExecptThread(pattern)) {
                        if (!seq.getResult() && p.getFirstFailAppearPlace() == null) {
                            p.setFirstFailAppearPlace(seq);
                        }
                        p.addOne(seq.getResult());
                        continue outer;
                    }
                }
                patternCountersList.add(new PatternCounter(pattern, seq.getResult(), seq.getResult() ? null : seq));
            }
        }

        Collections.sort(patternCountersList, new Comparator<PatternCounter>() {
            @Override
            public int compare(PatternCounter o1, PatternCounter o2) {
                double r1 = (double) o1.getSuccessCount() / (o1.getSuccessCount() + o1.getFailCount());
                double r2 = (double) o2.getSuccessCount() / (o2.getSuccessCount() + o2.getFailCount());
                return Double.compare(r1, r2) == 0 ?
                        Integer.compare(o1.getFailCount(), o2.getFailCount()) : Double.compare(r1, r2);//changed
            }
        });
    }


    private static Sequence reduceSeq(Sequence seq) {
        List<Node> nodesList = seq.getNodes();
        for (int i = 0; i < nodesList.size(); i++) {
            if (nodesList.get(i) instanceof ReadWriteNode) {
                for (int j = i - 1; j >= 0; j--) {
                    if (nodesList.get(j) instanceof ReadWriteNode) {
                        ReadWriteNode rwi = (ReadWriteNode) nodesList.get(i);
                        ReadWriteNode rwj = (ReadWriteNode) nodesList.get(j);
                        if ((rwi.getId() != rwj.getId()) && rwi.getElement().equals(rwj.getElement()) && rwi.getField().equals(rwj.getField()) && rwi.getType().equals(rwj.getType()) && rwi.getThread().equals(rwj.getThread()) && rwi.getPosition().equals(rwj.getPosition())) {
                            seq.getNodes().remove(j);
                            i--;
                        }
                    }
                }
            }
        }
        return seq;
    }


    public static class PatternCounter {
        private Pattern pattern;
        private int successCount;
        private int failCount;
        private Sequence firstFailAppearPlace;

        public Sequence getFirstFailAppearPlace() {
            return firstFailAppearPlace;
        }

        public void setFirstFailAppearPlace(Sequence firstFailAppearPlace) {
            this.firstFailAppearPlace = firstFailAppearPlace;
        }

        public Pattern getPattern() {
            return pattern;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public PatternCounter(Pattern pattern, boolean result) {
            this.pattern = pattern;
            this.firstFailAppearPlace = null;
            if (result) {
                successCount = 1;
                failCount = 0;
            } else {
                successCount = 0;
                failCount = 1;
            }
        }

        public PatternCounter(Pattern pattern, boolean result, Sequence ffap) {
            this.pattern = pattern;
            this.firstFailAppearPlace = ffap;
            if (result) {
                successCount = 1;
                failCount = 0;
            } else {
                successCount = 0;
                failCount = 1;
            }
        }

        public void addOne(boolean result) {
            if (result) {
                successCount += 1;
            } else {
                failCount += 1;
            }
        }

        public String toString() {
            return pattern.toString() + "\nsuccess count: " + this.successCount + "\nfail count: " + this.failCount;
        }
    }
}
