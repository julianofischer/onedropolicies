/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import java.util.HashMap;
import java.util.Iterator;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/** Juliano Fischer Naves
 * jfischer@ic.uff.br
 * julianofischer@gmail.com
 */

/**
 * Report for of amount of messages delivered vs. time. A new report line
 * is created every time when either a message is created or delivered.
 * Messages created during the warm up period are ignored.
 * For output syntax, see {@link #HEADER}.
 */
public class CountDeliveredTimesReport extends Report implements MessageListener {
	
	public HashMap<String,Integer> deliveredMessages; 
	/**
	 * Constructor.
	 */
	public CountDeliveredTimesReport() {
		init();
		deliveredMessages = new HashMap<String,Integer>();
	}
	
	@Override
	public void init() {
		super.init();
	}

	public void messageTransferred(Message m, DTNHost from, DTNHost to, 
			boolean firstDelivery) {
		if (!isWarmup() && !isWarmupID(m.getId())) {
			if(!deliveredMessages.containsKey(m.getId())){
				deliveredMessages.put(m.getId(),1);
			}else{
				Integer count = deliveredMessages.get(m.getId());
				count = count + 1;
				deliveredMessages.put(m.getId(), count);
			}
		}
	}

	public void newMessage(Message m) {
	}
	
	/**
	 * Writes the current values to report file
	 */
	private void reportValues() {
		Iterator<String> keySet = deliveredMessages.keySet().iterator();
		int redundancy = 0;
		while(keySet.hasNext()){
			String id = (String) keySet.next();
			Integer value = deliveredMessages.get(id);
			redundancy = redundancy + value - 1;
			write(id+" "+value);
		}
		write("Redundancy: "+redundancy);
	}

	// nothing to implement for the rest
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

	@Override
	public void done() {
		reportValues();
		super.done();
	}
}
