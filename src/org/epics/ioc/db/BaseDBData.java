/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.*;

import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDLinkSupport;
import org.epics.ioc.dbd.DBDSupport;
import org.epics.ioc.process.Support;
import org.epics.ioc.process.SupportCreationFactory;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.*;


/**
 * Abstract class for implementing scalar DB fields.
 * Support for non-array DB data can derive from this class.
 * @author mrk
 *
 */
public class BaseDBData implements DBData{
    private DBData parent;
    private DBRecord dbRecord;
    private PVData pvData;
    private Support support = null;
    private LinkedList<RecordListener> recordListenerList
        = new LinkedList<RecordListener>();
    
    /**
     * Constructor which must be called by classes that derive from this class.
     * @param parent The parent.
     * @param field The reflection interface.
     */
    public BaseDBData(DBData parent,DBRecord record, PVData pvData) {
        this.parent = parent;
        this.dbRecord = record;
        this.pvData = pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getDBRecord()
     */
    public DBRecord getDBRecord() {
        return dbRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.AbstractPVData#getParent()
     */
    public DBData getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getPVData()
     */
    public PVData getPVData() {
        return pvData;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#replacePVData(org.epics.ioc.pv.PVData)
     */
    public void replacePVData(PVData newPVData) {
        pvData.replacePVData(newPVData);
        pvData = newPVData;
        Field field = pvData.getField();
        Type type = field.getType();
        if(type==Type.pvStructure) {
            DBStructure dbStructure = (DBStructure)this;
            dbStructure.replacePVStructure();
        } else if(type==Type.pvArray) {
            Array array = (Array)field;
            if(!array.getElementType().isScalar()) {
                DBNonScalarArray nonScalarArray = (DBNonScalarArray)this;
                nonScalarArray.replacePVArray();
            }
            
        }
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getSupportName()
     */
    public String getSupportName() {
        return pvData.getSupportName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) { 
        DBD dbd = dbRecord.getDBD();
        if(dbd==null) return "DBD was not set";
        DBDLinkSupport dbdLinkSupport = null;
        DBDSupport dbdSupport = null;
        Type type = pvData.getField().getType();
        if(type==Type.pvLink) {
            dbdLinkSupport = dbd.getLinkSupport(name);
            if(dbdLinkSupport==null) {
                dbd = DBDFactory.getMasterDBD();
                dbdLinkSupport = dbd.getLinkSupport(name);
            }
            if(dbdLinkSupport==null) {
                return "linkSupport " + name + " not defined";
            }
            String result = ((DBLink)this).newSupport(dbdLinkSupport,dbd);
            if(result!=null) return result;
        } else {
            dbdSupport = dbd.getSupport(name);
            if(dbdSupport==null) {
                dbd = DBDFactory.getMasterDBD();
                dbdSupport = dbd.getSupport(name);
            }
            if(dbdSupport==null) {
                return "support " + name + " not defined";
            }
        }
        pvData.setSupportName(name);
        if(support==null) {
            // Wait until SupportCreation has been run
            return null;
        }
        SupportState supportState = support.getSupportState();
        if(supportState!=SupportState.readyForInitialize) {
            support.uninitialize();
        }
        support = null;
        Iterator<RecordListener> iter;
        iter = recordListenerList.iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.supportNamePut(this);
        }
        DBData dbData = parent;
        while(dbData!=null) {
            iter = dbData.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.supportNamePut(dbData, this);
            }
            dbData = dbData.getParent();
        }
        if(!SupportCreationFactory.createSupport(this)) {
            return "could not create support";
        }
        if(support==null) {
            return "support does not exist";
        }
        // if configurationStructure then wait for it to be initialized before changing state.
        if(type==Type.pvLink) {
            if(dbdLinkSupport.getConfigurationStructureName()!=null) return null;
        }
        DBStructure dbStructure = dbRecord.getDBStructure();
        if(this==dbStructure) return "dbRecord has no support";
        supportState = dbStructure.getSupport().getSupportState();
        switch(supportState) {
        case readyForInitialize:
            break;
        case readyForStart:
            support.initialize();
            break;
        case ready:
            support.initialize();
            if(support.getSupportState()!=SupportState.readyForStart) break;
            support.start();
            break;
        default:
        }
        return null;
    }
/* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getSupport()
     */
    public Support getSupport() {
        return support;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#setSupport(org.epics.ioc.process.Support)
     */
    public void setSupport(Support support) {
        this.support = support;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#postPut()
     */
    public void postPut() {
        Iterator<RecordListener> iter;
        iter = recordListenerList.iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.dataPut(this);
        }
        DBData dbData = parent;
        while(dbData!=null) {
            iter = dbData.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.dataPut(dbData, this);
            }
            dbData = dbData.getParent();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#addListener(org.epics.ioc.db.RecordListener)
     */
    public void addListener(RecordListener recordListener) {
        if(recordListenerList.isEmpty()) dbRecord.addListenerSource(this);
        recordListenerList.add(recordListener);
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#removeListener(org.epics.ioc.db.RecordListener)
     */
    public void removeListener(RecordListener recordListener) {
        recordListenerList.remove(recordListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getRecordListenerList()
     */
    public List<RecordListener> getRecordListenerList() {
        return recordListenerList;
    }
    /**
     * Called by BasePVRecord when DBRecord.removeListener or DBrecord.removeListeners are called.
     */
    protected void removeRecordListeners(){
        recordListenerList.clear();
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBData#toString(int)
     */
    public String toString(int indentLevel) {
        return pvData.toString(indentLevel);
    }
}
