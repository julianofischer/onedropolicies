/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import core.Tuple;

/**
 * Implementation of PRoPHET router as described in 
 * <I>Probabilistic routing in intermittently connected networks</I> by
 * Anders Lindgren et al.
 */
public class ProphetRouter extends ActiveRouter {
	/** delivery predictability initialization constant*/
	public static final double P_INIT = 0.75;
	/** delivery predictability transitivity scaling constant default value */
	public static final double DEFAULT_BETA = 0.25;
	/** delivery predictability aging constant */
	public static final double GAMMA = 0.98;
	
	/** Prophet router's setting namespace ({@value})*/ 
	public static final String PROPHET_NS = "ProphetRouter";
	/**
	 * Number of seconds in time unit -setting id ({@value}).
	 * How many seconds one time unit is when calculating aging of 
	 * delivery predictions. Should be tweaked for the scenario.*/
	public static final String SECONDS_IN_UNIT_S ="secondsInTimeUnit";
	
	/**
	 * Transitivity scaling constant (beta) -setting id ({@value}).
	 * Default value for setting is {@link #DEFAULT_BETA}.
	 */
	public static final String BETA_S = "beta";
	
	/** @julianofischer sendqueue */
	public static final String PROPHET_SEND_QUEUE = "sendQueue";
	public static final int PROPHET_SEND_QUEUE_RR = 1;

	/** the value of nrof seconds in time unit -setting */
	private int secondsInTimeUnit;
	/** value of beta setting */
	private double beta;
	
	/** @julianofischer sendqueue */
	private int sendQueue;

	public int getSendQueue() {
		return sendQueue;
	}

	public void setSendQueue(int sendQueue) {
		this.sendQueue = sendQueue;
	}

	/** delivery predictabilities */
	private Map<DTNHost, Double> preds;
	/** last delivery predictability update (sim)time */
	private double lastAgeUpdate;
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	
	public static final String MOLE_ALFA ="moleAlfa";
	private int mole_alfa;
	public static final String MOLE_COPIES = "moleCopies";
	private int mole_copies;
	public static final String MOLE_MAX_COPIES = "moleMaxCopies";
	private int mole_max_copies;
	
	public ProphetRouter(Settings s) {
		super(s);
		Settings prophetSettings = new Settings(PROPHET_NS);
		secondsInTimeUnit = prophetSettings.getInt(SECONDS_IN_UNIT_S);
		if (prophetSettings.contains(BETA_S)) {
			beta = prophetSettings.getDouble(BETA_S);
		}
		else {
			beta = DEFAULT_BETA;
		}
		
		if(prophetSettings.contains(MOLE_ALFA)){
			mole_alfa = prophetSettings.getInt(MOLE_ALFA);
			System.out.println("Alfa: "+mole_alfa);
		}
		
		if(prophetSettings.contains(MOLE_COPIES)){
			mole_copies = prophetSettings.getInt(MOLE_COPIES);
			System.out.println("Copies: "+mole_copies);
		}
		
		if(prophetSettings.contains(MOLE_MAX_COPIES)){
			mole_max_copies = prophetSettings.getInt(MOLE_MAX_COPIES);
		}
		
		if (prophetSettings.contains(PROPHET_SEND_QUEUE)){
			sendQueue = prophetSettings.getInt(PROPHET_SEND_QUEUE);
			if(sendQueue!=PROPHET_SEND_QUEUE_RR){
				throw new SettingsError("Invalid value for "
						+ s.getFullPropertyName(PROPHET_SEND_QUEUE));
			}else{
				System.out.println("SendQueue RR + GTRMax ativado...");
			}
		}else{
			sendQueue = 0;
		}
		initPreds();
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ProphetRouter(ProphetRouter r) {
		super(r);
		this.secondsInTimeUnit = r.secondsInTimeUnit;
		this.beta = r.beta;
		this.mole_alfa=r.mole_alfa;
		this.mole_copies = r.mole_copies;
		this.sendQueue = r.getSendQueue();
		initPreds();
	}
	
	/**
	 * Initializes predictability hash
	 */
	private void initPreds() {
		this.preds = new HashMap<DTNHost, Double>();
	}

	@Override
	public void changedConnection(Connection con) {
		if (con.isUp()) {
			DTNHost otherHost = con.getOtherNode(getHost());
			updateDeliveryPredFor(otherHost);
			updateTransitivePreds(otherHost);
		}
	}
	
	/**
	 * Updates delivery predictions for a host.
	 * <CODE>P(a,b) = P(a,b)_old + (1 - P(a,b)_old) * P_INIT</CODE>
	 * @param host The host we just met
	 */
	private void updateDeliveryPredFor(DTNHost host) {
		double oldValue = getPredFor(host);
		double newValue = oldValue + (1 - oldValue) * P_INIT;
		preds.put(host, newValue);
	}
	
	/**
	 * Returns the current prediction (P) value for a host or 0 if entry for
	 * the host doesn't exist.
	 * @param host The host to look the P for
	 * @return the current P value
	 */
	public double getPredFor(DTNHost host) {
		ageDeliveryPreds(); // make sure preds are updated before getting
		if (preds.containsKey(host)) {
			return preds.get(host);
		}
		else {
			return 0;
		}
	}
	
	/**
	 * Updates transitive (A->B->C) delivery predictions.
	 * <CODE>P(a,c) = P(a,c)_old + (1 - P(a,c)_old) * P(a,b) * P(b,c) * BETA
	 * </CODE>
	 * @param host The B host who we just met
	 */
	private void updateTransitivePreds(DTNHost host) {
		MessageRouter otherRouter = host.getRouter();
		assert otherRouter instanceof ProphetRouter : "PRoPHET only works " + 
			" with other routers of same type";
		
		double pForHost = getPredFor(host); // P(a,b)
		Map<DTNHost, Double> othersPreds = 
			((ProphetRouter)otherRouter).getDeliveryPreds();
		
		for (Map.Entry<DTNHost, Double> e : othersPreds.entrySet()) {
			if (e.getKey() == getHost()) {
				continue; // don't add yourself
			}
			
			double pOld = getPredFor(e.getKey()); // P(a,c)_old
			double pNew = pOld + ( 1 - pOld) * pForHost * e.getValue() * beta;
			preds.put(e.getKey(), pNew);
		}
	}

	/**
	 * Ages all entries in the delivery predictions.
	 * <CODE>P(a,b) = P(a,b)_old * (GAMMA ^ k)</CODE>, where k is number of
	 * time units that have elapsed since the last time the metric was aged.
	 * @see #SECONDS_IN_UNIT_S
	 */
	private void ageDeliveryPreds() {
		double timeDiff = (SimClock.getTime() - this.lastAgeUpdate) / 
			secondsInTimeUnit;
		
		if (timeDiff == 0) {
			return;
		}
		
		double mult = Math.pow(GAMMA, timeDiff);
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			e.setValue(e.getValue()*mult);
		}
		
		this.lastAgeUpdate = SimClock.getTime();
	}
	
	/**
	 * Returns a map of this router's delivery predictions
	 * @return a map of this router's delivery predictions
	 */
	private Map<DTNHost, Double> getDeliveryPreds() {
		ageDeliveryPreds(); // make sure the aging is done
		return this.preds;
	}
	
	@Override
	public void update() {
		super.update();
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
		}
		
		tryOtherMessages();		
	}
	
	/**
	 * Tries to send all other messages to all connected hosts ordered by
	 * their delivery probability
	 * @return The return value of {@link #tryMessagesForConnected(List)}
	 */
	private Tuple<Message, Connection> tryOtherMessages() {
		List<Tuple<Message, Connection>> messages = 
			new ArrayList<Tuple<Message, Connection>>(); 
	
		Collection<Message> msgCollection = getMessageCollection();
		
		/* for all connected hosts collect all messages that have a higher
		   probability of delivery by the other host */
		for (Connection con : getConnections()) {
			DTNHost other = con.getOtherNode(getHost());
			ProphetRouter othRouter = (ProphetRouter)other.getRouter();
			
			if (othRouter.isTransferring()) {
				continue; // skip hosts that are transferring
			}
			
			for (Message m : msgCollection) {
				if (othRouter.hasMessage(m.getId())) {
					continue; // skip messages that the other one has
				}
				if (othRouter.getPredFor(m.getTo()) > getPredFor(m.getTo())) {
					// the other node has higher probability of delivery
					messages.add(new Tuple<Message, Connection>(m,con));
				}
			}			
		}
		
		if (messages.size() == 0) {
			return null;
		}
		
		// sort the message-connection tuples
		/*@julianofischer
		 * Aqui vou utilizar o parametro ProphetRouter.sendQueue;
		 * */
		if(sendQueue==PROPHET_SEND_QUEUE_RR){ //é round robin
			//System.out.println("Está usando Round Robin ...");
			Collections.sort(messages, new TupleComparatorRR());
		}else{
			Collections.sort(messages, new TupleComparator());
		}
	
		return tryMessagesForConnected(messages);	// try to send messages
	}
	
	/**
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the 
	 * connection (GRTRMax)
	 */
	private class TupleComparator implements Comparator 
		<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ProphetRouter)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ProphetRouter)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());

			// bigger probability should come first
			if (p2-p1 == 0) {
				/* equal probabilities -> let queue mode decide */
				return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
			}
			else if (p2-p1 < 0) {
				return -1;
			}
			else {
				return 1;
			}
		}
	}
	
	/**
	 * @julianofischer
	 * Comparator for Message-Connection-Tuples that orders the tuples by
	 * their delivery probability by the host on the other side of the 
	 * connection (Round Robin + GRTRMax)
	 */
	private class TupleComparatorRR implements Comparator 
		<Tuple<Message, Connection>> {

		public int compare(Tuple<Message, Connection> tuple1,
				Tuple<Message, Connection> tuple2) {
			// delivery probability of tuple1's message with tuple1's connection
			double p1 = ((ProphetRouter)tuple1.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple1.getKey().getTo());
			// -"- tuple2...
			double p2 = ((ProphetRouter)tuple2.getValue().
					getOtherNode(getHost()).getRouter()).getPredFor(
					tuple2.getKey().getTo());
			
			int mofoP1;
			int mofoP2;
			mofoP1 = ((Message)tuple1.getKey()).getGlobalNumberOfReplicas();
			mofoP2 = ((Message)tuple2.getKey()).getGlobalNumberOfReplicas();
			
			if(mofoP2>mofoP1){
				return -1;
			}else if(mofoP2<mofoP1){
				return 1;
			}else{
				// bigger probability should come first
				if (p2-p1 == 0) {
					/* equal probabilities -> let queue mode decide */
					return compareByQueueMode(tuple1.getKey(), tuple2.getKey());
				}
				else if (p2-p1 < 0) {
					return -1;
				}
				else {
					return 1;
				}
			}
		}
	}
	
	@Override
	public RoutingInfo getRoutingInfo() {
		ageDeliveryPreds();
		RoutingInfo top = super.getRoutingInfo();
		RoutingInfo ri = new RoutingInfo(preds.size() + 
				" delivery prediction(s)");
		
		for (Map.Entry<DTNHost, Double> e : preds.entrySet()) {
			DTNHost host = e.getKey();
			Double value = e.getValue();
			
			ri.addMoreInfo(new RoutingInfo(String.format("%s : %.6f", 
					host, value)));
		}
		
		top.addMoreInfo(ri);
		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		ProphetRouter r = new ProphetRouter(this);
		return r;
	}
	
	protected boolean makeRoomForMessage(int size){
		if(this.getDropQueueMode()==ActiveRouter.DROP_MODE_LPMOFO || this.getDropQueueMode()==ActiveRouter.DROP_MODE_LPMOFOV2 || this.getDropQueueMode()==ActiveRouter.DROP_MODE_LEPR){
			
			if (size > this.getBufferSize()) {
				return false; // message too big for the buffer
			}

			int freeBuffer = this.getFreeBufferSize();
			/* delete messages from the buffer until there's enough space */
			boolean excludeMsgBeingSent = true;
			while (freeBuffer < size) {
				Message m = null;
				
				if(this.getDropQueueMode()==ActiveRouter.DROP_MODE_LPMOFO){
					m=this.getLPMOFOMessage(excludeMsgBeingSent);
				}else if(this.getDropQueueMode()==ActiveRouter.DROP_MODE_LPMOFOV2){
					m=this.getLPMOFOV2Message(excludeMsgBeingSent);
				}else if(this.getDropQueueMode()==ActiveRouter.DROP_MODE_LEPR){
					m=this.getLEPRMessage(excludeMsgBeingSent);
				}else if(this.getDropQueueMode()==ActiveRouter.DROP_MODE_DMDF){
					m=this.getDMDFMessage(excludeMsgBeingSent);
				}
				
				//else if(this.getDropQueueMode()==ActiveRouter.DROP_MODE_LPMOFO2_1){
				//	m=this.getLPMOFOV2_1Message(excludeMsgBeingSent);
				//}
				
				if(m==null){
					return false;
				}
				
				deleteMessage(m.getId(), true);
				freeBuffer += m.getSize();
			}
			return true;
			
		}else{
			return super.makeRoomForMessage(size);
		}
	}
	
	protected Message getLPMOFOMessage(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		Message lpmofo = null;
		for (Message m : messages) {

			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			
			if(lpmofo==null){
				lpmofo=m;
			}else{
				double value_lpmofo = calcExpMofo(lpmofo)+this.getPredFor(lpmofo.getTo());
				double value_m = calcExpMofo(m)+this.getPredFor(m.getTo());
				
				if(value_lpmofo>value_m){
					lpmofo = m;
				}
			}
		}
		return lpmofo;
	}
	
	
	private double calcExpMofo(Message m){
		double exp = m.getLocalNumberOfReplicas();
		double moleAlfa = this.mole_alfa;
		//System.out.println("numberGlobalOfReplicas: "+exp);
		//System.out.println("moleAlta: "+moleAlfa);
		exp = exp/moleAlfa;
		//System.out.println("exp/moleAlfa: "+exp);
		exp = Math.exp(-exp);
		//System.out.println("exp: "+exp);
		return exp;
	}
	
	protected Message getLPMOFOV2Message(boolean excludeMsgBeingSent){
		Collection<Message> messages = this.getMessageCollection();
		Message lpmofo = null;
		
		for (Message m : messages) {
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			
			if(lpmofo==null){
				if(m.getGlobalNumberOfReplicas()>mole_copies){
					lpmofo=m;
				}
			}else{
				if(m.getGlobalNumberOfReplicas()>mole_copies){
					if(this.getPredFor(m.getTo())<this.getPredFor(lpmofo.getTo())){
						lpmofo = m;
					}
				}
			}
		}
		
		/* Se não tiver nenhuma mensagem cuja quantidade de cópias seja maior que 
		 * mole_copies então descarta de modo fifo.
		 */
		
		if(lpmofo == null){
			lpmofo = getOldestMessage(excludeMsgBeingSent);
		}
		
		return lpmofo;
	}
	
	protected Message getLEPRMessage(boolean excludeMsgBeingSent){
		Collection<Message> messages = this.getMessageCollection();
		Message lepr = null;
		
		for (Message m : messages) {

			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}

			if (lepr == null) {
				lepr = m;
			} else if (this.getPredFor(lepr.getTo()) > this.getPredFor(m.getTo())) {
				lepr = m;
			}
		}

		return lepr;
	}
	
	protected Message getDMDFMessage(boolean excludeMsgBeingSent){
		Collection<Message> messages = this.getMessageCollection();
		Message dmdf = null;
		
		for (Message m : messages) {
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
	
			if(dmdf==null){
				dmdf = m;
			}else if(dmdf.getHigherDeliveryProb()<m.getHigherDeliveryProb() && m.getHigherDeliveryProb()>this.getPredFor(m.getTo())){
				dmdf = m;
			}
		}
		
		return dmdf;
	}
	
	/*
	protected Message getLPMOFOV2_1Message(boolean excludeMsgBeingSent){
		Collection<Message> messages = this.getMessageCollection();
		Message lpmofo = null;
		
		for (Message m : messages) {
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			
			if(lpmofo==null){
				if(m.getGlobalNumberOfReplicas()>mole_max_copies){
					lpmofo=m;
				}
			}else{
				if(m.getGlobalNumberOfReplicas()>mole_max_copies){
					if(this.getPredFor(m.getTo())<this.getPredFor(lpmofo.getTo())){
						lpmofo = m;
					}
				}
			}
		}
		
		if(lpmofo==null){
			for (Message m : messages) {
				if (excludeMsgBeingSent && isSending(m.getId())) {
					continue; // skip the message(s) that router is sending
				}
			
				if(lpmofo==null){
					if(m.getGlobalNumberOfReplicas()>mole_copies){
						lpmofo=m;
					}
				}else{
					if(m.getGlobalNumberOfReplicas()>mole_copies){
						if(this.getPredFor(m.getTo())<this.getPredFor(lpmofo.getTo())){
							lpmofo = m;
						}
					}
				}
			}
		}
		
		 //Se não tiver nenhuma mensagem cuja quantidade de cópias seja maior que 
		 // mole_copies então descarta de modo fifo.
		 //
		
		if(lpmofo == null){
			lpmofo = getOldestMessage(excludeMsgBeingSent);
		}
		
		return lpmofo;
	} */
}
