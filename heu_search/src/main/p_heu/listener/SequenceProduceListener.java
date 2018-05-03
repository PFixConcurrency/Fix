package p_heu.listener;

import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.vm.*;
import gov.nasa.jpf.vm.bytecode.FieldInstruction;
import gov.nasa.jpf.vm.choice.ThreadChoiceFromSet;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;
import p_heu.entity.ScheduleNode;
import p_heu.entity.SearchState;
import p_heu.entity.filter.Filter;
import p_heu.entity.pattern.Pattern;
import p_heu.entity.sequence.Sequence;

import java.util.ArrayList;
import java.util.List;

public class SequenceProduceListener extends ListenerAdapter {
    private Sequence sequence;
    private List<Node> currentStateNodes;
    private int nodeId;
    private SearchState currentState;
    private boolean execResult;
    private Filter positionFilter;

    public SequenceProduceListener() {
        this.sequence = new Sequence();
        nodeId = 0;
        currentStateNodes = null;
        currentState = null;
        execResult = true;
        positionFilter = null;
    }

    public void setPositionFilter(Filter filter) {
        positionFilter = filter;
    }

    public Sequence getSequence() {
        return sequence;
    }

    private int getNodeId() {
        return nodeId++;
    }

    public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction, Instruction executedInstruction) {
        if (executedInstruction instanceof FieldInstruction) {
            FieldInstruction fins = (FieldInstruction)executedInstruction;
            //position filter
            //System.out.println(fins.getFileLocation());

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

    public void choiceGeneratorAdvanced(VM vm, ChoiceGenerator<?> currentCG) {
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

    public void searchStarted(Search search) {
        VM vm = search.getVM();
        initCurrentState(vm);
    }

    public void stateAdvanced(Search search) {
        if (currentState != null) {
            saveLastState();
        }
        VM vm = search.getVM();
        initCurrentState(vm);
    }

    private void saveLastState() {
        sequence = sequence.advance(currentState.getStateId(), currentState.getState(), currentStateNodes);
    }

    private void initCurrentState(VM vm) {
        currentState = new SearchState(vm.getStateId(), vm.getRestorableState());
        currentStateNodes = new ArrayList<>();
    }

    public void propertyViolated(Search search) {
        execResult = false;
    }

    public void searchFinished(Search search) {
        sequence = sequence.advanceToEnd(currentState.getStateId(),currentState.getState(), currentStateNodes, execResult);
    }
}
