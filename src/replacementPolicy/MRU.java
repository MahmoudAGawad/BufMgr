package replacementPolicy;

import java.util.LinkedList;
import java.util.PriorityQueue;

import bufmgr.BufMgr;

public class MRU implements Policy{

	private LinkedList<Integer> list;
	private int[] order;
	private BufMgr bufmgr;
	private int periorty;
	
	public MRU (BufMgr BufferManager)
	{
		bufmgr = BufferManager;
		list = new LinkedList<Integer>();
		order = new int[bufmgr.getNumBufs() + 1];
		periorty = 0;
		for(int i=1;i<=bufmgr.getNumBufs();i++)
			list.add(i);

	}
	
	@Override
	public void pinPage(int frame) {
		order[frame] = ++periorty;
		if(periorty>bufmgr.getNumBufs()*2)
			reset();
		
		list.remove(new Integer(frame));
		

	}

	@Override
	public void unpinPage(int frame) {
		
		if(bufmgr.getPinCount(frame-1)==0)
		{
			if(list.contains(frame)) return;
			int i=0;
			boolean entered=true;
			for(int e : list){
				if(order[e]<order[frame]){
					list.add(i, frame);
					entered=false;
					break;
				}
				i++;
			}

			if(entered)
				list.add(frame);
		}
	}

	@Override
	public int requestPage() {
		if(list.size()==0)
			return 0;
		for(int x: list)
			System.out.print(x+" ");
		System.out.println();
		return list.removeFirst();
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
		periorty = count;
	}

	@Override
	public void remove(int frame) {
		
		list.remove((Object)frame);
		
	}

	@Override
	public int getSize() {
		return list.size();
	}
	

}
