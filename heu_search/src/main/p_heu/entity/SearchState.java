package p_heu.entity;

import gov.nasa.jpf.vm.RestorableVMState;

public class SearchState {
	private int stateId;
	private RestorableVMState state;
	
	public SearchState(int stateId, RestorableVMState state) {
		this.stateId = stateId;
		this.state = state;
	}
	
	public int getStateId() {
		return this.stateId;
	}

	public RestorableVMState getState() {
		return this.state;
	}

	public String toString() {
	    return "SearchState[" + "stateId:" + stateId + "]";
    }
}
