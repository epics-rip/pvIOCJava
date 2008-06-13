/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

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
class ImplDBStructure extends ImplDBField implements DBStructure
{
    private PVStructure pvStructure;
    private ImplDBField[] dbFields;
    
    /**
     * Constructor.
     * @param parent The parent.
     * @param record The DBRecord to which this field belongs.
     * @param pvStructure The reflection interface.
     */
    ImplDBStructure(ImplDBField parent,ImplDBRecord record, PVStructure pvStructure) {
        super(parent,record,pvStructure);
        this.pvStructure = pvStructure;
        createFields();
    }
    
    /**
     * Constructor for record instance classes.
     * @param dbRecord The dbRecord that contains this DBStructure.
     * @param pvRecord The PVRecord interface.
     */
    ImplDBStructure(ImplDBRecord dbRecord,PVRecord pvRecord) {
        super(null,dbRecord,pvRecord);
        pvStructure = (PVStructure)pvRecord;
        createFields();
    }
    
    /**
     * Called by ImplDBField.replacePVField.
     */
    void replacePVStructure() {
        this.pvStructure = (PVStructure)super.getPVField();
        createFields();
    }
    
    /**
     * Called when a record is created by ImplDBRecord
     */
    void replaceCreate() {
        for(int i=0; i<dbFields.length; i++) {
            ImplDBField dbField = dbFields[i];
            if(dbField==null) continue;
            dbField.replaceCreate();
        }
        super.replaceCreate();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructure#getFieldDBFields()
     */
    public DBField[] getDBFields() {
        return dbFields;
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
        dbFields = new ImplDBField[length];
        ImplDBRecord dbRecord = super.getImplDBRecord();
        for(int i=0; i<length; i++) {
            PVField pvField = pvFields[i];
            if(pvField==null) {
                throw new IllegalStateException("a structure not allowed to have a null field");
            }  
            Field field = pvField.getField();
            Type type = field.getType();
            
            ImplDBField dbField = null;
            switch(type) {
            case pvStructure:
                dbField = new ImplDBStructure(this,dbRecord,(PVStructure)pvField);
                break;
            case pvArray:
                PVArray pvArray = (PVArray)pvField;
                Type elementType = ((Array)pvArray.getField()).getElementType();
                if(elementType.isScalar()) {
                    dbField = new ImplDBField(this,dbRecord,pvArray);
                } else if(elementType==Type.pvArray){
                    dbField = new ImplDBArrayArray(this,dbRecord,(PVArrayArray)pvArray);
                } else if(elementType==Type.pvStructure){
                    dbField = new ImplDBStructureArray(this,dbRecord,(PVStructureArray)pvArray);
                } else {
                    pvField.message("logic error unknown type", MessageType.fatalError);
                    return;
                }
                break;
            default:
                dbField = new ImplDBField(this,dbRecord,pvField);
                break;
            }
            if(dbField==null) {
                throw new IllegalStateException("logic error. unknown pvType");
            }  
            dbFields[i] = dbField;
        }
    }
}
