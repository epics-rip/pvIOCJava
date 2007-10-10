/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
import org.epics.ioc.pv.*;
/**
 * Base class for a CD (Channel Data) Array of Structures.
 * @author mrk
 *
 */
public class BaseCDStructureArray extends BaseCDArray implements CDStructureArray{
    private boolean supportAlso;
    private PVStructureArray pvStructureArray;
    private CDStructure[] elementCDStructures;
    private StructureArrayData structureArrayData = new StructureArrayData();

    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvStructureArray The pvStructureArray that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDStructureArray(
        CDField parent,CDRecord cdRecord,PVStructureArray pvStructureArray,boolean supportAlso)
    {
        super(parent,cdRecord,pvStructureArray,supportAlso);
        this.supportAlso = supportAlso;
        this.pvStructureArray = pvStructureArray;
        createElementCDBStructures();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDStructures) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        if(supportAlso) {
            String supportName = targetPVField.getSupportName();
            if(supportName!=null) super.supportNamePut(targetPVField.getSupportName());
        }
        PVStructureArray targetPVStructureArray = (PVStructureArray)targetPVField;
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
                elementCDStructures[i] = new BaseCDStructure(this,cdRecord,newStructure,supportAlso);
            }
            CDStructure cdStructure = (CDStructure)elementCDStructures[i];   
            cdStructure.dataPut(targetPVStructure);
        }
        pvStructureArray.put(0, pvStructures.length, pvStructures, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#getElementCDStructures()
     */
    public CDStructure[] getElementCDStructures() {
        return elementCDStructures;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvStructureArray = (PVStructureArray)super.getPVField();
        createElementCDBStructures();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested,PVField targetPVField) {
        PVStructureArray targetPVStructureArray = (PVStructureArray)requested;
        checkPVStructureArray(targetPVStructureArray);
        int length = pvStructureArray.getLength();
        pvStructureArray.get(0, length, structureArrayData);
        targetPVStructureArray.get(0, length, structureArrayData);
        PVStructure[] targetStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            PVStructure targetStructure = targetStructures[i];
            if(cdStructure.dataPut(targetStructure, targetPVField)) return true;
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
                elementCDStructures[i] = new BaseCDStructure(this,dbRecord,pvStructure,supportAlso);
            }
        }
    }
       
    private boolean checkPVStructureArray(PVStructureArray targetPVStructureArray) {
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        boolean madeChanges = false;
        int length = targetPVStructureArray.getLength();
        if(elementCDStructures.length<length) {
            madeChanges = true;
            CDStructure[] newDatas = new CDStructure[length];
            pvStructureArray.setLength(length);
            for(int i=0;i<elementCDStructures.length; i++) {
                newDatas[i] = elementCDStructures[i];
            }
            for(int i=elementCDStructures.length; i<length; i++) {
                newDatas[i] = null;
            }
            elementCDStructures = newDatas;
        }
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
                elementCDStructures[i] = new BaseCDStructure(this,cdRecord,newStructure,supportAlso);
                CDStructure cdStructure = elementCDStructures[i];
                cdStructure.dataPut(targetPVStructure);
                cdStructure.supportNamePut(targetPVStructure.getSupportName());
            }
        }
        if(madeChanges) {
            pvStructureArray.put(0, pvStructures.length, pvStructures, 0);
        }
        return madeChanges;
    }
}
