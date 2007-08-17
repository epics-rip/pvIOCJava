/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.*;

import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDStructure;
import org.epics.ioc.dbd.DBDSupport;
import org.epics.ioc.process.SupportCreationFactory;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.*;
import org.epics.ioc.support.Support;
import org.epics.ioc.create.Create;


/**
 * Abstract class for implementing scalar DB fields.
 * Support for non-array DB data can derive from this class.
 * @author mrk
 *
 */
public class BaseDBField implements DBField{
    private DBField parent;
    private DBRecord dbRecord;
    private PVField pvField;
    private Support support = null;
    private Create create = null;
    private LinkedList<RecordListener> recordListenerList
        = new LinkedList<RecordListener>();
    
    /**
     * Constructor which must be called by classes that derive from this class.
     * @param parent The parent.
     * @param record The DBRecord to which this field belongs.
     * @param pvField The reflection interface.
     */
    public BaseDBField(DBField parent,DBRecord record, PVField pvField) {
        this.parent = parent;
        this.dbRecord = record;
        this.pvField = pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#getDBRecord()
     */
    public DBRecord getDBRecord() {
        return dbRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#getParent()
     */
    public DBField getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#getPVField()
     */
    public PVField getPVField() {
        return pvField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#replacePVField(org.epics.ioc.pv.PVField)
     */
    public void replacePVField(PVField newPVField) {
        pvField.replacePVField(newPVField);
        pvField = newPVField;
        Field field = pvField.getField();
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
     * @see org.epics.ioc.db.DBField#getSupportName()
     */
    public String getSupportName() {
        return pvField.getSupportName();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) {
        if(support!=null) {
            if(support.getSupportState()!=SupportState.readyForInitialize) {
                return "!SupportState.readyForInitialize";
            }
        }
        DBD dbd = dbRecord.getDBD();
        if(dbd==null) return "DBD was not set";
        DBDSupport dbdSupport = dbd.getSupport(name);
        if(dbdSupport==null) return "support " + name + " not defined";
        Type type = pvField.getField().getType();
        if(type==Type.pvStructure) {
            String structureName = dbdSupport.getStructureName();
            if(structureName!=null) {
                DBDStructure dbdStructure= dbd.getStructure(structureName);        
                if(dbdStructure==null) {
                    return "structure " + structureName
                    + " for support " + name
                    + " does not exist";
                }
                FieldCreate fieldCreate = FieldFactory.getFieldCreate();
                Field[] fields = dbdStructure.getFields();
                Structure structure = fieldCreate.createStructure(
                        pvField.getField().getFieldName(),
                        dbdStructure.getStructureName(),
                        fields);
                PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
                pvField = pvDataCreate.createPVField(parent.getPVField(), structure);
            }
        }
        pvField.setSupportName(name);
        return null;
    }
/* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#getSupport()
     */
    public Support getSupport() {
        return support;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#setSupport(org.epics.ioc.process.Support)
     */
    public void setSupport(Support support) {
        this.support = support;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#getCreate()
     */
    public Create getCreate() {
        return create;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#setCreate(org.epics.ioc.create.Create)
     */
    public void setCreate(Create create) {
        this.create = create;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#postPut()
     */
    public void postPut() {
        Iterator<RecordListener> iter;
        iter = recordListenerList.iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.dataPut(this);
        }
        DBField dbField = parent;
        while(dbField!=null) {
            iter = dbField.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.dataPut(dbField, this);
            }
            dbField = dbField.getParent();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#addListener(org.epics.ioc.db.RecordListener)
     */
    public void addListener(RecordListener recordListener) {
        if(recordListenerList.isEmpty()) dbRecord.addListenerSource(this);
        recordListenerList.add(recordListener);
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#removeListener(org.epics.ioc.db.RecordListener)
     */
    public void removeListener(RecordListener recordListener) {
        recordListenerList.remove(recordListener);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#getRecordListenerList()
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
     * @see org.epics.ioc.db.BaseDBField#toString(int)
     */
    public String toString(int indentLevel) {
        return pvField.toString(indentLevel);
    }
}
