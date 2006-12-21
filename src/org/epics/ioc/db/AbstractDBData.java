/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

import java.util.*;

/**
 * Abstract class for implementing scalar DB fields.
 * Support for non-array DB data can derive from this class.
 * @author mrk
 *
 */
public abstract class AbstractDBData extends AbstractPVData implements DBData{
    private DBData parent;
    private DBRecord record;
    protected LinkedList<RecordListener> listenerList
        = new LinkedList<RecordListener>(); 
    private Support support = null;
    private PVStructure thisStructure = null;
    
    /**
     * constructor which must be called by classes that derive from this class.
     * @param parent the parent structure.
     * @param field the reflection interface for the DBData data.
     */
    protected AbstractDBData(DBData parent, Field field) {
        super(parent,field);
        this.parent = parent;
        if(parent!=null) {
            record = parent.getRecord();
        } else {
            record = null;
        }
        if(field.getType()==Type.pvStructure) thisStructure = (PVStructure)this;
    }
    /**
     * Called by AbstractDBStructure when it replaces a field of the structure.
     * @param field The new field.
     */
    protected void replaceField(Field field) {
        super.replaceField(field);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return toString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return super.toString(indentLevel);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getFullFieldName()
     */
    public String getFullFieldName() {
        if(this==record) return "";
        StringBuilder fieldName = new StringBuilder();
        fieldName.insert(0,getField().getFieldName());
        if(parent.getField().getType()!=Type.pvArray) fieldName.insert(0,".");
        PVData parent = getParent();
        while(parent!=null && parent!=this.record) {
            PVData now = parent;
            fieldName.insert(0,now.getField().getFieldName());
            if(now.getParent()==null
            || now.getParent().getField().getType()!=Type.pvArray) fieldName.insert(0,".");
            parent = now.getParent();
        }
        return fieldName.toString();
    }   
    /**
     * specify the record that holds this data.
     * This is called by AbstractDBRecord.
     * @param record the record instance containing this field.
     */
    protected void setRecord(DBRecord record) {
        this.record = record;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getParent()
     */
    public PVData getParent() {
        return parent;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBData#getRecord()
     */
    public DBRecord getRecord() {
        return record;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getField()
     */
    public Field getField() {
        return super.getField();
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
                listener.newData(dbData.thisStructure,this);
            }
            if(dbData.parent==dbData) {
                System.err.printf("postPut parent = this Why???%n");
            } else {
                dbData = (AbstractDBData)dbData.parent;
            }
        }
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#getSupportName()
     */
    public String getSupportName() {
        return super.getSupportName();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVData#setSupportName(java.lang.String)
     */
    public String setSupportName(String name) {
        return super.setSupportName(name);
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
     * Called by AbstractDBRecord when DBRecord.removeListener or DBrecord.removeListeners are called.
     */
    protected void removeListeners(){
        listenerList.clear();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBData#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message, MessageType messageType) {
        record.message(getFullFieldName() + " " + message, messageType);
    }
}
