/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import java.util.*;
import java.util.concurrent.locks.*;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.util.MessageType;

/**
 * Abstract base class for a record instance.
 * @author mrk
 *
 */
public class AbstractDBRecord extends AbstractDBStructure implements DBRecord {
    private static int numberRecords = 0;
    private int id = numberRecords++;
    private String recordName;
    private IOCDB iocdb = null;
    private RecordState recordState;
    private ReentrantLock lock = new ReentrantLock();
    private RecordProcess recordProcess = null;
    private LinkedList<RecordStateListener> recordStateListenerList
        = new LinkedList<RecordStateListener>();
    private LinkedList<RecordListenerPvt> recordListenerList
        = new LinkedList<RecordListenerPvt>();
    private LinkedList<AbstractDBData> listenerSourceList
          = new LinkedList<AbstractDBData>();
    private DBD dbd = null;
    
    /**
     * constructor that derived clases must call.
     * @param recordName the name of the record.
     * @param dbdRecordType the introspection interface for the record.
     */
    protected AbstractDBRecord(String recordName,DBDRecordType dbdRecordType)
    {
        super(dbdRecordType);
        this.recordName = recordName;
        recordState = RecordState.constructed;
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
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordState()
     */
    public RecordState getRecordState() {
        return recordState;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#setRecordState(org.epics.ioc.dbAccess.RecordState)
     */
    public void setRecordState(RecordState state) {
        recordState = state;
        Iterator<RecordStateListener> iter = recordStateListenerList.iterator();
        while(iter.hasNext()) {
            RecordStateListener listener = iter.next();
            listener.newState(this,state);
        }
        if(state==RecordState.zombie) {
            recordProcess = null;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#addRecordStateListener(org.epics.ioc.dbAccess.RecordStateListener)
     */
    public void addRecordStateListener(RecordStateListener listener) {
        recordStateListenerList.add(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#removeRecordStateListener(org.epics.ioc.dbAccess.RecordStateListener)
     */
    public void removeRecordStateListener(RecordStateListener listener) {
        recordStateListenerList.remove(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#lock()
     */
    public void lock() {
        lock.lock();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#unlock()
     */
    public void unlock() {
        lock.unlock();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#lockOtherRecord(org.epics.ioc.dbAccess.DBRecord)
     */
    public void lockOtherRecord(DBRecord otherRecord) {
        int otherId = otherRecord.getRecordID();
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
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordProcess()
     */
    public RecordProcess getRecordProcess() {
        return recordProcess;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#setRecordProcess(org.epics.ioc.dbProcess.RecordProcess)
     */
    public boolean setRecordProcess(RecordProcess process) {
        if(recordProcess!=null) return false;
        recordProcess = process;
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getRecordID()
     */
    public int getRecordID() {
        return id;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#beginProcess()
     */
    public void beginProcess() {
        Iterator<RecordListenerPvt> iter = recordListenerList.iterator();
        while(iter.hasNext()) {
            RecordListenerPvt listener = iter.next();
            listener.beginProcess();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#endProcess()
     */
    public void endProcess() {
        Iterator<RecordListenerPvt> iter = recordListenerList.iterator();
        while(iter.hasNext()) {
            RecordListenerPvt listener = iter.next();
            listener.endProcesss();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#createListener(org.epics.ioc.dbAccess.DBListener)
     */
    public RecordListener createListener(DBListener listener) {
        RecordListenerPvt recordListenerPvt = new RecordListenerPvt(listener);
        recordListenerList.add(recordListenerPvt);
        return recordListenerPvt;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#destroyListener(org.epics.ioc.dbAccess.RecordListener)
     */
    public void removeListener(RecordListener listener) {
        Iterator<AbstractDBData> iter = listenerSourceList.iterator();
        while(iter.hasNext()) {
            AbstractDBData dbData = iter.next();
            dbData.removeListener(listener);
        }
        recordListenerList.remove(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.AbstractDBData#removeListeners()
     */
    public void removeListeners() {
        while(true) {
            RecordListenerPvt listener = recordListenerList.remove();
            if(listener==null) break;
            listener.unlisten();
            Iterator<AbstractDBData> iter = listenerSourceList.iterator();
            while(iter.hasNext()) {
                AbstractDBData dbData = iter.next();
                dbData.removeListener(listener);
            }
        }
        listenerSourceList.clear();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#addListenerSource(org.epics.ioc.dbAccess.AbstractDBData)
     */
    public void addListenerSource(AbstractDBData dbData) {
        listenerSourceList.add(dbData);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getDBD()
     */
    public DBD getDBD() {
        return dbd;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#setDBD(org.epics.ioc.dbDefinition.DBD)
     */
    public void setDBD(DBD dbd) {
        this.dbd = dbd;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#getIOCDB()
     */
    public IOCDB getIOCDB() {
        return iocdb;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#setIOCDB(org.epics.ioc.dbAccess.IOCDB)
     */
    public void setIOCDB(IOCDB iocdb) {
        this.iocdb = iocdb;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBRecord#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        if(message!=null && message.charAt(0)!='.') message = " " + message;
        iocdb.message(recordName + message, messageType);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return super.toString(recordName + " recordType",indentLevel);
    }

    private static class RecordListenerPvt implements RecordListener {
        private DBListener dbListener;
        
        void unlisten() {
            dbListener.unlisten(this);
        }

        RecordListenerPvt(DBListener listener) {
            dbListener = listener;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbAccess.RecordListener#newData(org.epics.ioc.dbAccess.DBData)
         */
        public void newData(DBData data) {
            dbListener.newData(data);
        }
        
        void beginProcess() {
            dbListener.beginProcess();
        }
        
        void endProcesss() {
            dbListener.endProcess();
        }
    }
}
