/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.monitor;

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
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Scalar;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Status.StatusType;
import org.epics.pvdata.pv.StatusCreate;
import org.epics.pvdata.pv.Type;
import org.epics.pvdata.copy.*;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.pvAccess.*;


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
    private static final Status wasDestroyedStatus = statusCreate.createStatus(StatusType.ERROR,"was destroyed",null);
	private static final LinkedList<MonitorAlgorithmCreate> monitorAlgorithmCreateList = monitorAlgorithmCreateListCreate.create();
	private static final BitSetUtil bitSetUtil = BitSetUtilFactory.getCompressBitSet();
	private static final Timer timer = TimerFactory.create("periodicMonitor", ThreadPriority.high);
	private static MonitorAlgorithmCreate algorithmDeadband;
	
	static {
		AlgorithmOnChangeFactory.register();
		AlgorithmDeadbandFactory.register();
		LinkedListNode<MonitorAlgorithmCreate> node = monitorAlgorithmCreateList.getHead();
		while(node!=null) {
			MonitorAlgorithmCreate algorithmCreate = node.getObject();
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
	
	
	
	private static class MonitorImpl implements Monitor,PVCopyMonitorRequester {
		private final PVRecord pvRecord;
		private final MonitorRequester monitorRequester;
		
		private boolean isPeriodic = false;
		private double periodicRate = 1.0;
		private PVCopy pvCopy = null;
		private ElementQueue queueImpl = null;
		private PVCopyMonitor pvCopyMonitor;
		private final LinkedList<MonitorFieldNode> monitorFieldList = MonitorFieldNodeListCreate.create();
		
        private boolean firstMonitor = false;
        private BitSet notMonitoredBitSet = null;
        private boolean isDestroyed = false;
        
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
		        if(isDestroyed) return null;
		        return queueImpl.poll();
		    }
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#release(org.epics.pvdata.monitor.MonitorElement)
		 */
		@Override
		public void release(MonitorElement currentElement) {
		    synchronized(queueImpl) {
		        if(isDestroyed) return;
		        queueImpl.release(currentElement);
		    }
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.monitor.Monitor#start()
		 */
		@Override
		public Status start() {
		    synchronized(queueImpl) {
		        if(isDestroyed) return wasDestroyedStatus;
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
		        if(!isDestroyed) queueImpl.stop();
		        return okStatus;
		    }
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.misc.Destroyable#destroy()
		 */
		@Override
		public void destroy() {
		    synchronized(queueImpl) {
		        if(isDestroyed) return;
		        isDestroyed = true;
		    }
		    unlisten();
			stop();
		}
		/* (non-Javadoc)
		 * @see org.epics.pvdata.pvCopy.PVCopyMonitorRequester#dataChanged()
		 */
		@Override
		public void dataChanged() {
		    boolean notifyClient = false;
		    synchronized(queueImpl) {
		        if(isDestroyed) return;
		        if(firstMonitor) {
		            queueImpl.dataChanged();
		            firstMonitor = false;
		            notifyClient = true;
		        } else {
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
		            notifyClient = queueImpl.dataChanged();
		            LinkedListNode<MonitorFieldNode> listNode  = monitorFieldList.getHead();
		            while(listNode!=null) {
		                MonitorFieldNode node = listNode.getObject();
		                node.monitorAlgorithm.monitorIssued();
		                listNode = monitorFieldList.getNext(listNode);
		            }
		        }
		    }
		    if(notifyClient) monitorRequester.monitorEvent(this);
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
				pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest, "");
				if(pvCopy==null) {
					monitorRequester.message("illegal pvRequest", MessageType.error);
					return false;
				}
			} else {
				if(!(pvField instanceof PVStructure)) {
					monitorRequester.message("illegal pvRequest.field", MessageType.error);
					return false;
				}
				pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest, "field");
				if(pvCopy==null) {
					monitorRequester.message("illegal pvRequest", MessageType.error);
					return false;
				}
			}
			pvCopyMonitor = PVCopyMonitorFactory.create(this, pvRecord, pvCopy);
			if(queueSize<2) queueSize = 2;
            queueImpl = new ElementQueue();
			queueImpl.init(this,queueSize);
			PVStructure pvStructure = pvCopy.createPVStructure();
			notMonitoredBitSet = new BitSet(pvStructure.getNumberFields());
			notMonitoredBitSet.clear();
			notMonitoredBitSet.set(0);
			pvCopy.updateCopyFromBitSet(pvStructure,notMonitoredBitSet);
			notMonitoredBitSet.clear();
			boolean result = initField(pvStructure,pvStructure);
			if(result) {
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
		        PVStructure pvOptions = pvCopy.getOptions(offset);
		        if(pvOptions!=null && pvOptions.getSubField("algorithm")!=null) {
		            PVRecordField pvRecordField = pvRecord.findPVRecordField(pvCopy.getMasterPVField(offset));
		            boolean result = initMonitorField(pvOptions,pvCopyField,pvRecordField);
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
						PVRecordField pvRecordField = pvRecord.findPVRecordField(pvCopy.getMasterPVField(bitOffset));
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
		
		
		private static class ElementQueue implements Timer.TimerCallback {
		    private MonitorImpl monitorLocal = null;
		    private MonitorQueue monitorQueue = null;
		    private MonitorElement activeElement = null;
		    private boolean queueIsFull = false;
		    private boolean isPeriodic = false;
		    private boolean timerExpired = true;
		    private Timer.TimerNode timerNode = TimerFactory.createNode(this);

		   
		    public void init(MonitorImpl monitorImpl,int queueSize) {
		        monitorLocal = monitorImpl;
		        MonitorElement[] elements = new MonitorElement[queueSize];
		        for(int i=0; i<elements.length;i++) {   
		            elements[i] = MonitorQueueFactory.createMonitorElement(monitorImpl.pvCopy.createPVStructure());
		        }
		        monitorQueue = MonitorQueueFactory.create(elements);
		        isPeriodic = monitorLocal.isPeriodic;
		    }
		    
		    public Status start() {
		        monitorQueue.clear();
		        queueIsFull = false;
		        activeElement = monitorQueue.getFree();
		        activeElement.getChangedBitSet().clear();
		        activeElement.getOverrunBitSet().clear();
		        monitorLocal.pvCopyMonitor.setMonitorElement(activeElement);
		        monitorLocal.pvCopyMonitor.startMonitoring();
		        if(isPeriodic) timer.schedulePeriodic(timerNode, monitorLocal.periodicRate, monitorLocal.periodicRate);
		        return okStatus;
		    }
		   
		    public void stop() {}
		    /* (non-Javadoc)
		     * @see org.epics.pvioc.monitor.MonitorFactory.ElementQueue#dataChanged()
		     */
		    public synchronized boolean dataChanged() {
		        if(isPeriodic) {
		            if(!timerExpired) return false;
		            timerExpired = false;
		        }
		        if(queueIsFull) return false;
		        PVStructure pvStructure = activeElement.getPVStructure();
                BitSet changedBitSet = activeElement.getChangedBitSet();
                BitSet overrunBitSet = activeElement.getOverrunBitSet();
                monitorLocal.pvCopy.updateCopyFromBitSet(pvStructure, changedBitSet);
                bitSetUtil.compress(changedBitSet, pvStructure);
                bitSetUtil.compress(overrunBitSet, pvStructure);
                monitorQueue.setUsed(activeElement);
                activeElement = monitorQueue.getFree();
                if(activeElement==null) {
                    throw new IllegalStateException("MultipleElementQueue::dataChanged() logic error");
                }
		        if(monitorQueue.getNumberFree()==0) queueIsFull = true;
		        activeElement.getChangedBitSet().clear();
		        activeElement.getOverrunBitSet().clear();
		        monitorLocal.pvCopyMonitor.setMonitorElement(activeElement);
                return true;
		    }
		   
            
            public synchronized BitSet getChangedBitSet() {
                return activeElement.getChangedBitSet();
            }
		    
		    public synchronized MonitorElement poll() {
		        return monitorQueue.getUsed();
		    }
		    
		    public synchronized void release(MonitorElement currentElement) {
		        monitorQueue.releaseUsed(currentElement);
		        if(!queueIsFull) return;
		        queueIsFull = false;
		        if(!activeElement.getChangedBitSet().isEmpty()) {
		            dataChanged();
		        }
		    }
		    
		    /* (non-Javadoc)
		     * @see org.epics.pvdata.misc.Timer.TimerCallback#callback()
		     */
		    public void callback() {
		        timerExpired = true;
		        monitorLocal.dataChanged();
		    }
		    /* (non-Javadoc)
		     * @see org.epics.pvdata.misc.Timer.TimerCallback#timerStopped()
		     */
		    public void timerStopped() {
                monitorLocal.monitorRequester.message("periodicTimer stopped", MessageType.error);
            }
		}
	}
}
