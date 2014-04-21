package bufmgr;

import chainexception.ChainException;

public class BufferPoolExceededException extends ChainException {

   public BufferPoolExceededException( Exception ex ,String name)
   { 
     super(ex, name);
   }
}
