/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.StructureArrayData;
import org.epics.ioc.pv.Type;

/**
 * Base class for a non scalar array.
 * @author mrk
 *
 */
class ImplDBStructureArray extends ImplDBArray implements DBStructureArray {
    private PVStructureArray pvStructureArray;
    private ImplDBStructure[] elementDBStructures;
    
    /**
     * Constructor.
     * @param parent The parent ImplDBField.
     * @param record The ImplDBRecord to which this field belongs.
     * @param pvStructureArray The pvStructureArray interface.
     */
    ImplDBStructureArray(ImplDBField parent,ImplDBRecord record, PVStructureArray pvStructureArray) {
        super(parent,record,pvStructureArray);
        this.pvStructureArray = pvStructureArray;
        createElementDBStructures();
        
    }
    
    /**
     * Called by ImplField
     */
    void replacePVArray() {
        pvStructureArray = (PVStructureArray)super.getPVField();
        createElementDBStructures();
    }
    /**
     * Called when a record is created by ImplDBRecord
     */
    void replaceCreate() {
        for(ImplDBStructure dbStructure: elementDBStructures) {
            if(dbStructure==null) continue;
            dbStructure.replaceCreate();
        }
        super.replaceCreate();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructureArray#getPVStructureArray()
     */
    public PVStructureArray getPVStructureArray() {
        return pvStructureArray;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBStructureArray#getElementDBStructures()
     */
    public DBStructure[] getElementDBStructures() {
        return elementDBStructures;
    }
     
    private void createElementDBStructures() {
        Type elementType = ((Array)pvStructureArray.getField()).getElementType();
        if(elementType!=Type.pvStructure) {
            throw new IllegalStateException("elementType is not pvStructureArray");
        }       
        int length = pvStructureArray.getLength();
        elementDBStructures = new ImplDBStructure[length];
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        ImplDBRecord dbRecord = super.getImplDBRecord();
        for(int i=0; i<length; i++) {
            PVStructure pvStructure = pvStructures[i];
            if(pvStructure==null) {
                elementDBStructures[i] = null;
            } else {
                elementDBStructures[i] = new ImplDBStructure(this,dbRecord,pvStructure);
            }
        }
        return;
    }
}
