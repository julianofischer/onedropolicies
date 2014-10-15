package report;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import routing.SprayAndWaitRouter;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class SNWCopiesLeftReport extends Report implements MessageListener{

	private HashMap<Integer,Integer> copiesLeft;
	private int totalDropped;
	
	public SNWCopiesLeftReport(){
		copiesLeft = new HashMap<Integer,Integer>();
		totalDropped=0;
	}
	
	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		if(dropped){
			totalDropped++;
			Integer copies = (Integer)m.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY);
			//if(m.getId().equals("M106")){
			//	System.out.println("Descartando mensagem com "+m.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY)+" cópias");
			//	System.out.println("Nó: "+where.getAddress());
			//}
			if(copiesLeft.containsKey(copies)){
				Integer times = copiesLeft.get(copies);
				times = times + 1;
				copiesLeft.put(copies, times);
			}else{
				copiesLeft.put(copies,new Integer(1));
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
		/*
		if(m.getId().equals("M106") && firstDelivery){
			System.out.println("A mensagem foi entregue.");
			System.out.println("From: "+from.getAddress()+" to: "+to.getAddress());
			System.out.println("Cópias: "+m.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY));
		}else if(m.getId().equals("M106")){
			System.out.println("A mensagem foi transferida.");
			System.out.println("From: "+from.getAddress()+" to: "+to.getAddress());
			System.out.println("Cópias: "+m.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY));
		}*/
	}

	@Override
	public void newMessage(Message m) {
		// TODO Auto-generated method stub
		//if(m.getId().equals("M106")){
		//	System.out.println("A mensagem foi criada.");
		//	System.out.println("Cópias: "+m.getProperty(SprayAndWaitRouter.MSG_COUNT_PROPERTY));
		//}
	}
	
	@Override
	public void done(){
		Set<Integer> keys = copiesLeft.keySet();
		Iterator<Integer> itKeys = keys.iterator();
		while(itKeys.hasNext()){
			Integer k = itKeys.next();
			Integer times = copiesLeft.get(k);
			double percent = (times * 100.0)/totalDropped; 
					
			write(k + " " + times + " " + percent);
		}
		
		super.done();
	}
	
}
