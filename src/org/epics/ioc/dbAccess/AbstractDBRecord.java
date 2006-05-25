/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * Abstract base class for a record instance.
 * @author mrk
 *
 */
public class AbstractDBRecord extends AbstractDBStructure implements DBRecord {
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordName()
     */
    public String getRecordName() {
        return recordName;
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#readLock()
     */
    public void readLock() {
        readLock.lock();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#readUnlock()
     */
    public void readUnlock() {
        readLock.unlock();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#writeLock()
     */
    public void writeLock() {
        writeLock.lock();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#writeUnlock()
     */
    public void writeUnlock() {
        writeLock.unlock();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#addListener(org.epics.ioc.dbAccess.DBListener)
     */
    public void addListener(DBListenerPvt listener) {
        listenerList.add(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#removeListener(org.epics.ioc.dbAccess.DBListener)
     */
    public void removeListener(DBListenerPvt listener) {
        listenerList.remove(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#insertMasterListener(org.epics.ioc.dbAccess.DBListener)
     */
    public boolean insertMasterListener(DBMasterListener listener) {
        return masterListener.compareAndSet(null,listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#removeMasterListener(org.epics.ioc.dbAccess.DBListener)
     */
    public void removeMasterListener(DBMasterListener listener) {
        if(!masterListener.compareAndSet(listener,null)) {
            throw new IllegalStateException("DBData.removeMasterListener called by non master");
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#postMaster(org.epics.ioc.dbAccess.DBData)
     */
    public boolean postMaster(DBData dbData) {
        DBMasterListener listener = masterListener.get();
        if(listener==null) return false;
        listener.newData(dbData);
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#startSynchronous()
     */
    public void startSynchronous() {
        if(masterListener.get()==null) {
            throw new IllegalStateException(
                "DBRecord.postForMaster but not masterListener");
        }
        Iterator<DBListenerPvt> iter = listenerList.iterator();
        while(iter.hasNext()) {
            DBListenerPvt listener = iter.next();
            listener.isSynchronous = true;
            listener.sentSynchronous = false;
        }
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#stopSynchronous()
     */
    public void stopSynchronous() {
        if(masterListener.get()==null) {
            throw new IllegalStateException(
                "DBRecord.postForMaster but not masterListener");
        }
        Iterator<DBListenerPvt> iter = listenerList.iterator();
        while(iter.hasNext()) {
            DBListenerPvt listener = iter.next();
            if(listener.sentSynchronous) {
                listener.listener.endSynchronous();
            }
            listener.isSynchronous = false;
            listener.sentSynchronous = false;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#postForMaster(org.epics.ioc.dbAccess.DBData)
     */
    public void postForMaster(DBData dbData) {
        if(masterListener.get()==null) {
            throw new IllegalStateException(
                "DBRecord.postForMaster but not masterListener");
        }
        dbData.postPut(dbData);
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
        builder.append("record " + recordName + " recordType " + structure.getStructureName() + "{");
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
    
    /**
     * constructor that derived clases must call.
     * @param recordName the name of the record.
     * @param dbdRecordType the introspection interface for the record.
     */
    protected AbstractDBRecord(String recordName,DBDRecordType dbdRecordType)
    {
        super(dbdRecordType);
        this.recordName = recordName;
        readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        listenerList = new ConcurrentLinkedQueue<DBListenerPvt>();
        super.setRecord(this);
        super.createFields(this);
    }

    private String recordName;
    private ReentrantReadWriteLock readWriteLock;
    private ReentrantReadWriteLock.ReadLock readLock;
    private ReentrantReadWriteLock.WriteLock writeLock;
    AtomicReference<DBMasterListener> masterListener = 
        new AtomicReference<DBMasterListener>(null);
    private static Convert convert = ConvertFactory.getConvert();
    private ConcurrentLinkedQueue<DBListenerPvt> listenerList;
    
}
