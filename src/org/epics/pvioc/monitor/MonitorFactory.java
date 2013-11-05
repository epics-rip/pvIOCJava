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
        public void init(MonitorImpl monitorImpl,int queueSize);
        public Status start();
        public void stop();
    	public boolean dataChanged();
    	public BitSet getChangedBitSet();
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
		
        private boolean firstMonitor = false;
//        private BitSet changedBitSet = null;
//        private BitSet overrunBitSet = null;
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
		    synchronized(queueImpl) {
		        return queueImpl.poll();
		    }
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#release(org.epics.pvdata.monitor.MonitorElement)
		 */
		@Override
		public void release(MonitorElement currentElement) {
		    synchronized(queueImpl) {
		        queueImpl.release(currentElement);
		    }
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#start()
		 */
		@Override
		public Status start() {
		    synchronized(queueImpl) {
		        firstMonitor = true;
		        return queueImpl.start();
		    }
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#stop()
		 */
		@Override
		public Status stop() {
		    pvCopyMonitor.stopMonitoring();
		    synchronized(queueImpl) {
		        queueImpl.stop();
		        return okStatus;
		    }
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
		        synchronized(queueImpl) {
		            queueImpl.dataChanged();
		            firstMonitor = false;
		        }
		        monitorRequester.monitorEvent(this);
		        return;
		    }
		    boolean gotMonitor = false;
		    if(!gotMonitor) {
		        LinkedListNode<MonitorFieldNode> listNode  = monitorFieldList.getHead();
		        while(listNode!=null) {
		            MonitorFieldNode node = listNode.getObject();
		            boolean result = node.monitorAlgorithm.causeMonitor();
		            if(result) {
		                gotMonitor = true;
		                break;
		            }
		            listNode = monitorFieldList.getNext(listNode);
		        }
		    }
		    if(!gotMonitor) {
		        int nextBit = notMonitoredBitSet.nextSetBit(0);
		        while(nextBit>=0) {
		            if(queueImpl.getChangedBitSet().get(nextBit)) {
		                gotMonitor = true;
		                break;
		            }
		            nextBit = notMonitoredBitSet.nextSetBit(nextBit+1);
		        }
		    }
		    if(!gotMonitor) return;
		    synchronized(queueImpl) {
		        if(queueImpl.dataChanged()) monitorRequester.monitorEvent(this);
		        
		    }
		    LinkedListNode<MonitorFieldNode> listNode  = monitorFieldList.getHead();
		    while(listNode!=null) {
		        MonitorFieldNode node = listNode.getObject();
		        node.monitorAlgorithm.monitorIssued();
		        listNode = monitorFieldList.getNext(listNode);
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
			if(isPeriodic) {
				queueImpl = new PeriodicNoQueue();
			} else if(queueSize>1) {
				queueImpl = new Queue();
			} else {
				queueImpl = new NoQueue();
			}
			queueImpl.init(this,queueSize);
			
			queueImpl.dataChanged();
			PVStructure pvStructure = pvCopy.createPVStructure();
			notMonitoredBitSet = new BitSet(pvStructure.getNumberFields());
			notMonitoredBitSet.clear();
			boolean result = initField(pvStructure,pvStructure);
			if(result) {
				initTimeStamp(pvStructure);
				initNumericFields(pvStructure);
				notMonitoredBitSet.flip(0, notMonitoredBitSet.size());
				monitorRequester.monitorConnect(okStatus,this, pvCopy.getStructure());
			}
			pvStructure = null;
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
		
		private void initTimeStamp(PVStructure pvStructure ) {
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
			if(monitorAlgorithm==null) return;
			PVTimeStamp pvTimeStamp = PVTimeStampFactory.create();
            PVField pvCopyField = null;
            if(pvTimeStamp.attach(pvStructure)) {
                pvCopyField= pvStructure;
            } else {
                pvCopyField = pvStructure.getSubField("timeStamp");
            }
            if(pvCopyField==null) return;
			MonitorFieldNode node = new MonitorFieldNode(monitorAlgorithm,bitOffset);
			listNode = MonitorFieldNodeListCreate.createNode(node);
			monitorFieldList.addTail(listNode);
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
		
		private static class NoQueue implements QueueImpl {
		    private MonitorImpl monitorLocal = null;
		    private MonitorElement monitorElement = null;
		    private BitSet changedBitSet = null;
		    private BitSet overrunBitSet = null;
		    private boolean gotMonitor = false;

		    @Override
		    public void init(MonitorImpl monitorImpl,int queueSize) {
		        monitorLocal = monitorImpl;
		        monitorElement = MonitorQueueFactory.createMonitorElement(monitorLocal.pvCopy.createPVStructure());
		        PVStructure pvStructure = monitorElement.getPVStructure();
		        changedBitSet = new BitSet(pvStructure.getNumberFields());
		        overrunBitSet = new BitSet(pvStructure.getNumberFields());
		    }
		    @Override
		    public Status start() {
		        gotMonitor = true;
		        changedBitSet.clear();
		        overrunBitSet.clear();
		        monitorLocal.pvCopyMonitor.startMonitoring(changedBitSet, overrunBitSet);
		        return okStatus;
		    }
		    @Override
		    public void stop() {}
		    /* (non-Javadoc)
		     * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#dataChanged()
		     */
		    public boolean dataChanged() {
		        gotMonitor = true;
		        return true;
		        
		    }
		    /* (non-Javadoc)
             * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#getChangedBitSet()
             */
            @Override
            public BitSet getChangedBitSet() {
                return changedBitSet;
            }
            /* (non-Javadoc)
		     * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#poll()
		     */
		    public MonitorElement poll() {
		        if(!gotMonitor) return null;
		        PVStructure pvStructure = monitorElement.getPVStructure();
		        monitorLocal.pvCopy.updateCopyFromBitSet(pvStructure, changedBitSet, true);
		        bitSetUtil.compress(changedBitSet, pvStructure);
		        bitSetUtil.compress(overrunBitSet, pvStructure);
		        BitSet ebs = monitorElement.getChangedBitSet();
		        ebs.clear();
		        ebs.or(changedBitSet);
		        ebs = monitorElement.getOverrunBitSet();
		        ebs.clear();
		        ebs.or(overrunBitSet);
		        changedBitSet.clear();
		        overrunBitSet.clear();
		        return monitorElement;
		    }
		    @Override
		    public void release(MonitorElement monitorElement) {
		        gotMonitor = false;
		    }
		}
		
		private static class Queue implements QueueImpl {
		    private MonitorImpl monitorLocal = null;
		    private MonitorQueue monitorQueue = null;
		    private BitSet changedBitSet = null;
		    private BitSet overrunBitSet = null;
		    private MonitorElement latestMonitorElement = null;
		    private boolean queueIsFull = false;

		    /* (non-Javadoc)
		     * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#init(org.epics.pvioc.monitor.MonitorFactory.MonitorImpl, int)
		     */
		    @Override
		    public void init(MonitorImpl monitorImpl,int queueSize) {
		        monitorLocal = monitorImpl;
		        MonitorElement[] elements = new MonitorElement[queueSize];
		        for(int i=0; i<elements.length;i++) {   
		            elements[i] = MonitorQueueFactory.createMonitorElement(monitorImpl.pvCopy.createPVStructure());
		        }
		        monitorQueue = MonitorQueueFactory.create(elements);
		        PVStructure pvStructure = elements[0].getPVStructure();
		        changedBitSet = new BitSet(pvStructure.getNumberFields());
		        overrunBitSet = new BitSet(pvStructure.getNumberFields());
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#start()
		     */
		    @Override
		    public Status start() {
		        monitorQueue.clear();
		        changedBitSet.clear();
		        overrunBitSet.clear();
		        monitorLocal.pvCopyMonitor.startMonitoring(changedBitSet,overrunBitSet);
		        return okStatus;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#stop()
		     */
		    @Override
		    public void stop() {}
		    /* (non-Javadoc)
		     * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#dataChanged()
		     */
		    public boolean dataChanged() {
		        if(queueIsFull) {
		            MonitorElement monitorElement = latestMonitorElement;
		            PVStructure pvStructure = monitorElement.getPVStructure();
	                monitorLocal.pvCopy.updateCopyFromBitSet(pvStructure, changedBitSet, false);
	                BitSet ebs = monitorElement.getChangedBitSet();
	                ebs.or(changedBitSet);
	                ebs = monitorElement.getOverrunBitSet();
	                ebs.or(changedBitSet);
	                changedBitSet.clear();
	                overrunBitSet.clear();
	                return false;
		        }
		        MonitorElement monitorElement = monitorQueue.getFree();
		        if(monitorQueue.getNumberFree()==0){
		            queueIsFull = true;
		            latestMonitorElement = monitorElement;
		        }
                PVStructure pvStructure = monitorElement.getPVStructure();
                monitorLocal.pvCopy.updateCopyFromBitSet(pvStructure, changedBitSet, false);
                bitSetUtil.compress(changedBitSet, pvStructure);
                bitSetUtil.compress(overrunBitSet, pvStructure);
                BitSet ebs = monitorElement.getChangedBitSet();
                ebs.clear();
                ebs.or(changedBitSet);
                ebs = monitorElement.getOverrunBitSet();
                ebs.clear();
                ebs.or(overrunBitSet);
                changedBitSet.clear();
                overrunBitSet.clear();
                monitorQueue.setUsed(monitorElement);
                return true;
		    }
		    /* (non-Javadoc)
             * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#getChangedBitSet()
             */
            @Override
            public BitSet getChangedBitSet() {
                return changedBitSet;
            }
		    
		    @Override
		    public MonitorElement poll() {
		        return monitorQueue.getUsed();
		    }
		    @Override
		    public void release(MonitorElement currentElement) {
		        if(queueIsFull) {
		            MonitorElement monitorElement = latestMonitorElement;
	                PVStructure pvStructure = monitorElement.getPVStructure();
	                BitSet ebs = monitorElement.getChangedBitSet();
	                bitSetUtil.compress(ebs, pvStructure);
	                ebs = monitorElement.getOverrunBitSet();
	                bitSetUtil.compress(ebs, pvStructure);
		            queueIsFull = false;
		            latestMonitorElement = null;
		        }
		        monitorQueue.releaseUsed(currentElement);
		    }
		}
		
		private static class PeriodicNoQueue implements QueueImpl,Timer.TimerCallback {
		    private MonitorImpl monitorLocal = null;
		    private MonitorElement monitorElement = null;
		    private boolean gotMonitor = false;
		    private boolean timerExpired = false;
		    private BitSet changedBitSet = null;
		    private BitSet overrunBitSet = null;
		    private Timer.TimerNode timerNode = TimerFactory.createNode(this);

		    @Override
		    public void init(MonitorImpl monitorImpl,int queueSize) {
		        this.monitorLocal = monitorImpl;
		        monitorElement = MonitorQueueFactory.createMonitorElement(monitorImpl.pvCopy.createPVStructure());
		        PVStructure pvStructure = monitorElement.getPVStructure();
		        changedBitSet = new BitSet(pvStructure.getNumberFields());
		        overrunBitSet = new BitSet(pvStructure.getNumberFields());
		    }
		    @Override
		    public Status start() {
		        gotMonitor = true;
		        changedBitSet.clear();
		        overrunBitSet.clear();
		        monitorLocal.pvCopyMonitor.startMonitoring(
		                changedBitSet,overrunBitSet);
		        timer.schedulePeriodic(timerNode, monitorLocal.periodicRate, monitorLocal.periodicRate);
		        return okStatus;
		    }
		    @Override
		    public void stop() {
		        timerNode.cancel();
		    }
		    @Override
		    public boolean dataChanged() {
		        if(!timerExpired) return false;
		        timerExpired = false;
		        gotMonitor = true;
		        return true;
		    }
		    /* (non-Javadoc)
             * @see org.epics.pvioc.monitor.MonitorFactory.QueueImpl#getChangedBitSet()
             */
            @Override
            public BitSet getChangedBitSet() {
                return changedBitSet;
            }
		    @Override
		    public MonitorElement poll() {
		        if(!gotMonitor) return null;
		        PVStructure pvStructure = monitorElement.getPVStructure();
                monitorLocal.pvCopy.updateCopyFromBitSet(pvStructure, changedBitSet, true);
                bitSetUtil.compress(changedBitSet, pvStructure);
                bitSetUtil.compress(overrunBitSet, pvStructure);
                BitSet ebs = monitorElement.getChangedBitSet();
                ebs.clear();
                ebs.or(changedBitSet);
                ebs = monitorElement.getOverrunBitSet();
                ebs.clear();
                ebs.or(overrunBitSet);
                changedBitSet.clear();
                overrunBitSet.clear();
                return monitorElement;
		    }
		    @Override
		    public void release(MonitorElement monitorElement) {
		        gotMonitor = false;
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
		     */
		    @Override
		    public void callback() {
		        timerExpired = true;
		        monitorLocal.dataChanged();
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
		     */
		    @Override
		    public void timerStopped() {
		        monitorLocal.monitorRequester.message("periodicTimer stopped", MessageType.error);
		    }
		}
	}
}
