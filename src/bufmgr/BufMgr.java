package bufmgr;

import java.io.IOException;
import java.util.Hashtable;
import java.util.PriorityQueue;
import global.PageId;
import global.SystemDefs;
import diskmgr.DiskMgrException;
import diskmgr.FileIOException;
import diskmgr.InvalidPageNumberException;
import diskmgr.InvalidRunSizeException;
import diskmgr.OutOfSpaceException;
import diskmgr.Page;
import replacementPolicy.*;

public class BufMgr {

	private byte[][] buffPool;
	private Page[] pagesInThePool;
	private String replacementPolicy;
	private int numBufs;

	private BufferDescriptor[] bufferDescriptors; // Map the frame to the pages
	private Hashtable<Integer, Integer> pageToFrameMap;
	private ReplacementPloicy policy;

	/**
	 * Create the BufMgr object Allocate pages (frames) for the buffer pool in
	 * main memory and make the buffer manager aware that the replacement policy
	 * is specified by replaceArg (i.e. FIFO, LRU, MRU, love/hate)
	 * 
	 * @param numbufs
	 *            number of buffers in the buffer pool
	 * @param replaceArg
	 *            name of the buffer replacement policy
	 */
	public BufMgr(int numBufs, String replaceArg) {
		this.numBufs = numBufs;
		int page_size = global.GlobalConst.MINIBASE_PAGESIZE;
		buffPool = new byte[numBufs][page_size];
		replacementPolicy = replaceArg;
		bufferDescriptors = new BufferDescriptor[numBufs];
		pageToFrameMap = new Hashtable<>();
		pagesInThePool = new Page[numBufs];
		policy = new ReplacementPloicy(replaceArg, this);
		for (int i = 0; i < numBufs; i++) {
			bufferDescriptors[i] = new BufferDescriptor(new PageId());
			pagesInThePool[i] = new Page();
		}
	}

	public int getNumBufs() {
		return numBufs;
	}

	public int getPinCount(int frame) {
		return bufferDescriptors[frame].getPinCount();
	}

	public boolean getLove(int frame) {
		return bufferDescriptors[frame].getLoved();
	}

	/**
	 * Pin a page First check if this page is already in the buffer pool. If it
	 * is, increment the pin_count and return pointer to this page. If the
	 * pin_count was 0 before the call, the page was a replacement candidate,
	 * but is no longer a candidate. If the page is not in the pool, choose a
	 * frame (from the set of replacement candidates) to hold this page, read
	 * the page (using the appropriate method from diskmgr package) and pin it.
	 * Also, must write out the old page in chosen frame if it is dirty before
	 * reading new page. (You can assume that emptyPage == false for this
	 * assignment.)
	 * 
	 * @param pgid
	 *            page number in the minibase.
	 * @param page
	 *            the pointer point to the page.
	 * @param emptyPage
	 *            true (empty page), false (nonempty page).
	 * @throws BufferPoolExceededException
	 */
	public void pinPage(PageId pgid, Page page, boolean emptyPage, boolean loved)
			throws BufferPoolExceededException {
		System.out.println("PIN Page : " + pgid.pid);
		if (pageToFrameMap.containsKey(pgid.pid)) {
			int indexOfThePage;
			BufferDescriptor currentDesc = bufferDescriptors[indexOfThePage = pageToFrameMap
					.get(pgid.pid)];
			policy.pinPage(indexOfThePage + 1);
			// if(currentDesc.getPinCount()==0){
			// policy.remove(indexOfThePage+1);
			// }
			currentDesc.updatePinCount(1); // pinCount++;
			page.setpage(buffPool[indexOfThePage]);
		} else {
			try {
				Page pg = new Page();

				SystemDefs.JavabaseDB.read_page(pgid, pg);
				page.setpage(pg.getpage());
				// page = pg;
				int index = policy.requestPage() - 1;
				if (index == -1) {
					throw (new BufferPoolExceededException(null, ""));
				}
				System.out.println("PageId : " + pgid.pid + " | index : "
						+ index);

				if (bufferDescriptors[index].isDirty()) {
					flushPage(bufferDescriptors[index].getPageNumber());
				}

				if (pageToFrameMap.containsKey(bufferDescriptors[index]
						.getPageNumber().pid)) {
					pageToFrameMap.remove(bufferDescriptors[index]
							.getPageNumber().pid);
				}

				pageToFrameMap.put(pgid.pid, index);

				buffPool[index] = pg.getpage();
				pagesInThePool[index].setpage(pg.getpage());

				policy.pinPage(index + 1);

				bufferDescriptors[index].setPinCount(1);
				bufferDescriptors[index].setPageNumber(new PageId(pgid.pid));
				bufferDescriptors[index].setDirty(false);
				bufferDescriptors[index].setLove(false);
				// candidateChoser.remove(index);

			} catch (InvalidPageNumberException | FileIOException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Unpin a page specified by a pageId. This method should be called with
	 * dirty == true if the client has modified the page. If so, this call
	 * should set the dirty bit for this frame. Further, if pin_count > 0, this
	 * method should decrement it. If pin_count = 0 before this call, throw an
	 * excpetion to report error. (for testing purposes, we ask you to throw an
	 * exception named PageUnpinnedExcpetion in case of error.)
	 * 
	 * @param pgid
	 *            page number in the minibase
	 * @param dirty
	 *            the dirty bit of the frame.
	 * @throws PageUnpinnedException
	 * @throws HashEntryNotFoundException
	 */
	public void unpinPage(PageId pgid, boolean dirty, boolean loved)
			throws PageUnpinnedException, HashEntryNotFoundException {
		System.out.println("UNPIN Page : " + pgid.pid);
		if (pageToFrameMap.containsKey(pgid.pid)) {
			int index = pageToFrameMap.get(pgid.pid);
			if (bufferDescriptors[index].getPinCount() == 0) {
				throw (new PageUnpinnedException(null, ""));
			}
			bufferDescriptors[index].updatePinCount(-1);
			// take care: two users unpinned the page with a true then false it
			// should remain true
			bufferDescriptors[index].setDirty(dirty
					| bufferDescriptors[index].isDirty());
			bufferDescriptors[index].setLove(loved);
			policy.unpinPage(index + 1);
		} else {
			System.out.println("HashTable Size = " + pageToFrameMap.size());
			System.out.println("PageId : " + pgid.pid);
			throw (new HashEntryNotFoundException(null, ""));
		}
	}

	/**
	 * Allocate new page(s). Call DB Object to allocate a run of new pages and
	 * find a frame in the buffer pool for the first page and pin it. (This call
	 * allows a client f the Buffer Manager to allocate pages on disk.) If
	 * buffer is full, i.e., you can\t find a frame for the first page, ask DB
	 * to deallocate all these pages, and return null.
	 * 
	 * @param firstPage
	 *            the address of the first page.
	 * @param howmany
	 *            total number of allocated new pages.
	 * 
	 * @return the first page id of the new pages. null, if error.
	 * @throws PagePinnedException
	 */
	public PageId newPage(Page firstPage, int howmany)
			throws BufferPoolExceededException {

		PageId firstPageId = new PageId();
		try {
			if (getNumUnpinnedBuffers() > 0) {
				firstPageId = new PageId();
				SystemDefs.JavabaseDB.allocate_page(firstPageId, howmany);
				pinPage(firstPageId, firstPage, false, false);
			}
		} catch (OutOfSpaceException | InvalidRunSizeException
				| InvalidPageNumberException | FileIOException
				| DiskMgrException | IOException e) {
			e.printStackTrace();
		}
		return firstPageId;
	}

	/**
	 * This method should be called to delete a page that is on disk. This
	 * routine must call the method in diskmgr package to deallocate the page.
	 * 
	 * @param pgid
	 *            the page number in the database.
	 * @throws PagePinnedException
	 * @throws HashEntryNotFoundException
	 * @throws PageUnpinnedException
	 */
	public void freePage(PageId pgid) throws PagePinnedException,
			PageUnpinnedException, HashEntryNotFoundException {
		try {
			System.out.println(pageToFrameMap.containsKey(pgid.pid));
			 if(pgid==null) return;
			if (pageToFrameMap.containsKey(pgid.pid)) {
				if (bufferDescriptors[pageToFrameMap.get(pgid.pid)]
						.getPinCount() > 1) {
					throw (new PagePinnedException(null, ""));
				}
				if (bufferDescriptors[pageToFrameMap.get(pgid.pid)]
						.getPinCount() == 1)
					unpinPage(pgid,false, false);
				pageToFrameMap.remove(pgid.pid);
				
			}
			SystemDefs.JavabaseDB.deallocate_page(pgid);
		} catch (InvalidRunSizeException | InvalidPageNumberException
				| FileIOException | DiskMgrException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void flushAllPages() {
		for (int i = 0; i < buffPool.length; i++) {
			if (bufferDescriptors[i].getPageNumber() != null) {
				flushPage(bufferDescriptors[i].getPageNumber());
			}
		}
	}

	/**
	 * Used to flush a particular page of the buffer pool to disk. This method
	 * calls the write_page method of the diskmgr package.
	 * 
	 * @param pgid
	 *            the page number in the database.
	 */
	public void flushPage(PageId pgid) {
		try {
			if (pageToFrameMap.get(pgid.pid) == null)
				return;
			SystemDefs.JavabaseDB.write_page(pgid,
					pagesInThePool[pageToFrameMap.get(pgid.pid)]);
		} catch (InvalidPageNumberException | FileIOException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public int getNumUnpinnedBuffers() {
		int len = policy.getSize();
		System.out.println(">>Unpinned Frames = " + len);
		return len;
	}
}
