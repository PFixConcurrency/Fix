package p_heu.run;

import fix.entity.ImportPath;
import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import p_heu.entity.pattern.Pattern;

import java.util.HashSet;
import java.util.Set;

public class FixVerification {

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
                return verifyFlag;
            }

        }

        return verifyFlag;
    }
}
