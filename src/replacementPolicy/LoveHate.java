package replacementPolicy;

import bufmgr.BufMgr;
import java.util.LinkedList;
import java.util.PriorityQueue;

public class LoveHate implements Policy {
	private BufMgr bufmgr;
	private LinkedList<Integer> love , hate ;
	private int[] order;
	private int priority ;
	
	public LoveHate(BufMgr BufferManager){
		// initially we set all pages to be hated 
		bufmgr = BufferManager;
		love = new LinkedList<Integer>();
		hate = new LinkedList<Integer>();
		for(int i=1;i<=bufmgr.getNumBufs();i++)
			hate.add(i);
		order = new int[bufmgr.getNumBufs() + 1];
		priority = 0;
	}

	@Override
	public void pinPage(int frame) {
		order[frame] = ++priority;
		
		if(priority>bufmgr.getNumBufs()*2)
			reset();
		remove(new Integer(frame));
	}

	@Override
	public void unpinPage(int frame) {
		if(bufmgr.getPinCount(frame-1)==0)
		{
			if(bufmgr.getLove(frame-1))
			{
				if(love.contains(frame)) return;
				int i=0;
				boolean entered=true;
				for(int e : love){
					if(order[e]<order[frame]){
						love.add(i, frame);
						entered=false;
						break;
					}
					i++;
				}

				if(entered)
					love.add(frame);
				
			} else {
				if(hate.contains(frame)) return;
				int i=0;
				boolean entered=true;
				for(int e : hate){
					if(order[e]>order[frame]){
						hate.add(i, frame);
						entered=false;
						break;
					}
					i++;
				}

				if(entered)
					hate.add(frame);
			}
		}
	}

	@Override
	public int requestPage() {
		if(hate.size()==0)
		{
			if(love.size()==0)
				return 0;
			return love.remove(0);
		}
		return hate.remove(0);
	}

	@Override
	public void remove(int frame) {
		if(!hate.remove((Object)frame)){
			love.remove((Object)frame);
		}
	}

	@Override
	public int getSize() {
		return love.size()+hate.size();
	}
	
	public void reset()
	{
		PriorityQueue<IntegerPair> sort = new PriorityQueue<IntegerPair>();
		for(int i=0;i<bufmgr.getNumBufs();i++)
			sort.add(new IntegerPair(i+1,order[i+1]));
		
		IntegerPair u;
		int count=1;
		while(!sort.isEmpty())
		{
			u = sort.poll();
			order[u.f1] = count++; 
		}
		priority = count;
	}
	
}
