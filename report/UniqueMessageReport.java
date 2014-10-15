package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class UniqueMessageReport extends Report implements MessageListener{
	private static final String messageId = "M10";

	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if(m.getId().equalsIgnoreCase(messageId)){
			if(dropped){
				write("dropped "+m.getId()+" "+where.getAddress()+" "+m.getGlobalNumberOfReplicas());
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
		if(m.getId().equalsIgnoreCase(messageId)){
			if(firstDelivery){
				write("end: "+m.getGlobalNumberOfReplicas());
			}else{
				write("transferred from:"+from.getAddress()+" to:"+to.getAddress()+" "+m.getGlobalNumberOfReplicas());
			}
		}
	}

	@Override
	public void newMessage(Message m) {
		if(m.getId().equalsIgnoreCase(messageId)){
			write("new "+m.getId()+" from:"+m.getFrom()+" to:"+m.getTo()+" "+m.getGlobalNumberOfReplicas());
		}
	}

}
