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
public class BaseDBArrayArray extends BaseDBArray implements DBArrayArray {
    private PVArrayArray pvArrayArray;
    private DBArray[] elementDBArrays;
    
    /**
     * Constructor.
     * @param parent The parent DBField.
     * @param record The DBRecord to which this field belongs.
     * @param pvArrayArray The pvArrayArray interface.
     */
    public BaseDBArrayArray(DBField parent,DBRecord record, PVArrayArray pvArrayArray) {
        super(parent,record,pvArrayArray);
        this.pvArrayArray = pvArrayArray;
        createElementDBArrays();
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBArrayArray#getPVArrayArray()
     */
    public PVArrayArray getPVArrayArray() {
        return pvArrayArray;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBArrayArray#getElementDBArrays()
     */
    public DBArray[] getElementDBArrays() {
        return elementDBArrays;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvArrayArray = (PVArrayArray)super.getPVField();
        createElementDBArrays();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBField#postPut()
     */
    public void postPut() {
        createElementDBArrays();
        super.postPut();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBField#postPut(org.epics.ioc.db.DBField)
     */
    @Override
    public void postPut(DBField dbField) {
        for(DBField dbF : elementDBArrays) {
            dbF.postPut(dbField);
        }
    }
    
    private void createElementDBArrays() {
        int length = pvArrayArray.getLength();
        elementDBArrays = new DBArray[length];
        Type elementType = ((Array)pvArrayArray.getField()).getElementType();
        DBRecord dbRecord = super.getDBRecord();
        if(elementType!=Type.pvArray) {
            throw new IllegalStateException("elemenType is not pvArrayArray");
        }       
        ArrayArrayData arrayArrayData = new ArrayArrayData();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray elementArray = pvArrays[i];
            if(elementArray==null) {
                elementDBArrays[i] = null;
            } else {
                Type elementArrayType = elementArray.getArray().getElementType();
                if(elementArrayType.isScalar()) {
                    elementDBArrays[i] = new BaseDBArray(this,dbRecord,elementArray);
                } else if(elementArrayType==Type.pvArray) {
                    elementDBArrays[i] = new BaseDBArrayArray(this,dbRecord,(PVArrayArray)elementArray);
                } else if(elementArrayType==Type.pvStructure) {
                    elementDBArrays[i] = new BaseDBStructureArray(this,dbRecord,(PVStructureArray)elementArray);
                }
            }
        }
    }
}
