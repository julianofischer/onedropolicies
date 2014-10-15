package report;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class ForwardingDeliveredReport extends Report implements MessageListener{

	private HashMap<String,Message> forwardingDelivered;
	private HashMap<String,Message> delivereds;
	int fDelivered = 0;
	int f=0;
	
	public ForwardingDeliveredReport(){
		forwardingDelivered = new HashMap<String,Message>();
		delivereds = new HashMap<String,Message>();
	}
	
	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
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
		f++;
		
		if(delivereds.containsKey(m.getId())){
			forwardingDelivered.put(m.getId(), m);
			fDelivered++;
		}
		
		if(firstDelivery){
			delivereds.put(m.getId(),m);
		}
	}

	@Override
	public void newMessage(Message m) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void done(){
		double perc = (fDelivered*100.0)/f;
		write("Encaminhamentos: "+f);
		write("Delivereds: "+fDelivered);
		write("Delivered(%): "+perc);
		
		super.done();
	}
	
}
