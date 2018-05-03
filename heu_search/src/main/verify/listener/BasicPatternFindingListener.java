package verify.listener;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.*;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;
import verify.entity.Node;
import verify.entity.ReadWriteNode;
import verify.entity.ScheduleNode;
import verify.entity.SearchState;
import verify.entity.filter.Filter;
import verify.entity.sequence.Sequence;
import verify.search.DistanceBasedSearch;

import java.util.ArrayList;
import java.util.Set;

public class BasicPatternFindingListener extends ListenerAdapter {

    private Sequence sequence;
    private SearchState currentState;
    private ArrayList<Node> currentStateNodes;
    private int nodeId;
    private boolean execResult;
    private Filter positionFilter;
    private Set<Sequence> correctSeqs;
    private Sequence errorSequence;
    private MOD mod;

    public BasicPatternFindingListener(Set<Sequence> correctSeqs) {

        this.sequence = new Sequence(correctSeqs);
        this.correctSeqs = correctSeqs;
        currentState = null;
        currentStateNodes = null;
        nodeId = 0;
        execResult = true;
        positionFilter = null;
        errorSequence = null;
        mod = MOD.NORMAL;
    }

    public enum MOD {
        NORMAL,
        VERIFY
    }

    public MOD getMod() {
        return mod;
    }

    public void setMod(MOD mod) {
        this.mod = mod;
    }

    private void initCurrentState(VM vm, Search search) {

        DistanceBasedSearch dbsearch = (DistanceBasedSearch) search;
        //从search中，获取正确的序列个数
        correctSeqs = dbsearch.getCorrectSeqs();
        this.sequence = new Sequence(correctSeqs);
        currentState = new SearchState(vm.getStateId(),vm.getRestorableState());
        //将当前的sequence,传入search中
        if (currentState != null) {
            saveLastState();
            dbsearch.addCurrentSequence(sequence);
        }
        currentStateNodes = new ArrayList<>();
    }

    private void saveLastState() {
        sequence = sequence.advance(currentState.getStateId(),currentState.getState(), currentStateNodes);
    }

    public Sequence getSequence() {
        return sequence;
    }

    private int getNodeId() {
        return nodeId++;
    }

    public Set<Sequence> getCorrectSeqs() {
        return correctSeqs;
    }

    public Sequence getErrorSequence() {
        return errorSequence;
    }

    public void setPositionFilter(Filter filter) {
        positionFilter = filter;
    }

    @Override
    public void searchStarted(Search search) {
        super.searchStarted(search);
        DistanceBasedSearch dbsearch = (DistanceBasedSearch) search;
        //设置search中的正确序列，为初始化中第一个正确的执行序列
        dbsearch.setCorrectSeqs(correctSeqs);
        currentStateNodes = new ArrayList<>();
    }

    @Override
    public void stateAdvanced(Search search) {

        VM vm = search.getVM();
        initCurrentState(vm,search);
    }

    @Override
    public void stateRestored(Search search) {
        super.stateRestored(search);
    }

    @Override
    public void propertyViolated(Search search) {
        super.propertyViolated(search);
        execResult = false;
    }

    @Override
    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
        super.instructionExecuted(vm, currentThread, nextInstruction, executedInstruction);
        if (executedInstruction instanceof FieldInstruction) {
            FieldInstruction fins = (FieldInstruction)executedInstruction;
            if (positionFilter != null && !positionFilter.filter(fins.getFileLocation())) {
                return;
            }

            FieldInfo fi = fins.getFieldInfo();
            ElementInfo ei = fins.getElementInfo(currentThread);

            String type = fins.isRead() ? "READ" : "WRITE";
            String eiString = ei == null ? "null" : ei.toString();
            String fiName = fi.getName();
            ReadWriteNode node = new ReadWriteNode(getNodeId(), eiString, fiName, type, currentThread.getName(), fins.getFileLocation());
            currentStateNodes.add(node);
        }
    }

    @Override
    public void choiceGeneratorAdvanced(VM vm, ChoiceGenerator<?> currentCG) {
        super.choiceGeneratorAdvanced(vm, currentCG);
        if (currentCG instanceof ThreadChoiceFromSet) {

            ThreadInfo[] threads = ((ThreadChoiceFromSet)currentCG).getAllThreadChoices();
            if (threads.length == 1) {
                return;
            }
            ThreadInfo ti = (ThreadInfo)currentCG.getNextChoice();
            Instruction insn = ti.getPC();
            String type = insn.getClass().getName();
            ScheduleNode node = new ScheduleNode(getNodeId(), ti.getName(), insn.getFileLocation(), type);
            currentStateNodes.add(node);
        }
    }

    @Override
    public void searchFinished(Search search) {
        super.searchFinished(search);
        //VM vm = search.getVM();
        //initCurrentState(vm,search);
        errorSequence = ((DistanceBasedSearch) search).getErrorSequence();
        if (errorSequence != null) {
            errorSequence = errorSequence.advanceToEnd(currentState.getStateId(), currentState.getState(), sequence.getNodes(), execResult);
        }
        //System.out.println(errorSequence);
    }
}