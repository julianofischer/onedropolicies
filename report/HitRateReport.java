package report;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class HitRateReport extends Report implements MessageListener{

	int droppingDelivered;
	int droppingUndelivered;
	int forwardingDelivered;
	int forwardingUndelivered;
	
	private HashMap<String,Message> delivereds;
	
	public HitRateReport(){
		droppingDelivered = 0;
		delivereds = new HashMap<String,Message>();
		droppingUndelivered = 0;
		forwardingDelivered = 0;
		forwardingUndelivered = 0;
	}
	
	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if(dropped){
			if(delivereds.containsKey(m.getId())){
				droppingDelivered++;
			}else{
				droppingUndelivered++;
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
		if(firstDelivery){
			delivereds.put(m.getId(),m);
		}else{
			if(delivereds.containsKey(m.getId())){
				this.forwardingDelivered++;
			}else{
				this.forwardingUndelivered++;
			}
		}
	}

	@Override
	public void newMessage(Message m) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void done(){
		int totalDropped = droppingDelivered + droppingUndelivered;
		int totalForwards = forwardingDelivered + forwardingUndelivered;
		double percForwardDelivered = (forwardingDelivered*100.0)/totalForwards;
		double percForwardUndelivered = (forwardingUndelivered*100.0)/totalForwards;
		
		double percDelivered = (droppingDelivered*100.0)/totalDropped;
		double percUndelivered = (droppingUndelivered*100.0)/totalDropped;
		write("Total Dropped: "+totalDropped);
		write("Dropping Delivereds: "+droppingDelivered);
		write("Dropping Undelivereds: "+droppingUndelivered);
		write("Dropping Delivereds (%)"+percDelivered);
		write("Dropping Undelivereds (%)"+percUndelivered);
		
		write("Total Forwards (Except first delivery): "+totalForwards);
		write("Forwarding Delivereds: "+forwardingDelivered);
		write("Forwarding Undelivereds: "+forwardingUndelivered);
		write("Forwarding Delivereds (%)"+percForwardDelivered);
		write("Forwarding Undelivereds (%)"+percForwardUndelivered);
		super.done();
	}
	
}
