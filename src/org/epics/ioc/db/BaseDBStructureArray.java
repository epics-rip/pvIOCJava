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
public class BaseDBStructureArray extends BaseDBArray implements DBStructureArray {
    private PVStructureArray pvStructureArray;
    private DBStructure[] elementDBStructures;
    
    /**
     * Constructor.
     * @param parent The parent DBField.
     * @param record The DBRecord to which this field belongs.
     * @param pvStructureArray The pvStructureArray interface.
     */
    public BaseDBStructureArray(DBField parent,DBRecord record, PVStructureArray pvStructureArray) {
        super(parent,record,pvStructureArray);
        this.pvStructureArray = pvStructureArray;
        createElementDBStructures();
        
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
    /* (non-Javadoc)
     * @see org.epics.ioc.db.DBNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvStructureArray = (PVStructureArray)super.getPVField();
        createElementDBStructures();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.db.BaseDBField#postPut()
     */
    public void postPut() {
        createElementDBStructures();
        super.postPut();
    }
    
    private void createElementDBStructures() {
        int length = pvStructureArray.getLength();
        elementDBStructures = new DBStructure[length];
        Type elementType = ((Array)pvStructureArray.getField()).getElementType();
        if(elementType!=Type.pvStructure) {
            throw new IllegalStateException("elementType is not pvStructureArray");
        }       
        DBRecord dbRecord = super.getDBRecord();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure pvStructure = pvStructures[i];
            if(pvStructure==null) {
                elementDBStructures[i] = null;
            } else {
                elementDBStructures[i] = new BaseDBStructure(this,dbRecord,pvStructure);
            }
        }
        return;
    }
}
