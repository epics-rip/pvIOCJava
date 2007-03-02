/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import java.util.Iterator;

import org.epics.ioc.pv.*;

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
     * @param pvField The reflection interface.
     */
    public BaseDBStructure(DBField parent,DBRecord record, PVField pvField) {
        super(parent,record,pvField);
        this.pvStructure = (PVStructure)pvField;
        createFields();
    }
    
    /**
     * Constructor for record instance classes.
     * @param dbdRecordType The reflection interface for the record type.
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
        PVField[] pvFields = pvStructure.getFieldPVFields();
        int length = pvFields.length;
        dbFields = new DBField[length];
        for(int i=0; i<length; i++) {
            PVField pvField = pvFields[i];
            Type type = pvField.getField().getType();
            DBRecord dbRecord = super.getDBRecord();
            BaseDBField dbField = null;
            switch(type) {
            case pvEnum:
                dbField = new BaseDBEnum(this,dbRecord,pvField);
                break;
            case pvMenu:
                dbField = new BaseDBMenu(this,dbRecord,pvField);
                break;
            case pvLink:
                dbField = new BaseDBLink(this,dbRecord,pvField);
                break;
            case pvStructure:
                dbField = new BaseDBStructure(this,dbRecord,pvField);
                break;
            case pvArray:
                PVArray pvArray = (PVArray)pvField;
                if(((Array)pvArray.getField()).getElementType().isScalar()) {
                    dbField = new BaseDBField(this,dbRecord,pvArray);
                } else {
                    dbField = new BaseDBNonScalarArray(this,dbRecord,pvArray);
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
