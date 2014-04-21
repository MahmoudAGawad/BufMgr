package replacementPolicy;

import java.util.LinkedList;
import java.util.Queue;

import bufmgr.BufMgr;

public class FIFO implements Policy {
	
	private BufMgr bufmgr;
	Queue<Integer> list;
	
	public FIFO(BufMgr buf){
		bufmgr = buf;
		list = new LinkedList<>();
		int len = buf.getNumBufs();
		for(int i=1;i<=len;i++)
			list.add(i);
	}
	
	@Override
	public void pinPage(int frame) {
		list.remove(new Integer(frame));
	}

	@Override
	public void unpinPage(int frame) {
		if(bufmgr.getPinCount(frame-1)==0&& !list.contains(frame)){
			list.add(frame);
		}
	}

	@Override
	public int requestPage() {
		System.out.println("FIFO List Size = "+list.size());
		for(int x: list)
			System.out.print(x+" ");
		System.out.println();
		System.out.println(list.peek());
		return list.isEmpty()?0:list.poll();
	}
	
	public void remove(int frame){
		list.remove(new Integer(frame));
	}

	@Override
	public int getSize() {
		return list.size();
	}
}
