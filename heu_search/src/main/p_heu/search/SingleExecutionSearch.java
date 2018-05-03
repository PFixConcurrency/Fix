package p_heu.search;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.VM;

public class SingleExecutionSearch extends Search {
    public SingleExecutionSearch(Config config, VM vm) {
        super(config, vm);
    }

    @Override
    public void search() {
        boolean depthLimitReached = false;

        depth = 0;

        notifySearchStarted();

        while (!done) {
            if (checkAndResetBacktrackRequest() || !isNewState() || isEndState() || isIgnoredState() || depthLimitReached ) {
                break;
            }

            if (forward()) {
                depth++;
                notifyStateAdvanced();

                if (currentError != null){
                    notifyPropertyViolated();

                    if (hasPropertyTermination()) {
                        break;
                    }
                    // for search.multiple_errors we go on and treat this as a new state
                    // but hasPropertyTermination() will issue a backtrack request
                }

                if (depth >= depthLimit) {
                    depthLimitReached = true;
                    notifySearchConstraintHit("depth limit reached: " + depthLimit);
                    continue;
                }

                if (!checkStateSpaceLimit()) {
                    notifySearchConstraintHit("memory limit reached: " + minFreeMemory);
                    // can't go on, we exhausted our memory
                    break;
                }

            } else { // forward did not execute any instructions
                notifyStateProcessed();
            }
        }

        notifySearchFinished();
    }

    public boolean requestBacktrack () {
        doBacktrack = true;

        return true;
    }

    public boolean supportsBacktrack () {
        return true;
    }
}
