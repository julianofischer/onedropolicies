package report;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class HopCountDroppedReport extends Report implements MessageListener{

	private HashMap<String,Integer> messageHopCount;
	
	public HopCountDroppedReport(){
		messageHopCount = new HashMap<String,Integer>();
	}
	
	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if(dropped){
			this.messageHopCount.put(m.getId(),m.getHopCount());
		}
	}

	@Override
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
	}

	@Override
	public void newMessage(Message m) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void done(){
		Set<String> set = messageHopCount.keySet();		
		Iterator<String> it = set.iterator();
		
		while(it.hasNext()){
			String key = (String) it.next();
			write(key+" "+messageHopCount.get(key));
		}
		
		super.done();
	}
	
}
