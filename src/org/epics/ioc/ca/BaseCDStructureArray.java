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
public class BaseCDStructureArray extends BaseCDField implements CDStructureArray{
    private PVStructureArray pvStructureArray;
    private CDStructure[] elementCDStructures;
    private StructureArrayData structureArrayData = new StructureArrayData();
    
    /**
     * @param parent
     * @param cdRecord
     * @param pvStructureArray
     */
    public BaseCDStructureArray(
            CDField parent,CDRecord cdRecord,PVStructureArray pvStructureArray)
        {
            super(parent,cdRecord,pvStructureArray);
            this.pvStructureArray = pvStructureArray;
            createElementCDBStructures();
        }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    @Override
    public void clearNumPuts() {
        for(CDField cdField : elementCDStructures) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#dataPut(org.epics.ioc.pv.PVStructureArray)
     */
    public void dataPut(PVStructureArray targetPVStructureArray) {
        if(checkPVStructureArray(targetPVStructureArray)) {
            super.incrementNumPuts();
            return;
        }
        int length = targetPVStructureArray.getLength();
        if(elementCDStructures.length<length) {
            CDStructure[] newDatas = new CDStructure[length];
            for(int i=0;i<elementCDStructures.length; i++) {
                newDatas[i] = elementCDStructures[i];
            }
            elementCDStructures = newDatas;
        }
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure targetPVStructure = targetStructures[i];
            if(targetPVStructure==null) {
                elementCDStructures[i] = null;
                continue;
            }
            if(elementCDStructures[i]==null) {
                Field newField = cdRecord.createField(targetPVStructure.getField());
                PVStructure newStructure = (PVStructure)pvDataCreate.createPVField(pvStructureArray, newField);
                pvStructures[i] = newStructure;
                elementCDStructures[i] = new BaseCDStructure(this,cdRecord,newStructure);
            }
            CDStructure cdStructure = (CDStructure)elementCDStructures[i];   
            cdStructure.fieldPut(targetPVStructure);
        }
        pvStructureArray.put(0, pvStructures.length, pvStructures, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#getElementCDBStructures()
     */
    public CDStructure[] getElementCDStructures() {
        return elementCDStructures;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#replacePVArray()
     */
    public void replacePVStructureArray() {
        pvStructureArray = (PVStructureArray)super.getPVField();
        createElementCDBStructures();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean fieldPut(PVField requested,PVField targetPVField) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        checkPVStructureArray(targetPVStructureArray);
        int length = pvStructureArray.getLength();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            PVStructure targetStructure = targetStructures[i];
            if(cdStructure.fieldPut(targetStructure, targetPVField)) return true;
        }
        return false;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#enumIndexPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumIndexPut(PVField requested,PVEnum targetPVEnum) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        checkPVStructureArray(targetPVStructureArray);
        int length = pvStructureArray.getLength();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            PVStructure targetStructure = targetStructures[i];
            if(cdStructure.enumIndexPut(targetStructure, targetPVEnum)) return true;
        }
        return false;
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#enumChoicesPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumChoicesPut(PVField requested,PVEnum targetPVEnum) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        checkPVStructureArray(targetPVStructureArray);
        int length = pvStructureArray.getLength();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            PVStructure targetStructure = targetStructures[i];
            if(cdStructure.enumChoicesPut(targetStructure, targetPVEnum)) return true;
        }
        return false;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested,PVField targetPVField) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        checkPVStructureArray(targetPVStructureArray);
        int length = pvStructureArray.getLength();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            PVStructure targetStructure = targetStructures[i];
            if(cdStructure.supportNamePut(targetStructure, targetPVField)) return true;
        }
        return false;
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#configurationStructurePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVLink)
     */
    public boolean configurationStructurePut(PVField requested,PVLink targetPVLink) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        checkPVStructureArray(targetPVStructureArray);
        int length = pvStructureArray.getLength();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            PVStructure targetStructure = targetStructures[i];
            if(cdStructure.configurationStructurePut(targetStructure, targetPVLink)) return true;
        }
        return false;
    }
    
    private void createElementCDBStructures() {
        int length = pvStructureArray.getLength();
        elementCDStructures = new CDStructure[length];
        CDRecord dbRecord = super.getCDRecord();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure pvStructure = pvStructures[i];
            if(pvStructure==null) {
                elementCDStructures[i] = null;
            } else {
                elementCDStructures[i] = new BaseCDStructure(this,dbRecord,pvStructure);
            }
        }
    }
       
    private boolean checkPVStructureArray(PVStructureArray targetPVStructureArray) {
        boolean madeChanges = false;
        int length = targetPVStructureArray.getLength();
        if(elementCDStructures.length<length) {
            madeChanges = true;
            CDStructure[] newDatas = new CDStructure[length];
            for(int i=0;i<elementCDStructures.length; i++) {
                newDatas[i] = elementCDStructures[i];
            }
            elementCDStructures = newDatas;
        }
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure targetPVStructure = targetStructures[i];
            if(targetPVStructure==null) {
                if(pvStructures[i]!=null) {
                    madeChanges = true;
                    pvStructures[i] = null;
                    elementCDStructures[i] = null;
                }
                continue;
            }
            if(elementCDStructures[i]==null) {
                madeChanges = true;
                Field newField = cdRecord.createField(targetPVStructure.getField());
                PVStructure newStructure = (PVStructure)pvDataCreate.createPVField(pvStructureArray, newField);
                pvStructures[i] = newStructure;
                elementCDStructures[i] = new BaseCDStructure(this,cdRecord,newStructure);
                CDStructure cdStructure = elementCDStructures[i];
                cdStructure.fieldPut(targetPVStructure);
                cdStructure.supportNamePut(targetPVStructure);
            }
        }
        if(madeChanges) {
            pvStructureArray.put(0, pvStructures.length, pvStructures, 0);
        }
        return madeChanges;
    }
}
