package replacementPolicy;

import bufmgr.*;

public class ReplacementPloicy {
	Policy current;
	public ReplacementPloicy(String policy, BufMgr bufmgr)
	{
		if(policy.equalsIgnoreCase("Clock")||policy.equalsIgnoreCase("fifo"))
			current = new FIFO(bufmgr);
		else if(policy.equalsIgnoreCase("LRU"))
			current = new LRU(bufmgr);
		else if(policy.equalsIgnoreCase("lovehate"))
			current = new LoveHate(bufmgr);
		else if(policy.equalsIgnoreCase("MRU"))
			current = new MRU(bufmgr);
		else System.err.println("Unrecognized Replacement Policy!");
	}



	public void pinPage(int frame)
	{
		current.pinPage(frame);
	}


	public void unpinPage(int frame)
	{
		current.unpinPage(frame);
	}
	
	public int requestPage(){
		return current.requestPage();
	}
	
	public void remove(int frame){
		current.remove(frame);
	}
	
	public int getSize(){
		return current.getSize();
	}
}
