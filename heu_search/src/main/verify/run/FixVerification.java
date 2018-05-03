package verify.run;

import fix.entity.ImportPath;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import p_heu.entity.pattern.Pattern;
import verify.entity.sequence.Sequence;
import verify.listener.BasicPatternFindingListener;
import verify.listener.SequenceProduceListener;

import java.util.HashSet;
import java.util.Set;

public class FixVerification {
    public static void main(String[] args) {
        boolean result = verify(ImportPath.examplesRootPath + "/out/production/Patch");
        System.out.println(result);
    }

    public static boolean verify(String classpath) {
        String[] str = new String[]{
                "+classpath=" + classpath,
                "+search.class=verify.search.PatternDistanceBasedSearch",
                ImportPath.projectName + "." + ImportPath.mainClassName};
        Config config = new Config(str);

        Sequence correctSeq = getCorrectSequence(classpath);
        if (!correctSeq.getResult()) {
            return false;
        }

        Set<Sequence> correctSeqs = new HashSet<>();
        correctSeqs.add(correctSeq);
        BasicPatternFindingListener listener = new BasicPatternFindingListener(correctSeqs);
        listener.setMod(BasicPatternFindingListener.MOD.VERIFY);
        JPF jpf = new JPF(config);
        jpf.addListener(listener);
        jpf.run();
        boolean findBug = listener.getErrorSequence() != null;
        return !findBug;
    }

    public static Sequence getCorrectSequence(String classpath){

        String[] str = new String[]{
                "+classpath=" + classpath,
                "+search.class=verify.search.SingleExecutionSearch",
                ImportPath.projectName + "." + ImportPath.mainClassName};
        Config config = new Config(str);
        JPF jpf = new JPF(config);
        SequenceProduceListener listener = new SequenceProduceListener();

        jpf.addListener(listener);
        jpf.run();
        return listener.getSequence();
    }

    //”√unicorn—È÷§≤π∂°
    public static boolean verifyByUnicorn(String classpath) {
        boolean verifyFlag = true;
        Pattern.setPatternSet("unicorn");

        for (int i = 0; i < 100; ++i) {
            String[] str = new String[]{
                    "+classpath=" + classpath,
                    "+search.class=p_heu.search.SingleExecutionSearch",
                    ImportPath.projectName + "." + ImportPath.mainClassName
            };
            Config config = new Config(str);
            JPF jpf = new JPF(config);
            p_heu.listener.SequenceProduceListener listener = new p_heu.listener.SequenceProduceListener();


            jpf.addListener(listener);
            jpf.run();

            p_heu.entity.sequence.Sequence seq = listener.getSequence();

            if (!seq.getResult()) {
                verifyFlag = false;
            }

        }

        return verifyFlag;
    }
}
