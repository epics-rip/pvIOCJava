/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.monitor;

import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.factory.StatusFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.misc.BitSetUtil;
import org.epics.pvdata.misc.BitSetUtilFactory;
import org.epics.pvdata.misc.LinkedList;
import org.epics.pvdata.misc.LinkedListCreate;
import org.epics.pvdata.misc.LinkedListNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.misc.Timer;
import org.epics.pvdata.misc.TimerFactory;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorAlgorithm;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.monitor.MonitorQueue;
import org.epics.pvdata.monitor.MonitorQueueFactory;
import org.epics.pvdata.monitor.MonitorRequester;
import org.epics.pvdata.property.PVTimeStamp;
import org.epics.pvdata.property.PVTimeStampFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.pvCopy.PVCopy;
import org.epics.pvioc.pvCopy.PVCopyFactory;
import org.epics.pvioc.pvCopy.PVCopyMonitor;
import org.epics.pvioc.pvCopy.PVCopyMonitorRequester;


/**
 * @author mrk
 *
 */
public class MonitorFactory {
	
	/**
	 * Create a monitor.
	 * @param pvRecord The record to monitor.
	 * @param monitorRequester The requester.
	 * @param pvRequest Then request structure defining the monitor options.
	 * @return The Monitor interface.
	 */
	public static Monitor create(PVRecord pvRecord,MonitorRequester monitorRequester,PVStructure pvRequest)
	{
		MonitorImpl monitor = new MonitorImpl(pvRecord,monitorRequester);
		if(!monitor.init(pvRequest)) {
			monitorRequester.monitorConnect(failedToCreateMonitorStatus, null, null);
			return null;
		}
		return monitor;
	}
	
	public static void registerMonitorAlgorithmCreater(MonitorAlgorithmCreate monitorAlgorithmCreate) {
		synchronized(monitorAlgorithmCreateList) {
			if(monitorAlgorithmCreateList.contains(monitorAlgorithmCreate)) {
				throw new IllegalStateException("already on list");
			}
			LinkedListNode<MonitorAlgorithmCreate> node = monitorAlgorithmCreateListCreate.createNode(monitorAlgorithmCreate);
			monitorAlgorithmCreateList.addTail(node);
        }
	}
	private static final LinkedListCreate<MonitorAlgorithmCreate> monitorAlgorithmCreateListCreate = new LinkedListCreate<MonitorAlgorithmCreate>();
	private static final LinkedListCreate<MonitorFieldNode> MonitorFieldNodeListCreate= new LinkedListCreate<MonitorFieldNode>();
	private static final StatusCreate statusCreate = StatusFactory.getStatusCreate();
    private static final Status okStatus = statusCreate.getStatusOK();
    private static final Status failedToCreateMonitorStatus = statusCreate.createStatus(StatusType.FATAL, "failed to create monitor", null);
    private static final FieldCreate fieldCreate = FieldFactory.getFieldCreate();
    private static final PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
	private static final LinkedList<MonitorAlgorithmCreate> monitorAlgorithmCreateList = monitorAlgorithmCreateListCreate.create();
	private static final BitSetUtil bitSetUtil = BitSetUtilFactory.getCompressBitSet();
	private static final Convert convert = ConvertFactory.getConvert();
	private static final Timer timer = TimerFactory.create("periodicMonitor", ThreadPriority.high);
	private static PVStructure pvTimeStampRequest;
	private static MonitorAlgorithmCreate algorithmOnChangeCreate;
	private static MonitorAlgorithmCreate algorithmDeadband;
	
	static {
		AlgorithmOnChangeFactory.register();
		AlgorithmDeadbandFactory.register();
		String[] fieldNames = new String[2];
		Field[] fields = new Field[2];
		fieldNames[0] = "algorithm";
		fieldNames[1] = "causeMonitor";
		fields[0] = fieldCreate.createScalar(ScalarType.pvString);
		fields[1] = fieldCreate.createScalar(ScalarType.pvBoolean);
		Structure structure = fieldCreate.createStructure(fieldNames, fields);
		pvTimeStampRequest = pvDataCreate.createPVStructure(structure);
		PVString pvAlgorithm = pvTimeStampRequest.getStringField(fieldNames[0]);
		PVBoolean pvCauseMonitor = pvTimeStampRequest.getBooleanField(fieldNames[1]);
		pvAlgorithm.put("onChange");
		pvCauseMonitor.put(false);
		LinkedListNode<MonitorAlgorithmCreate> node = monitorAlgorithmCreateList.getHead();
		while(node!=null) {
			MonitorAlgorithmCreate algorithmCreate = node.getObject();
			if(algorithmCreate.getAlgorithmName().equals("onChange")) {
				algorithmOnChangeCreate = algorithmCreate;
			}
			if(algorithmCreate.getAlgorithmName().equals("deadband")) {
				algorithmDeadband = algorithmCreate;
			}
			node = monitorAlgorithmCreateList.getNext(node);
		}
	}
	
	private static class MonitorFieldNode {
		MonitorAlgorithm monitorAlgorithm;
		int bitOffset; // in pvCopy
		
		MonitorFieldNode(MonitorAlgorithm monitorAlgorithm,int bitOffset) {
			this.monitorAlgorithm = monitorAlgorithm;
			this.bitOffset = bitOffset;
		}
	}
	
	private interface QueueImpl {
        public MonitorElement init(MonitorImpl monitorImpl,int queueSize);
        public Status start();
        public void stop();
    	public boolean dataChanged();
    	public MonitorElement poll();
    	public void release(MonitorElement monitorElement);
    }
	
	
	
	private static class MonitorImpl implements Monitor,PVCopyMonitorRequester {
		private final PVRecord pvRecord;
		private final MonitorRequester monitorRequester;
		
		private boolean isPeriodic = false;
		private double periodicRate = 1.0;
		private PVCopy pvCopy = null;
		private QueueImpl queueImpl = null;
		private PVCopyMonitor pvCopyMonitor;
		private final LinkedList<MonitorFieldNode> monitorFieldList = MonitorFieldNodeListCreate.create();
		
        private volatile boolean firstMonitor = false;
        private volatile boolean gotMonitor = false;
        private BitSet changedBitSet = null;
        private BitSet overrunBitSet = null;
        
        private BitSet notMonitoredBitSet = null;
        
		private MonitorImpl(PVRecord pvRecord,MonitorRequester monitorRequester) {
			this.pvRecord = pvRecord;
			this.monitorRequester = monitorRequester;
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#poll()
		 */
		@Override
		public MonitorElement poll() {
			return queueImpl.poll();
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#release(org.epics.pvdata.monitor.MonitorElement)
		 */
		@Override
		public void release(MonitorElement currentElement) {
			queueImpl.release(currentElement);
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#start()
		 */
		@Override
		public Status start() {
			firstMonitor = true;
			gotMonitor = false;
			Status status = queueImpl.start();
			if(!status.isSuccess()) return status;
			changedBitSet.clear();
    		overrunBitSet.clear();
			pvCopyMonitor.startMonitoring(changedBitSet, overrunBitSet);
			return status;
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#stop()
		 */
		@Override
		public Status stop() {
			pvCopyMonitor.stopMonitoring();
			queueImpl.stop();
	        return okStatus;
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.misc.Destroyable#destroy()
		 */
		@Override
		public void destroy() {
			stop();
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.pvCopy.PVCopyMonitorRequester#dataChanged()
		 */
		@Override
		public void dataChanged() {
			if(firstMonitor) {
				queueImpl.dataChanged();
				firstMonitor = false;
				monitorRequester.monitorEvent(this);
				gotMonitor = false;
				return;
			}
			if(!gotMonitor) {
				LinkedListNode<MonitorFieldNode> listNode  = monitorFieldList.getHead();
				while(listNode!=null) {
					MonitorFieldNode node = listNode.getObject();
					boolean result = node.monitorAlgorithm.causeMonitor();
					if(result) gotMonitor = true;
					listNode = monitorFieldList.getNext(listNode);
				}
			}
			if(!gotMonitor) {
				int nextBit = notMonitoredBitSet.nextSetBit(0);
				while(nextBit>=0) {
					if(changedBitSet.get(nextBit)) {
						gotMonitor = true;
						break;
					}
					nextBit = notMonitoredBitSet.nextSetBit(nextBit+1);
				}
			}
			if(!gotMonitor) return;
			if(queueImpl.dataChanged()) {
				monitorRequester.monitorEvent(this);
				LinkedListNode<MonitorFieldNode> listNode  = monitorFieldList.getHead();
				while(listNode!=null) {
					MonitorFieldNode node = listNode.getObject();
				    node.monitorAlgorithm.monitorIssued();
					listNode = monitorFieldList.getNext(listNode);
				}
				gotMonitor = false;
			}
		}	
		/* (non-Javadoc)
		 * @see org.epics.pvdata.pvCopy.PVCopyMonitorRequester#unlisten()
		 */
		@Override
		public void unlisten() {
			monitorRequester.unlisten(this);
		}


		private boolean init(PVStructure pvRequest) {
		    PVField pvField = null;
		    PVStructure pvOptions = null;
			//Marty onPut changeTimeStamp
			int queueSize = 2;
			pvField = pvRequest.getSubField("record._options");
			if(pvField!=null) {
			    pvOptions = (PVStructure)pvField;
			    pvField = pvOptions.getSubField("queueSize");
			}
			if(pvField!=null && (pvField instanceof PVString)) {
				PVString pvString = (PVString)pvField;
				String value = pvString.get();
				try {
					queueSize = Integer.parseInt(value);
				} catch (NumberFormatException e) {
					monitorRequester.message("queueSize " + e.getMessage(), MessageType.error);
					return false;
				}
			}
			if(queueSize<1) {
				monitorRequester.message("queueSize must be >= 1", MessageType.error);
				return false;
			}
			pvField =  null;
			if(pvOptions!=null) pvField = pvOptions.getSubField("periodicRate");
			if(pvField!=null && (pvField instanceof PVString)) {
				PVString pvString = (PVString)pvField;
				String value = pvString.get();
				try {
					periodicRate = Double.parseDouble(value);
				} catch (NumberFormatException e) {
					monitorRequester.message("periodicRate " + e.getMessage(), MessageType.error);
					return false;
				}
				isPeriodic = true;
			}
			pvField = pvRequest.getSubField("field");
			if(pvField==null) {
				pvCopy = PVCopyFactory.create(pvRecord, pvRequest, "");
				if(pvCopy==null) {
					monitorRequester.message("illegal pvRequest", MessageType.error);
					return false;
				}
			} else {
				if(!(pvField instanceof PVStructure)) {
					monitorRequester.message("illegal pvRequest.field", MessageType.error);
					return false;
				}
				pvCopy = PVCopyFactory.create(pvRecord, pvRequest, "field");
				if(pvCopy==null) {
					monitorRequester.message("illegal pvRequest", MessageType.error);
					return false;
				}
			}
			pvCopyMonitor = pvCopy.createPVCopyMonitor(this);
			MonitorElement monitorElement = null;
			if(isPeriodic) {
				queueImpl = new PeriodicNoQueue();
			} else if(queueSize>1) {
				queueImpl = new Queue();
			} else {
				queueImpl = new NoQueue();
			}
			monitorElement = queueImpl.init(this,queueSize);
			notMonitoredBitSet = (BitSet)monitorElement.getChangedBitSet().clone();
			notMonitoredBitSet.clear();
			boolean result = initField(monitorElement.getPVStructure(),monitorElement.getPVStructure());
			if(result) {
				initTimeStamp(monitorElement);
				initNumericFields(monitorElement.getPVStructure());
				notMonitoredBitSet.flip(0, notMonitoredBitSet.size());
				monitorRequester.monitorConnect(okStatus,this, pvCopy.getStructure());
			}
			return result;
		}
		
		private boolean initField(PVStructure pvCopyTop,PVStructure pvCopyField) {
		    PVField[] pvFields = pvCopyField.getPVFields();
		    for(int i=0; i<pvFields.length; i++) {
		        PVField pvField = pvFields[i];
		        int offset = pvField.getFieldOffset();
		        PVStructure pvOptions = pvCopy.getOptions(pvCopyTop, offset);
		        if(pvOptions!=null && pvOptions.getSubField("algorithm")!=null) {
		            PVRecordField pvRecordField = pvCopy.getRecordPVField(offset);
		            boolean result = initMonitorField(
		                    pvOptions,pvCopyField,pvRecordField);
		            if(!result) return false;
		        }
		        if(pvField.getField().getType()==Type.structure) {
		            boolean result = initField(pvCopyTop,(PVStructure)pvField);
		            if(!result) return false;
		        }
		    }
		    return true;
		}
		
		private boolean initMonitorField(
				PVStructure pvOptions,PVField pvCopyField,
				PVRecordField pvRecordField)
		{
			PVString pvAlgorithm = pvOptions.getStringField("algorithm");
			if(pvAlgorithm==null) return false;
			String algorithm = pvAlgorithm.get();
			if(algorithm.equals("onPut")) return true;
			MonitorAlgorithmCreate monitorAlgorithmCreate = null;
			LinkedListNode<MonitorAlgorithmCreate> listNode = monitorAlgorithmCreateList.getHead();
			while(listNode!=null) {
				monitorAlgorithmCreate = listNode.getObject();
				if(monitorAlgorithmCreate.getAlgorithmName().equals(algorithm)) break;
				listNode = monitorAlgorithmCreateList.getNext(listNode);
			}
			if(monitorAlgorithmCreate==null) {
				monitorRequester.message("algorithm not registered", MessageType.error);
				return false;
			}
			MonitorAlgorithm monitorAlgorithm = monitorAlgorithmCreate.create(pvRecord,monitorRequester,pvRecordField,pvOptions);
			if(monitorAlgorithm==null) return false;
			int bitOffset = pvCopyField.getFieldOffset();
			int numBits = pvCopyField.getNumberFields();
			notMonitoredBitSet.set(bitOffset, bitOffset+numBits);
			MonitorFieldNode node = new MonitorFieldNode(monitorAlgorithm,bitOffset);
			node.monitorAlgorithm = monitorAlgorithm;
			node.bitOffset = bitOffset;
			LinkedListNode<MonitorFieldNode> listNode1 = MonitorFieldNodeListCreate.createNode(node);
			monitorFieldList.addTail(listNode1);
			return true;
		}
		
		private void initTimeStamp(MonitorElement monitorElement) {
			PVField pvField = pvRecord.getPVRecordStructure().getPVStructure().getSubField("timeStamp");
			if(pvField==null) return;
			int bitOffset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvField));
			if(bitOffset<0) return;
			LinkedListNode<MonitorFieldNode> listNode = monitorFieldList.getHead();
			while(listNode!=null) {
				MonitorFieldNode monitorFieldNode = listNode.getObject();
				if(monitorFieldNode.bitOffset==bitOffset) return;
				listNode = monitorFieldList.getNext(listNode);
			}
			PVRecordField pvRecordField = pvRecord.findPVRecordField(pvField);
			MonitorAlgorithm monitorAlgorithm = algorithmOnChangeCreate.create(pvRecord,monitorRequester,pvRecordField,pvTimeStampRequest);
			MonitorFieldNode node = new MonitorFieldNode(monitorAlgorithm,bitOffset);
			listNode = MonitorFieldNodeListCreate.createNode(node);
			monitorFieldList.addTail(listNode);
			PVStructure pvStructure =  monitorElement.getPVStructure();
			PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
			PVField pvCopyField = null;
			if(pvTimeStamp.attach(pvStructure)) {
			    pvCopyField= pvStructure;
			} else {
			    pvCopyField = monitorElement.getPVStructure().getSubField("timeStamp");
			}
			if(pvCopyField==null) {
			    System.err.printf("record %s MonitorFactory.initTimeStamp failed%n",pvRecordField.getFullName());
			    return;
			}
			int numBits = pvCopyField.getNumberFields();
			notMonitoredBitSet.set(bitOffset, bitOffset+numBits);
		}
		
		private void initNumericFields(PVStructure pvStructure) {
			PVField[] pvFields = pvStructure.getPVFields();
			outer:
			for(int i=0; i<pvFields.length; i++) {
				PVField pvField = pvFields[i];
				Field field = pvField.getField();
				Type type = field.getType();
				if(type==Type.structure) {
					initNumericFields((PVStructure)pvField);
				} else if(type==Type.scalar) {
					Scalar scalar = (Scalar)field;
					if(scalar.getScalarType().isNumeric()) {
						int bitOffset = pvField.getFieldOffset();
						LinkedListNode<MonitorFieldNode> listNode = monitorFieldList.getHead();
						while(listNode!=null) {
							MonitorFieldNode monitorFieldNode = listNode.getObject();
							if(monitorFieldNode.bitOffset==bitOffset) continue outer; // already monitored
							listNode = monitorFieldList.getNext(listNode);
						}
						PVRecordField pvRecordField = pvCopy.getRecordPVField(bitOffset);
						MonitorAlgorithm monitorAlgorithm = algorithmDeadband.create(pvRecord,monitorRequester,pvRecordField,null);
						if(monitorAlgorithm!=null) {
							int numBits = pvField.getNumberFields();
							notMonitoredBitSet.set(bitOffset, bitOffset+numBits);
						    MonitorFieldNode node = new MonitorFieldNode(monitorAlgorithm,bitOffset);
						    listNode = MonitorFieldNodeListCreate.createNode(node);
							monitorFieldList.addTail(listNode);
						}
					}
				}
			}
		}
		
		private class NoQueue implements QueueImpl {
			private PVStructure pvCopyStructure = null;
			private MonitorElement monitorElement = null;
			private volatile boolean gotMonitor = false;
			private volatile boolean wasReleased = true;
	        private BitSet noQueueChangedBitSet = null;
	        private BitSet noQueueOverrunBitSet = null;
			
			@Override
			public MonitorElement init(MonitorImpl monitorImpl,int queueSize) {
				monitorElement = MonitorQueueFactory.createMonitorElement(pvCopy.createPVStructure());
				pvCopyStructure = monitorElement.getPVStructure();
	        	changedBitSet = monitorElement.getChangedBitSet();
	        	overrunBitSet = monitorElement.getOverrunBitSet();
	        	noQueueChangedBitSet = (BitSet)monitorElement.getChangedBitSet().clone();
	        	noQueueOverrunBitSet = (BitSet)monitorElement.getChangedBitSet().clone();
	        	return monitorElement;
			}
			@Override
			public Status start() {
				synchronized(monitorElement) {
		    		gotMonitor = true;
		    		wasReleased = true;
		    		noQueueChangedBitSet.clear();
		    		noQueueOverrunBitSet.clear();
	        	}
	            return okStatus;
			}
			@Override
			public void stop() {}
			@Override
			public boolean dataChanged() {
				synchronized(monitorElement) {
					noQueueChangedBitSet.or(changedBitSet);
					noQueueOverrunBitSet.or(overrunBitSet);
					gotMonitor = true;
					return wasReleased ? true : false;
				}
			}
			@Override
			public MonitorElement poll() {
				synchronized(monitorElement) {
					if(!gotMonitor) return null;
					changedBitSet.or(noQueueChangedBitSet);
					overrunBitSet.or(noQueueOverrunBitSet);
					pvCopy.updateCopyFromBitSet(pvCopyStructure, changedBitSet, true);
					bitSetUtil.compress(changedBitSet, pvCopyStructure);
		            bitSetUtil.compress(overrunBitSet, pvCopyStructure);
					noQueueChangedBitSet.clear();
					noQueueOverrunBitSet.clear();
					return monitorElement;
				}
			}
			@Override
			public void release(MonitorElement monitorElement) {
				synchronized(monitorElement) {
	                gotMonitor = false;
	                wasReleased = true;
	                changedBitSet.clear();
	                overrunBitSet.clear();
	            }
			}
		}
		
		private class Queue implements QueueImpl {
			private MonitorQueue monitorQueue = null;
			private MonitorElement monitorElement = null;
			private volatile boolean queueIsFull = false;
			
			@Override
			public MonitorElement init(MonitorImpl monitorImpl,int queueSize) {
				MonitorElement[] elements = new MonitorElement[queueSize];
				for(int i=0; i<elements.length;i++) elements[i] = MonitorQueueFactory.createMonitorElement(pvCopy.createPVStructure());
				monitorQueue = MonitorQueueFactory.create(elements);
				monitorElement = monitorQueue.getFree();
				return monitorElement;
			}
			@Override
			public Status start() {
				firstMonitor = true;
	    		monitorQueue.clear();
	    		monitorElement = monitorQueue.getFree();
	    		changedBitSet = monitorElement.getChangedBitSet();
	    		overrunBitSet = monitorElement.getOverrunBitSet();
	            return okStatus;
			}
			@Override
			public void stop() {}
			@Override
			public boolean dataChanged() {
				PVStructure pvStructure = monitorElement.getPVStructure();
				pvCopy.updateCopyFromBitSet(pvStructure, changedBitSet, false);
				synchronized(monitorQueue) {
					MonitorElement newElement = monitorQueue.getFree();
					if(newElement==null) {
						queueIsFull = true;
						return true;
					}
					bitSetUtil.compress(changedBitSet, pvStructure);
					bitSetUtil.compress(overrunBitSet, pvStructure);
					convert.copy(pvStructure, newElement.getPVStructure());
					changedBitSet = newElement.getChangedBitSet();
					overrunBitSet = newElement.getOverrunBitSet();
					changedBitSet.clear();
					overrunBitSet.clear();
					pvCopyMonitor.switchBitSets(changedBitSet, overrunBitSet, false);
					monitorQueue.setUsed(monitorElement);
					monitorElement = newElement;
				}
				return true;
			}
			@Override
			public MonitorElement poll() {
				synchronized(monitorQueue) {
					return monitorQueue.getUsed();
				}
			}
			@Override
			public void release(MonitorElement currentElement) {
				synchronized(monitorQueue) {
					monitorQueue.releaseUsed(currentElement);
					currentElement.getOverrunBitSet().clear();
					currentElement.getChangedBitSet().clear();
					if(!queueIsFull) return;
					queueIsFull = false;
					PVStructure pvStructure = monitorElement.getPVStructure();
					MonitorElement newElement = monitorQueue.getFree();
					bitSetUtil.compress(changedBitSet, pvStructure);
					bitSetUtil.compress(overrunBitSet, pvStructure);
					convert.copy(pvStructure, newElement.getPVStructure());
					changedBitSet = newElement.getChangedBitSet();
					overrunBitSet = newElement.getOverrunBitSet();
					changedBitSet.clear();
					overrunBitSet.clear();
					pvCopyMonitor.switchBitSets(changedBitSet, overrunBitSet, true);
					monitorQueue.setUsed(monitorElement);
					monitorElement = newElement;
				}
			}
		}
		
		private class PeriodicNoQueue implements QueueImpl,Timer.TimerCallback {
			private MonitorImpl monitorImpl = null;
			private PVStructure pvCopyStructure = null;
			private MonitorElement monitorElement = null;
			private volatile boolean gotMonitor = false;
			private volatile boolean wasReleased = true;
			private volatile boolean timerExpired = false;
	        private BitSet noQueueChangedBitSet = null;
	        private BitSet noQueueOverrunBitSet = null;
	        private Timer.TimerNode timerNode = TimerFactory.createNode(this);
			
			@Override
			public MonitorElement init(MonitorImpl monitorImpl,int queueSize) {
				this.monitorImpl = monitorImpl;
				monitorElement = MonitorQueueFactory.createMonitorElement(pvCopy.createPVStructure());
				pvCopyStructure = monitorElement.getPVStructure();
	        	changedBitSet = monitorElement.getChangedBitSet();
	        	overrunBitSet = monitorElement.getOverrunBitSet();
	        	noQueueChangedBitSet = (BitSet)monitorElement.getChangedBitSet().clone();
	        	noQueueOverrunBitSet = (BitSet)monitorElement.getChangedBitSet().clone();
	        	return monitorElement;
			}
			@Override
			public Status start() {
				synchronized(monitorElement) {
		    		gotMonitor = true;
		    		wasReleased = true;
		    		noQueueChangedBitSet.clear();
		    		noQueueOverrunBitSet.clear();
	        	}
				timer.schedulePeriodic(timerNode, periodicRate, periodicRate);
	            return okStatus;
			}
			@Override
			public void stop() {
				timerNode.cancel();
			}
			@Override
			public boolean dataChanged() {
				synchronized(monitorElement) {
					if(!timerExpired) return false;
					timerExpired = false;
					if(changedBitSet.isEmpty()) return false;

					noQueueChangedBitSet.or(changedBitSet);
					noQueueOverrunBitSet.or(overrunBitSet);
					gotMonitor = true;
					return wasReleased ? true : false;
				}
			}
			@Override
			public MonitorElement poll() {
				synchronized(monitorElement) {
					if(!gotMonitor) return null;
					changedBitSet.or(noQueueChangedBitSet);
					overrunBitSet.or(noQueueOverrunBitSet);
					pvCopy.updateCopyFromBitSet(pvCopyStructure, changedBitSet, true);
					bitSetUtil.compress(changedBitSet, pvCopyStructure);
		            bitSetUtil.compress(overrunBitSet, pvCopyStructure);
					noQueueChangedBitSet.clear();
					noQueueOverrunBitSet.clear();
					return monitorElement;
				}
			}
			@Override
			public void release(MonitorElement monitorElement) {
				synchronized(monitorElement) {
	                gotMonitor = false;
	                wasReleased = true;
	                changedBitSet.clear();
	                overrunBitSet.clear();
	            }
			}
			/* (non-Javadoc)
			 * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
			 */
			@Override
			public void callback() {
				synchronized(monitorElement) {
					timerExpired = true;
				}
				monitorImpl.dataChanged();
			}
			/* (non-Javadoc)
			 * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
			 */
			@Override
			public void timerStopped() {
				monitorRequester.message("periodicTimer stopped", MessageType.error);
			}
		}
	}
}
