package neld9968;

/**
 * Planning class that creates the predicted state
 * 
 * 
 * @author Luis & Brian
 */
public class PredictedState {
	
	public int flags = 0; // flags
	public boolean hasFlag = false; // does this have a flag
	public boolean isChasingFlag = false; // is this chasing the flag
	
	public PredictedState(){
		//empty
	}
	// build state based on another state's properties
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
