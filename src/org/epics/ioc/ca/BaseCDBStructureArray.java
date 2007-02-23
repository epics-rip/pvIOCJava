/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
import org.epics.ioc.pv.*;
/**
 * @author mrk
 *
 */
public class BaseCDBStructureArray extends BaseCDBData implements CDBStructureArray{
    private PVStructureArray pvStructureArray;
    private CDBStructure[] elementCDBStructures;
    
    public BaseCDBStructureArray(
            CDBData parent,CDBRecord cdbRecord,PVStructureArray pvStructureArray)
        {
            super(parent,cdbRecord,pvStructureArray);
            this.pvStructureArray = pvStructureArray;
            createElementCDBStructures();
        }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBStructureArray#dataPut(org.epics.ioc.pv.PVStructureArray)
     */
    public void dataPut(PVStructureArray targetPVStructureArray) {
        int length = targetPVStructureArray.getLength();
        if(elementCDBStructures.length<length) {
            CDBStructure[] newDatas = new CDBStructure[length];
            for(int i=0;i<elementCDBStructures.length; i++) {
                newDatas[i] = elementCDBStructures[i];
            }
            elementCDBStructures = newDatas;
        }
        CDBRecord cdbRecord = super.getCDBRecord();
        PVDataCreate pvDataCreate = cdbRecord.getPVDataCreate();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure targetPVStructure = targetStructures[i];
            if(targetPVStructure==null) {
                elementCDBStructures[i] = null;
                continue;
            }
            if(elementCDBStructures[i]==null) {
                Field newField = cdbRecord.createField(targetPVStructure.getField());
                PVStructure newStructure = (PVStructure)pvDataCreate.createData(pvStructureArray, newField);
                pvStructures[i] = newStructure;
                elementCDBStructures[i] = new BaseCDBStructure(this,cdbRecord,newStructure);
            }
            CDBStructure cdbStructure = (CDBStructure)elementCDBStructures[i];   
            cdbStructure.dataPut(targetPVStructure);
            super.setNumPuts(cdbStructure.getNumPuts());
        }
        pvStructureArray.put(0, pvStructures.length, pvStructures, 0);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBStructureArray#getElementCDBStructures()
     */
    public CDBStructure[] getElementCDBStructures() {
        return elementCDBStructures;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBStructureArray#replacePVArray()
     */
    public void replacePVStructureArray() {
        pvStructureArray = (PVStructureArray)super.getPVData();
        createElementCDBStructures();
    }

    public int dataPut(PVData requested,PVData targetPVData) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        int length = pvStructureArray.getLength();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDBStructure cdbStructure = elementCDBStructures[i];
            PVStructure targetStructure = targetStructures[i];
            int result = cdbStructure.dataPut(targetStructure, targetPVData);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int enumIndexPut(PVData requested,PVEnum targetPVEnum) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        int length = pvStructureArray.getLength();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDBStructure cdbStructure = elementCDBStructures[i];
            PVStructure targetStructure = targetStructures[i];
            int result = cdbStructure.enumIndexPut(targetStructure, targetPVEnum);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int enumChoicesPut(PVData requested,PVEnum targetPVEnum) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        int length = pvStructureArray.getLength();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDBStructure cdbStructure = elementCDBStructures[i];
            PVStructure targetStructure = targetStructures[i];
            int result = cdbStructure.enumChoicesPut(targetStructure, targetPVEnum);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int supportNamePut(PVData requested,PVData targetPVData) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        int length = pvStructureArray.getLength();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDBStructure cdbStructure = elementCDBStructures[i];
            PVStructure targetStructure = targetStructures[i];
            int result = cdbStructure.supportNamePut(targetStructure, targetPVData);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int configurationStructurePut(PVData requested,PVLink targetPVLink) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        int length = pvStructureArray.getLength();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDBStructure cdbStructure = elementCDBStructures[i];
            PVStructure targetStructure = targetStructures[i];
            int result = cdbStructure.configurationStructurePut(targetStructure, targetPVLink);
            if(result>0) return result;
        }
        return 0;
    }
    
    private void createElementCDBStructures() {
        int length = pvStructureArray.getLength();
        elementCDBStructures = new CDBStructure[length];
        CDBRecord dbRecord = super.getCDBRecord();
        StructureArrayData structureArrayData = new StructureArrayData();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure pvStructure = pvStructures[i];
            if(pvStructure==null) {
                elementCDBStructures[i] = null;
            } else {
                elementCDBStructures[i] = new BaseCDBStructure(this,dbRecord,pvStructure);
            }
        }
    }
}
