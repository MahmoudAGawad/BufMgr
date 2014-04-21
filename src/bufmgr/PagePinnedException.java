package bufmgr;

import chainexception.ChainException;

public class PagePinnedException extends ChainException{
	public PagePinnedException(Exception e, String s){
		super(e, s);
	}
}
