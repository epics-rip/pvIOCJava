/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.ArrayArrayData;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVArrayArray;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.Type;

/**
 * Implementation of a DBArrayArray.
 * It has package visibility.
 * @author mrk
 *
 */
class ImplDBArrayArray extends ImplDBArray implements DBArrayArray {
    private PVArrayArray pvArrayArray;
    private ImplDBArray[] elementDBArrays;
    
    /**
     * Constructor.
     * @param parent The parent BaseDBField.
     * @param record The ImplDBRecord to which this field belongs.
     * @param pvArrayArray The pvArrayArray interface.
     */
    ImplDBArrayArray(ImplDBField parent,ImplDBRecord record, PVArrayArray pvArrayArray) {
        super(parent,record,pvArrayArray);
        this.pvArrayArray = pvArrayArray;
        createElementDBArrays();
        
    }
    
    /**
     * Called by ImplDBField.
     */
    void replacePVArray() {
        pvArrayArray = (PVArrayArray)super.getPVField();
        createElementDBArrays();
    }
    /**
     * Called when a record is created by ImplDBRecord
     */
    void replaceCreate() {
        for(ImplDBArray dbArray: elementDBArrays) {
            if(dbArray==null) continue;
            dbArray.replaceCreate();
        }
        super.replaceCreate();
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
    
    private void createElementDBArrays() {
        Type elementType = pvArrayArray.getArray().getElementType();
        if(elementType!=Type.pvArray) {
            throw new IllegalStateException("elementType is not pvArrayArray");
        }
        int length = pvArrayArray.getLength();
        elementDBArrays = new ImplDBArray[length];
        ArrayArrayData arrayArrayData = new ArrayArrayData();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        ImplDBRecord dbRecord = super.getImplDBRecord();
        for(int i=0; i<length; i++) {
            PVArray elementArray = pvArrays[i];
            if(elementArray==null) {
                elementDBArrays[i] = null;
            } else {
                Type elementArrayType = elementArray.getArray().getElementType();
                if(elementArrayType.isScalar()) {
                    elementDBArrays[i] = new ImplDBArray(this,dbRecord,elementArray);
                } else if(elementArrayType==Type.pvArray) {
                    elementDBArrays[i] = new ImplDBArrayArray(this,dbRecord,(PVArrayArray)elementArray);
                } else if(elementArrayType==Type.pvStructure) {
                    elementDBArrays[i] = new ImplDBStructureArray(this,dbRecord,(PVStructureArray)elementArray);
                }
            }
        }
    }
}
