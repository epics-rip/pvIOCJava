/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * Base class for a non scalar array.
 * @author mrk
 *
 */
public class BaseDBNonScalarArray extends BaseDBField implements DBNonScalarArray {
    private PVArray pvArray;
    private DBField[] elementDBFields;
    
    /**
     * Constructor.
     * @param parent The parent DBField.
     * @param record The DBRecord to which this field belongs.
     * @param pvArray The pvArray interface.
     */
    public BaseDBNonScalarArray(DBField parent,DBRecord record, PVArray pvArray) {
        super(parent,record,pvArray);
        this.pvArray = pvArray;
        createElementDBFields();
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBNonScalarArray#getElementDBFields()
     */
    public DBField[] getElementDBFields() {
        return elementDBFields;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvArray = (PVArray)super.getPVField();
        createElementDBFields();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBField#postPut()
     */
    public void postPut() {
        createElementDBFields();
        super.postPut();
    }
    
    private void createElementDBFields() {
        int length = pvArray.getLength();
        elementDBFields = new DBField[length];
        Type elementType = ((Array)pvArray.getField()).getElementType();
        DBRecord dbRecord = super.getDBRecord();
        switch(elementType) {
        case pvStructure:
            PVStructureArray pvStructureArray = (PVStructureArray)pvArray;
            StructureArrayData structureArrayData = new StructureArrayData();
            pvStructureArray.get(0, length, structureArrayData);
            PVStructure[] pvStructures = structureArrayData.data;
            for(int i=0; i<length; i++) {
                PVStructure pvStructure = pvStructures[i];
                if(pvStructure==null) {
                    elementDBFields[i] = null;
                } else {
                    elementDBFields[i] = new BaseDBStructure(this,dbRecord,pvStructure);
                }
            }
            return;
        case pvArray:
            PVArrayArray pvArrayArray = (PVArrayArray)pvArray;
            ArrayArrayData arrayArrayData = new ArrayArrayData();
            pvArrayArray.get(0, length, arrayArrayData);
            PVArray[] pvArrays = arrayArrayData.data;
            for(int i=0; i<length; i++) {
                PVArray elementArray = pvArrays[i];
                if(elementArray==null) {
                    elementDBFields[i] = null;
                } else {
                    if(elementArray.getArray().getElementType().isScalar()) {
                        elementDBFields[i] = new BaseDBField(this,dbRecord,elementArray);
                    } else {
                        elementDBFields[i] = new BaseDBNonScalarArray(this,dbRecord,elementArray);
                    }
                }
            }
            return;
        default: throw new IllegalStateException("not valid for scalars");
        }
    }
}
