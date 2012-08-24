/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.database;

import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.misc.LinkedList;
import org.epics.pvdata.misc.LinkedListCreate;
import org.epics.pvdata.misc.LinkedListNode;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.support.RecordProcess;



/**
 * Base class for a record instance.
 * @author mrk
 *
 */
public class BasePVRecord implements PVRecord {
	private static final Convert convert = ConvertFactory.getConvert();
	private static LinkedListCreate<Requester> requesterListCreate = new LinkedListCreate<Requester>();
	private static LinkedListCreate<PVListener> listenerListCreate = new LinkedListCreate<PVListener>();
	private static LinkedListCreate<PVRecordClient> clientListCreate = new LinkedListCreate<PVRecordClient>();
    private PVRecordStructure pvRecordStructure = null;
    private String recordName;
    private RecordProcess recordProcess = null;
    private LinkedList<Requester> requesterList = requesterListCreate.create();
    private LinkedList<PVListener> pvAllListenerList = listenerListCreate.create();
    private LinkedList<PVRecordClient> clientList = clientListCreate.create();
    private ReentrantLock lock = new ReentrantLock();
    private static volatile int numberRecords = 0;
    private int id = numberRecords++;
    private volatile int depthGroupPut = 0;
    
    /**
     * Create a PVRecord that has pvStructure as it's top level structure.
     * @param recordName The record name.
     * @param pvStructure The top level structure.
     */
    public BasePVRecord(String recordName,PVStructure pvStructure) {
    	if(pvStructure.getParent()!=null) {
    		throw new IllegalStateException(recordName + " pvStructure not a top level structure");
    	}
    	this.recordName = recordName;
    	pvRecordStructure = new BasePVRecordStructure(pvStructure,null,this);
    	pvStructure.setRequester(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.database.PVRecord#getRecordProcess()
     */
    @Override
	public RecordProcess getRecordProcess() {
		if(recordProcess==null) {
			throw new IllegalStateException(recordName + " no recordProcess has been assigned to this record");
		}
		return recordProcess;
	}
	/* (non-Javadoc)
	 * @see org.epics.pvioc.database.PVRecord#setRecordProcess(org.epics.pvioc.support.RecordProcess)
	 */
	@Override
	public void setRecordProcess(RecordProcess recordProcess) {
		if(this.recordProcess!=null) {
			throw new IllegalStateException(recordName + " a recordProcess has already been assigned to this record");
		}
		this.recordProcess = recordProcess;
	}
	/* (non-Javadoc)
     * @see org.epics.pvioc.database.PVRecord#findPVRecordField(org.epics.pvdata.pv.PVField)
     */
    @Override
	public PVRecordField findPVRecordField(PVField pvField) {
		return findPVRecordField(pvRecordStructure,pvField);
	}
    private PVRecordField findPVRecordField(PVRecordStructure pvrs,PVField pvField) {
    	int desiredOffset = pvField.getFieldOffset();
    	PVField pvf = pvrs.getPVField();
    	int offset = pvf.getFieldOffset();
    	if(offset==desiredOffset) return pvrs;
    	PVRecordField[] pvrss = pvrs.getPVRecordFields();
    	for(int i=0; i<pvrss.length; i++) {
    		PVRecordField pvrf = pvrss[i];
    	    pvf = pvrf.getPVField();
    	    offset = pvf.getFieldOffset();
    	    if(offset==desiredOffset) return pvrf;
    	    int nextOffset = pvf.getNextFieldOffset();
    	    if(nextOffset<=desiredOffset) continue;
    	    return findPVRecordField((PVRecordStructure)pvrf,pvField);
    	}
    	throw new IllegalStateException(recordName + " pvField " + pvField.getFieldName() + " not in PVRecord");
    }
	/* (non-Javadoc)
	 * @see org.epics.pvioc.database.PVRecord#getPVRecordStructure()
	 */
	@Override
	public PVRecordStructure getPVRecordStructure() {
		return pvRecordStructure;
	}
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#getRecordName()
     */
    public String getRecordName() {
        return recordName;
    }
    @Override
    public String getRequesterName() {
        return recordName;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.factory.AbstractPVField#message(java.lang.String, org.epics.pvdata.pv.MessageType)
     */
    public void message(String message, MessageType messageType) {
        if(message!=null && message.charAt(0)!='.') message = " " + message;
        if(requesterList.isEmpty()) {
            System.out.println(messageType.toString() + " " + message);
            return;
        }
        // no need to synchronize because record must be locked when this is called.
        LinkedListNode<Requester> node = requesterList.getHead();
        while(node!=null) {
        	Requester requester = node.getObject();
            requester.message(message, messageType);
            node = requesterList.getNext(node);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#addRequester(org.epics.pvdata.pv.Requester)
     */
    public void addRequester(Requester requester) {
        // no need to synchronize because record must be locked when this is called.
        if(requesterList.contains(requester)) {
            requester.message(
                    "addRequester " + requester.getRequesterName() + " but already on requesterList",
                    MessageType.warning);
            return;
        }
        LinkedListNode<Requester> listNode = requesterListCreate.createNode(requester);
        requesterList.addTail(listNode);
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#removeRequester(org.epics.pvdata.pv.Requester)
     */
    public void removeRequester(Requester requester) {
        // no need to synchronize because record must be locked when this is called.
        requesterList.remove(requester);
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#lock()
     */
    public void lock() {
        lock.lock();
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#lockOtherRecord(org.epics.pvdata.pv.PVRecord)
     */
    public void lockOtherRecord(PVRecord otherRecord) {
        BasePVRecord impl = (BasePVRecord)otherRecord;
        int otherId = impl.id;
        if(id<=otherId) {
            otherRecord.lock();
            return;
        }
        int count = lock.getHoldCount();
        for(int i=0; i<count; i++) lock.unlock();
        otherRecord.lock();
        for(int i=0; i<count; i++) lock.lock();
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#unlock()
     */
    public void unlock() {
        lock.unlock();
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#beginGroupPut()
     */
    public void beginGroupPut() {
    	if(++depthGroupPut>1) return;
    	// no need to synchronize because record must be locked when this is called.
    	LinkedListNode<PVListener> listNode = pvAllListenerList.getHead();
    	while(listNode!=null) {
    		PVListener pvListener = listNode.getObject();
    		pvListener.beginGroupPut(this);
    		listNode = pvAllListenerList.getNext(listNode);
    	}
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#endGroupPut()
     */
    public void endGroupPut() {
        if(--depthGroupPut>0) return;
        // no need to synchronize because record must be locked when this is called.
        LinkedListNode<PVListener> listNode = pvAllListenerList.getHead();
    	while(listNode!=null) {
    		PVListener pvListener = listNode.getObject();
    		pvListener.endGroupPut(this);
    		listNode = pvAllListenerList.getNext(listNode);
    	}
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#registerListener(org.epics.pvdata.pv.PVListener)
     */
    public void registerListener(PVListener recordListener) {
        if(pvAllListenerList.contains(recordListener)) {
            message(
                "PVRecord.registerListener called but listener " + recordListener.toString() + " already registered",
                MessageType.warning);
            return;
        }
        LinkedListNode<PVListener> listNode = listenerListCreate.createNode(recordListener);
        pvAllListenerList.addTail(listNode);
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#unregisterListener(org.epics.pvdata.pv.PVListener)
     */
    public void unregisterListener(PVListener recordListener) {
        pvAllListenerList.remove(recordListener);
        pvRecordStructure.removeListener(recordListener);
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#isRegisteredListener(org.epics.pvdata.pv.PVListener)
     */
    public boolean isRegisteredListener(PVListener pvListener) {
        if(pvAllListenerList.contains(pvListener)) return true;
        return false;
    }
    /* (non-Javadoc)
     * @see org.epics.pvdata.pv.PVRecord#removeEveryListener()
     */
    public void removeEveryListener() {
    	while(true) {
    		LinkedListNode<PVListener> listNode = pvAllListenerList.removeHead();
    		if(listNode==null) break;
    		PVListener pvListener = listNode.getObject();
    		pvListener.unlisten(this);
    	}
    }
	/* (non-Javadoc)
	 * @see org.epics.pvdata.pv.PVRecord#registerClient(org.epics.pvdata.pv.PVRecordClient)
	 */
	@Override
	public void registerClient(PVRecordClient pvRecordClient) {
		 if(clientList.contains(pvRecordClient)) {
	            message(
	                "PVRecord.registerClient called but Client " + pvRecordClient.toString() + " already registered",
	                MessageType.warning);
	            return;
	        }
	        LinkedListNode<PVRecordClient> listNode = clientListCreate.createNode(pvRecordClient);
	        clientList.addTail(listNode);
	}
	/* (non-Javadoc)
	 * @see org.epics.pvdata.pv.PVRecord#unregisterClient(org.epics.pvdata.pv.PVRecordClient)
	 */ 
	@Override
	public void unregisterClient(PVRecordClient pvRecordClient) {
		clientList.remove(pvRecordClient);
	}
	/* (non-Javadoc)
	 * @see org.epics.pvdata.pv.PVRecord#detachClients()
	 */
	@Override
	public void detachClients() {
		while(true) {
			LinkedListNode<PVRecordClient> listNode = clientList.removeHead();
			if(listNode==null) break;
			PVRecordClient pvRecordClient = listNode.getObject();
			pvRecordClient.detach(this);
		}
	}
	/* (non-Javadoc)
	 * @see org.epics.pvdata.pv.PVRecord#getNumberClients()
	 */
	@Override
	public int getNumberClients() {
		return clientList.getLength();
	}
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}
   
    /* (non-Javadoc)
     * @see org.epics.pvioc.database.PVRecord#toString(int)
     */
    public String toString(int indentLevel) {
    	StringBuilder builder = new StringBuilder();
    	convert.newLine(builder,indentLevel);
    	builder.append("record ");
    	builder.append(recordName);
    	builder.append(" ");
    	pvRecordStructure.getPVStructure().toString(builder, indentLevel);
    	return builder.toString();
    } 
    
    private static boolean checkSub(PVRecordStructure recordStructure, PVStructure pvStructure)
    {
        boolean result = true;
        PVRecordField[] recordFields = recordStructure.getPVRecordFields();
        PVField[] pvFields = pvStructure.getPVFields();
        int length = recordFields.length;
        if(length!= pvFields.length) {
            result = false;
            System.err.println(recordStructure.getFullName() + " length mismatch");
        }
        for(int i=0; i< length; i++) {
            PVStructure zzz = pvFields[i].getParent();
            if(zzz!=pvStructure) {
                result = false;
                System.err.printf("%s pv pvField parent bad %s %s%n",recordFields[i].getFullName(),zzz,pvStructure.getFieldName());
            }
            PVRecordStructure yyy = recordFields[i].getParent();
            if(yyy!=recordStructure) {
                result = false;
                System.err.printf("%s parent bad %s %s%n",recordFields[i].getFullName(),yyy,recordStructure);
            }
            PVField fromRecord = recordFields[i].getPVField();
            if(fromRecord!=pvFields[i]) {
                result = false;
                System.err.printf("%s pvField do not match%n",recordFields[i].getFullName()); 
            }
            if(pvFields[i].getField().getType()==Type.structure) {
                boolean xxx =checkSub((PVRecordStructure)recordFields[i],(PVStructure)pvFields[i]);
                if(!xxx) result = false;
            }
        }
        return result;
    }
    
    @Override
    public boolean checkValid() {
        return checkSub(pvRecordStructure,pvRecordStructure.getPVStructure());
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return id;
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		return (obj instanceof BasePVRecord && ((BasePVRecord)obj).id == id);
	}
}
