package neld9968;

import com.sun.xml.internal.bind.v2.runtime.RuntimeUtil.ToStringAdapter;

public class PredictedState {
	
	public int flags = 0;
	public boolean hasFlag = false;
	public boolean isChasingFlag = false;
	
	public PredictedState(){
		
	}
	public PredictedState(PredictedState state){
		this.flags = state.flags;
		this.hasFlag = state.hasFlag;
		this.isChasingFlag = state.isChasingFlag;
	}
	
	@Override
	public String toString(){
		return "Flags: " + flags + ", hasFlag: " + hasFlag + ", isChasingFlag: " + isChasingFlag;
	}
	
}
