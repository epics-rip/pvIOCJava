/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;
import java.lang.reflect.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Type;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.Array;
import org.epics.ioc.create.Create;

import org.epics.ioc.dbd.*;
import org.epics.ioc.util.*;

/**
 * Base class for a DBStructure.
 * @author mrk
 *
 */
public class BaseDBStructure extends BaseDBField implements DBStructure
{
    private PVStructure pvStructure;
    private DBField[] dbFields;
    
    /**
     * Constructor.
     * @param parent The parent.
     * @param record The DBRecord to which this field belongs.
     * @param pvStructure The reflection interface.
     */
    public BaseDBStructure(DBField parent,DBRecord record, PVStructure pvStructure) {
        super(parent,record,pvStructure);
        this.pvStructure = pvStructure;
        createFields();
    }
    
    /**
     * Constructor for record instance classes.
     * @param dbRecord The dbRecord that contains this DBStructure.
     * @param pvRecord The PVRecord interface.
     */
    public BaseDBStructure(DBRecord dbRecord,PVRecord pvRecord) {
        super(null,dbRecord,pvRecord);
        pvStructure = (PVStructure)pvRecord;
        createFields();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#replacePVStructure()
     */
    public void replacePVStructure() {
        this.pvStructure = (PVStructure)super.getPVField();
        createFields();
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#getFieldDBFields()
     */
    public DBField[] getFieldDBFields() {
        return dbFields;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#endPut()
     */
    public void endPut() {
        Iterator<RecordListener> iter = super.getRecordListenerList().iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            listener.getDBListener().endPut(this);
        }   
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#beginPut()
     */
    public void beginPut() {
        Iterator<RecordListener> iter = super.getRecordListenerList().iterator();
        while(iter.hasNext()) {
            RecordListener listener = iter.next();
            listener.getDBListener().beginPut(this);
        }   
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#getPVStructure()
     */
    public PVStructure getPVStructure() {
        return (PVStructure)super.getPVField();
    }
    
    private void createFields() {
        PVField[] pvFields = pvStructure.getPVFields();
        int length = pvFields.length;
        dbFields = new DBField[length];
        for(int i=0; i<length; i++) {
            PVField pvField = pvFields[i];
            Field field = pvField.getField();
            Type type = field.getType();
            DBRecord dbRecord = super.getDBRecord();
            BaseDBField dbField = null;
            switch(type) {
            case pvStructure:
                dbField = new BaseDBStructure(this,dbRecord,(PVStructure)pvField);
                break;
            case pvArray:
                PVArray pvArray = (PVArray)pvField;
                Type elementType = ((Array)pvArray.getField()).getElementType();
                if(elementType.isScalar()) {
                    dbField = new BaseDBField(this,dbRecord,pvArray);
                } else if(elementType==Type.pvArray){
                    dbField = new BaseDBArrayArray(this,dbRecord,(PVArrayArray)pvArray);
                } else if(elementType==Type.pvStructure){
                    dbField = new BaseDBStructureArray(this,dbRecord,(PVStructureArray)pvArray);
                } else {
                    pvField.message("logic error unknown type", MessageType.fatalError);
                    return;
                }
                break;
            default:
                dbField = new BaseDBField(this,dbRecord,pvField);
                break;
            }
            dbFields[i] = dbField;
            String message = callCreate(dbField);
            if(message!=null) pvField.message(message, MessageType.error);
        }
    }
    
    private String callCreate(DBField dbField) {
        PVField pvField = dbField.getPVField();
        Field field = pvField.getField();
        String createName = field.getCreateName();
        if(createName==null) return null;
        DBD dbd = super.getDBRecord().getDBD();
        if(dbd==null) dbd = DBDFactory.getMasterDBD();
        DBDCreate dbdCreate = dbd.getCreate(createName);
        if(dbdCreate==null) {
            return "create " + createName + " not found";
        }
        String factoryName = dbdCreate.getFactoryName();
        Class supportClass;
        Method method = null;
        Create create = null;
        try {
            supportClass = Class.forName(factoryName);
        }catch (ClassNotFoundException e) {
            return "factory " + factoryName 
            + " " + e.getLocalizedMessage();
        }
        try {
            method = supportClass.getDeclaredMethod("create",
                    Class.forName("org.epics.ioc.db.DBField"));
            
        } catch (NoSuchMethodException e) {

        } catch (ClassNotFoundException e) {
            return "factory " + factoryName 
            + " " + e.getLocalizedMessage();
        }
        if(!Modifier.isStatic(method.getModifiers())) {
            return "factory " + factoryName 
            + " create is not a static method ";
        }
        try {
            create = (Create)method.invoke(null,dbField);
            if(create!=null) super.setCreate(create);
            return null;
        } catch(IllegalAccessException e) {
            return "factory " + factoryName 
            + " " + e.getLocalizedMessage();
        } catch(IllegalArgumentException e) {
            return "factory " + factoryName 
            + " " + e.getLocalizedMessage();
        } catch(InvocationTargetException e) {
            return "factory " + factoryName 
            + " " + e.getLocalizedMessage();
        }
    }
    
}
