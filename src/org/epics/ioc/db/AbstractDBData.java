/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.dbd.*;
import org.epics.ioc.dbd.DBDLinkSupport;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;

import java.util.*;

/**
 * Abstract class for implementing scalar DB fields.
 * Support for non-array DB data can derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBData extends AbstractPVData implements DBData{
    protected DBData parent;
    private   DBRecord record;
    protected LinkedList<RecordListener> listenerList
        = new LinkedList<RecordListener>(); 
    private   Support support = null;
    protected PVStructure thisStructure = null;
    
    /**
     * Constructor which must be called by classes that derive from this class.
     * @param parent The parent structure.
     * @param field The reflection interface.
     */
    protected AbstractDBData(DBData parent, Field field) {
        super(parent,field);
        this.parent = parent;
        if(parent!=null) {
            record = parent.getDBRecord();
            super.setRecord(record);
        } else {
            record = null;
        }
        String supportName = field.getSupportName();
        if(supportName!=null) {
            super.setSupportName(supportName);
        }
        if(field.getType()==Type.pvStructure) thisStructure = (PVStructure)this;
    }
    /**
     * Called by DBStructureBase when it replaces a field of the structure.
     * @param field The new field.
     */
    protected void replaceField(Field field) {
        super.replaceField(field);
    }
    /**
     * specify the record that holds this data.
     * This is called by DBRecordBase.
     * @param record the record instance containing this field.
     */
    protected void setRecord(DBRecord record) {
        this.record = record;
        super.setRecord((PVRecord)record);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getDBRecord()
     */
    public DBRecord getDBRecord() {
        return record;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#addListener(org.epics.ioc.db.RecordListener)
     */
    public void addListener(RecordListener listener) {
        if(listenerList.isEmpty()) record.addListenerSource(this);
        listenerList.add(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#removeListener(org.epics.ioc.db.RecordListener)
     */
    public void removeListener(RecordListener listener) {
        listenerList.remove(listener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#postPut()
     */
    public final void postPut() {
        AbstractDBData dbData = this;
        while(dbData!=null) {
            Iterator<RecordListener> iter = dbData.listenerList.iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                Type type = dbData.getField().getType();
                if(type.isScalar()||type==Type.pvArray) {
                    dbListener.dataPut(dbData);
                } else if(type==Type.pvStructure) {
                    dbListener.dataPut(dbData.thisStructure, this);
                } else if(type==Type.pvLink) {
                    // let PVLink handle the change
                } else {
                    throw new IllegalStateException("postPut called for illegal Type");
                }
            }
            if(dbData.parent==dbData) {
                System.err.printf("postPut parent = this Why???%n");
            } else {
                dbData = (AbstractDBData)dbData.parent;
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) {
        DBD dbd = record.getDBD();
        if(dbd==null) return "DBD was not set";
        DBDLinkSupport dbdLinkSupport = null;
        DBDSupport dbdSupport = null;
        if(getField().getType()==Type.pvLink) {
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
        if(support==null) {
            // Wait until SupportCreation has been run
            return super.setSupportName(name);
        }
        SupportState supportState = SupportState.readyForInitialize;
        if(support!=null) {
            supportState = support.getSupportState();
            if(supportState!=SupportState.readyForInitialize) {
                support.uninitialize();
            }
            support = null;
        }
        String result = super.setSupportName(name);
        if(result!=null) return result;
        AbstractDBData dbData = this;
        while(dbData!=null) {
            Iterator<RecordListener> iter = dbData.listenerList.iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                Type type = dbData.getField().getType();
                if(type.isScalar()||type==Type.pvArray) {
                    dbListener.supportNamePut(dbData);
                } else if(type==Type.pvStructure) {
                    dbListener.supportNamePut(dbData.thisStructure, this);
                } else {
                    throw new IllegalStateException("postPut called for illegal Type");
                }
            }
            if(dbData.parent==dbData) {
                System.err.printf("postPut parent = this Why???%n");
            } else {
                dbData = (AbstractDBData)dbData.parent;
            }
        }
        if(!SupportCreationFactory.createSupport(this)) {
            return "could not create support";
        }
        if(support==null) {
            return "support does not exist";
        }
        // if configurationStructure then wait for it to be initialized before changing state.
        if(getField().getType()==Type.pvLink) {
            if(dbdLinkSupport.getConfigurationStructureName()!=null) return null;
        }
        if(this!=record) {
            if(record.getSupport()==null) return "record has no support";
            supportState = record.getSupport().getSupportState();
        }
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
    /**
     * Called by DBRecordBase when DBRecord.removeListener or DBrecord.removeListeners are called.
     */
    protected void removeRecordListeners(){
        listenerList.clear();
    }
}
