/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVArrayArray;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;

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
    public DBField[] getDBFields() {
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
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBField#postPut()
     */
    @Override
    public void postPut() {
        for(DBField dbField : dbFields) {
            BaseDBField baseDBField = (BaseDBField)dbField;
            baseDBField.postPut(this);
        }
        super.postPut();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBField#postPut(org.epics.ioc.db.DBField)
     */
    @Override
    public void postPut(DBField dbField) {
        for(DBField dbF : dbFields) {
            dbF.postPut(dbField);
           
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBField#replaceCreate()
     */
    public void replaceCreate() {
        PVField[] pvFields = pvStructure.getPVFields();
        int length = pvFields.length;
        for(int i=0; i<length; i++) {
            DBField dbField = dbFields[i];
            if(dbField==null) continue;
            dbField.replaceCreate();
        }
        super.replaceCreate();
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
        }
    }
}
