/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.epics.ioc.create.Create;
import org.epics.ioc.dbd.DBD;
import org.epics.ioc.dbd.DBDCreate;
import org.epics.ioc.dbd.DBDFactory;
import org.epics.ioc.dbd.DBDSupport;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.MessageType;


/**
 * Implementation of a DBField.
 * It has package visibility.
 * It is a base class. For scalar fields it is the complete implementation.
 * Support for other fields derive from this class.
 * @author mrk
 *
 */
class ImplDBField implements DBField{
    private ImplDBField parent;
    private ImplDBRecord dbRecord;
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
    ImplDBField(ImplDBField parent,ImplDBRecord record, PVField pvField) {
        this.parent = parent;
        this.dbRecord = record;
        this.pvField = pvField;
    }
    
    ImplDBRecord getImplDBRecord() {
        return dbRecord;
    }
    /**
     * Called when a record is created by ImplDBRecord
     */
    void replaceCreate() {
        String createName = pvField.getCreateName();
        if(createName==null) {
            Field field = pvField.getField();
            createName = field.getCreateName();
        }
        if(createName==null) return;
        DBD dbd = dbRecord.getDBD();
        if(dbd==null) dbd = DBDFactory.getMasterDBD();
        DBDCreate dbdCreate = dbd.getCreate(createName);
        if(dbdCreate==null) return;
        String factoryName = dbdCreate.getFactoryName();
        Class supportClass;
        Method method = null;
        Create create = null;
        try {
            supportClass = Class.forName(factoryName);
        }catch (ClassNotFoundException e) {
           message("factory " + factoryName 
            + " " + e.getLocalizedMessage());
           return;
        }
        try {
            method = supportClass.getDeclaredMethod("create",
                    Class.forName("org.epics.ioc.db.DBField"));
            
        } catch (NoSuchMethodException e) {
            message("factory " + factoryName 
                    + " " + e.getLocalizedMessage());
                    return;
        } catch (ClassNotFoundException e) {
            message("factory " + factoryName 
            + " " + e.getLocalizedMessage());
            return;
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            message("factory " + factoryName 
            + " create is not a static method ");
            return;
        }
        try {
            create = (Create)method.invoke(null,this);
            this.create = create;
            return;
        } catch(IllegalAccessException e) {
            message("factory " + factoryName 
            + " " + e.getLocalizedMessage());
            return;
        } catch(IllegalArgumentException e) {
            message("factory " + factoryName 
            + " " + e.getLocalizedMessage());
            return;
        } catch(InvocationTargetException e) {
            message("factory " + factoryName 
            + " " + e.getLocalizedMessage());
        }
    }
    
    protected ImplDBField getParentDBField() {
        return parent;
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
            ImplDBStructure dbStructure = (ImplDBStructure)this;
            dbStructure.replacePVStructure();
        } else if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(elementType==Type.pvArray) {
                ImplDBArrayArray dbArrayArray = (ImplDBArrayArray)this;
                dbArrayArray.replacePVArray();
            } else if(elementType==Type.pvStructure) {
                ImplDBStructureArray dbStructureArray = (ImplDBStructureArray)this;
                dbStructureArray.replacePVArray();
            } 
        }
        recordListenerList.clear();
        dbRecord.removeRecordListeners();
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
        postChildren(this);
        ImplDBField parentDBField = parent;
        while(parentDBField!=null) {
            iter = parentDBField.getRecordListenerList().iterator();
            while(iter.hasNext()) {
                RecordListener listener = iter.next();
                DBListener dbListener = listener.getDBListener();
                dbListener.dataPut(parentDBField, this);
            }
            parentDBField = parentDBField.getParentDBField();
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#addListener(org.epics.ioc.db.RecordListener)
     */
    public boolean addListener(RecordListener recordListener) {
        if(recordListenerList.isEmpty()) {
            // return is ignored. OK if already on recordListenerList
            dbRecord.addListenerSource(this);
        }
        if(recordListenerList.contains(recordListener)) return false;
        recordListenerList.add(recordListener);
        return true;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBField#removeListener(org.epics.ioc.db.RecordListener)
     */
    public void removeListener(RecordListener recordListener) {
        recordListenerList.remove(recordListener);
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
    
    private List<RecordListener> getRecordListenerList() {
        return recordListenerList;
    }
    
    private void message(String message) {
        pvField.message(message, MessageType.error);
    }
    
    private void postChildren(DBField dbField) {
        // must not be array of array or structure and
        // no subfield can be an array of array or structure.
        PVField pvField = dbField.getPVField();
        Field field = pvField.getField();
        Type type = field.getType();
        if(type.isScalar()) return;
        if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) badChild(dbField);
            return;
        }
        if(type!=Type.pvStructure) {
            badChild(dbField);
            return;
        }
        DBStructure dbStructure = (DBStructure)dbField;
        DBField[] childDBFields = dbStructure.getDBFields();
        for(DBField childDBField : childDBFields) {
            PVField childPVField = childDBField.getPVField();
            Field childField = childPVField.getField();
            Type childType = childField.getType();
            if(childType.isScalar()) {
                postChild(childDBField);
            } else if(childType==Type.pvArray) {
                Array childArray = (Array)childField;
                if(childArray.getElementType().isScalar()) {
                    postChild(childDBField);
                } else {
                    badChild(childDBField);
                }
            } else if(childType==Type.pvStructure){
                postChild(childDBField);
                postChildren(childDBField);
            } else {
                throw new IllegalStateException("logic error. unknown pvType");
            }
        }
    }
    
    private void postChild(DBField dbField) {
        Iterator<RecordListener> iter;
        iter = ((ImplDBField)dbField).getRecordListenerList().iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            DBListener dbListener = listener.getDBListener();
            dbListener.dataPut(dbField);
        }
    }
    private void badChild(DBField dbField) {
        String childName = dbField.getPVField().getFullFieldName();
        String postName = this.pvField.getFullName();
        message(postName +
            " postPut was issued but field "
            + childName + " has illegal type for postPut ");
    }
}
