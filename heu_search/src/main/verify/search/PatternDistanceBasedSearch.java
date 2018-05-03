package verify.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.vm.VM;
import verify.entity.sequence.Sequence;

import java.util.Comparator;

public class PatternDistanceBasedSearch extends DistanceBasedSearch {

    private static Comparator<Sequence> comparator = null;

    private class DistanceComparator implements Comparator<Sequence> {

        @Override
        public int compare(Sequence seq1, Sequence seq2) {
            int distanceComp = -Integer.compare(seq1.getDistance(), seq2.getDistance());
//            if (distanceComp == 0) {
//                //System.out.println(seq1.getThreadSwitch() + "," + seq2.getThreadSwitch());
//                return -Integer.compare(seq1.getThreadSwitch(), seq2.getThreadSwitch());
//            }
//            else {
//                return distanceComp;
//            }
            return distanceComp;
        }

    }

    public PatternDistanceBasedSearch(Config config, VM vm) {
        super(config, vm);
    }

    @Override
    protected Comparator<Sequence> getComparator() {
        if (comparator == null) {
            comparator = new DistanceComparator();
        }
        return comparator;
    }
}
