package report;

import java.util.HashMap;
import java.util.Iterator;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class UselesslyLoopReport extends Report implements MessageListener{
	
	private HashMap<String,Integer> countDropped;
	
	public UselesslyLoopReport(){
		countDropped = new HashMap<String,Integer>();
	}
	
	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if(dropped){
			String id = where.getAddress()+":"+m.getId();
			if(!countDropped.containsKey(id)){
				countDropped.put(id,new Integer(1));
			}else{
				Integer value  = countDropped.get(id);
				value = value + 1;
				countDropped.put(id, value);
			}
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
		
		Iterator<String> keys = countDropped.keySet().iterator();
		
		while(keys.hasNext()){
			String key = (String)keys.next();
			Integer value = countDropped.get(key); 
			String[] s = key.split(":");
			write("Host: "+s[0]+" Message: "+s[1]+" "+value);
		}
		
		super.done();
	}
	
}
