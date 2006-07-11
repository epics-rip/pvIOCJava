/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbProcess.RecordProcess;
import org.epics.ioc.dbProcess.RecordSupport;
import org.epics.ioc.pvAccess.*;

/**
 * Abstract base class for a record instance.
 * @author mrk
 *
 */
public class AbstractDBRecord extends AbstractDBStructure implements DBRecord {
    private static AtomicInteger numberRecords = new AtomicInteger();
    private int id = numberRecords.addAndGet(1);
    private String recordName;
    private ReentrantLock lock = new ReentrantLock();
    private AtomicReference<RecordSupport> recordSupport = 
        new AtomicReference<RecordSupport>();
    private AtomicReference<RecordProcess> recordProcess = 
        new AtomicReference<RecordProcess>();
    private static Convert convert = ConvertFactory.getConvert();
    private ConcurrentLinkedQueue<ListenerPvt> listenerList
        = new ConcurrentLinkedQueue<ListenerPvt>();
    
    /**
     * constructor that derived clases must call.
     * @param recordName the name of the record.
     * @param dbdRecordType the introspection interface for the record.
     */
    protected AbstractDBRecord(String recordName,DBDRecordType dbdRecordType)
    {
        super(dbdRecordType);
        this.recordName = recordName;
        super.setRecord(this);
        super.createFields(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordName()
     */
    public String getRecordName() {
        return recordName;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordSupport()
     */
    public RecordSupport getRecordSupport() {
        return recordSupport.get();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#setRecordSupport(org.epics.ioc.dbProcess.RecordSupport)
     */
    public boolean setRecordSupport(RecordSupport support) {
        return recordSupport.compareAndSet(null,support);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordProcess()
     */
    public RecordProcess getRecordProcess() {
        return recordProcess.get();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#setRecordProcess(org.epics.ioc.dbProcess.RecordProcess)
     */
    public boolean setRecordProcess(RecordProcess process) {
        return recordProcess.compareAndSet(null,process);
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getLock()
     */
    public ReentrantLock getLock() {
        return lock;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#lockOther(java.util.concurrent.locks.ReentrantLock)
     */
    public ReentrantLock lockOtherRecord(DBRecord otherRecord) {
        ReentrantLock lockOther = otherRecord.getLock();
        if(lockOther.tryLock()) return lockOther;
        int otherId = otherRecord.getRecordID();
        if(id<=otherId) {
            lockOther.lock();
            return lockOther;
        }
        lock.unlock();
        lockOther.lock();
        lock.lock();
        return lockOther;
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordID()
     */
    public int getRecordID() {
        return id;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#beginSynchronous()
     */
    public void beginSynchronous() {
        Iterator<ListenerPvt> iter = listenerList.iterator();
        while(iter.hasNext()) {
            ListenerPvt listener = iter.next();
            listener.beginSynchronous();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#endSynchronous()
     */
    public void endSynchronous() {
        Iterator<ListenerPvt> iter = listenerList.iterator();
        while(iter.hasNext()) {
            ListenerPvt listener = iter.next();
            listener.endSynchronous();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#createListener(org.epics.ioc.dbAccess.DBListener)
     */
    public Listener createListener(DBListener listener) {
        ListenerPvt listenerPvt = new ListenerPvt(listener);
        listenerList.add(listenerPvt);
        return listenerPvt;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#destroyListener(org.epics.ioc.dbAccess.Listener)
     */
    public void destroyListener(Listener listener) {
        listenerList.remove(listener);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        newLine(builder,indentLevel);
        Structure structure = (Structure)this.getField();
        DBData[] dbData = super.getFieldDBDatas();
        PVData[] pvData = super.getFieldPVDatas();
        RecordSupport recordSupport = this.getRecordSupport();
        String supportName = null;
        if(recordSupport!=null) {
            supportName = recordSupport.getName();
        } else {
            supportName = "none";
        }
        builder.append("record " + recordName + " recordType " + structure.getStructureName()
                + " support " + supportName + "{");
        for(int i=0, n= dbData.length; i < n; i++) {
            newLine(builder,indentLevel + 1);
            Field field = pvData[i].getField();
            builder.append(field.getName() + " = ");
            DBDField dbdField = dbData[i].getDBDField();
            switch(dbdField.getDBType()) {
            case dbPvType:
                builder.append(convert.getString(
                    dbData[i],indentLevel + 2));
                break;
            case dbMenu:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                break;
            case dbStructure:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                break;
            case dbArray:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                break;
            case dbLink:
                builder.append(dbData[i].toString(
                    indentLevel + 2));
                 break;
            }
            
        }
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
    private static class ListenerPvt implements Listener {
        private DBListener dbListener;
        private boolean isSynchronous = false;
        private boolean sentWhileSynchronous = false;
        
        ListenerPvt(DBListener listener) {
            dbListener = listener;
        }

        public void newData(DBData data) {
            if(isSynchronous && !sentWhileSynchronous) {
                dbListener.beginSynchronous();
                sentWhileSynchronous = true;
            }
            dbListener.newData(data);
        }
        
        void beginSynchronous() {
            isSynchronous = true;
            sentWhileSynchronous = false;
        }
        
        void endSynchronous() {
            if(sentWhileSynchronous) dbListener.endSynchronous();
            isSynchronous = false;
        }
    }
}
