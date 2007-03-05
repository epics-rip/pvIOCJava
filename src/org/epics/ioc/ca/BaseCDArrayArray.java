/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
import org.epics.ioc.pv.*;
/**
 * Base class for a CD (Channel Data) Array of Arrays.
 * @author mrk
 *
 */
public class BaseCDArrayArray extends BaseCDField implements CDNonScalarArray{
    private PVArrayArray pvArrayArray;
    private CDField[] elementCDFields;
    private ArrayArrayData arrayArrayData = new ArrayArrayData();
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvArrayArray The pvArrayArray that this CDField references.
     */
    public BaseCDArrayArray(
        CDField parent,CDRecord cdRecord,PVArrayArray pvArrayArray)
    {
        super(parent,cdRecord,pvArrayArray);
        this.pvArrayArray = pvArrayArray;
        createElementCDBArrays();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDFields) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)targetPVField;
        if(checkPVArrayArray(targetPVArrayArray)) {
            super.incrementNumPuts();
            return;
        }
        int length = targetPVArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetPVArray = targetArrays[i];
            if(targetPVArray==null) continue;
            CDField cdField = elementCDFields[i];
            cdField.dataPut(targetPVArray);
        }
        pvArrayArray.put(0, pvArrays.length, pvArrays, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#getElementCDFields()
     */
    public CDField[] getElementCDFields() {
        return elementCDFields;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvArrayArray = (PVArrayArray)super.getPVField();
        createElementCDBArrays();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested,PVField targetPVField) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        checkPVArrayArray(targetPVArrayArray);
        int length = pvArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            CDField cdField = elementCDFields[i];
            if(cdField.dataPut(targetArray, targetPVField)) {
                super.setMaxNumPuts(cdField.getMaxNumPuts());
                return true;
            }
        }
        return false;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#enumIndexPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumIndexPut(PVField requested,PVEnum targetPVEnum) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        checkPVArrayArray(targetPVArrayArray);
        int length = pvArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            Array array = (Array)targetArray.getField();
            Type elementType = array.getElementType();
            boolean okType = false;
            switch(elementType) {
            case pvArray:
            case pvEnum:
            case pvMenu:
            case pvStructure:
                okType = true;
                break;
            default:
                okType = false;
            }
            if(!okType) continue;
            CDField cdField = elementCDFields[i];
            if(cdField.enumIndexPut(targetArray, targetPVEnum)) return true;
        }
        return false;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#enumChoicesPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public boolean enumChoicesPut(PVField requested,PVEnum targetPVEnum) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        checkPVArrayArray(targetPVArrayArray);
        int length = pvArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            Array array = (Array)targetArray.getField();
            Type elementType = array.getElementType();
            boolean okType = false;
            switch(elementType) {
            case pvArray:
            case pvEnum:
            case pvStructure:
                okType = true;
                break;
            default:
                okType = false;
            }
            if(!okType) continue;
            CDField cdField = elementCDFields[i];
            if(cdField.enumChoicesPut(targetArray, targetPVEnum)) return true;
        }
        return false;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested,PVField targetPVField) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        checkPVArrayArray(targetPVArrayArray);
        int length = pvArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            CDField cdField = elementCDFields[i];
            if(cdField.supportNamePut(targetArray, targetPVField)) return true;
        }
        return false;
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#configurationStructurePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVLink)
     */
    public boolean configurationStructurePut(PVField requested,PVLink targetPVLink) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        checkPVArrayArray(targetPVArrayArray);
        int length = pvArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            Array array = (Array)targetArray.getField();
            Type elementType = array.getElementType();
            boolean okType = false;
            switch(elementType) {
            case pvArray:
            case pvLink:
            case pvStructure:
                okType = true;
                break;
            default:
                okType = false;
            }
            if(!okType) continue;
            CDField cdField = elementCDFields[i];
            if(cdField.configurationStructurePut(targetArray, targetPVLink)) return true;
        }
        return false;
    }
    
    private void createElementCDBArrays() {
        int length = pvArrayArray.getLength();
        elementCDFields = new CDField[length];
        CDRecord cdRecord = super.getCDRecord();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray pvArray = pvArrays[i];
            if(pvArray==null) {
                elementCDFields[i] = null;
            } else {
                Array array = (Array)pvArray.getField();
                Type elementType = array.getElementType();
                if(elementType.isScalar()) {
                    elementCDFields[i] = new BaseCDField(this,cdRecord,pvArray);
                    continue;
                }
                switch(elementType) {
                case pvArray:
                    elementCDFields[i] = new BaseCDArrayArray(this,cdRecord,(PVArrayArray)pvArray);
                    continue;
                case pvEnum:
                    elementCDFields[i] = new BaseCDEnumArray(this,cdRecord,(PVEnumArray)pvArray);
                    continue;
                case pvMenu:
                    elementCDFields[i] = new BaseCDMenuArray(this,cdRecord,(PVMenuArray)pvArray);
                    continue;
                case pvLink:
                    elementCDFields[i] = new BaseCDLinkArray(this,cdRecord,(PVLinkArray)pvArray);
                    continue;
                case pvStructure:
                    elementCDFields[i] = new BaseCDStructureArray(this,cdRecord,(PVStructureArray)pvArray);
                    continue;
                default:
                    throw new IllegalStateException("Logic error");
                }
            }
        }
    }
       
    private boolean checkPVArrayArray(PVArrayArray targetPVArrayArray) {
        boolean madeChanges = false;
        int length = targetPVArrayArray.getLength();
        if(elementCDFields.length<length) {
            madeChanges = true;
            CDField[] newDatas = new CDField[length];
            for(int i=0;i<elementCDFields.length; i++) {
                newDatas[i] = elementCDFields[i];
            }
            elementCDFields = newDatas;
        }
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetPVArray = targetArrays[i];
            if(targetPVArray==null) {
                if(pvArrays[i]!=null) {
                    madeChanges = true;
                    pvArrays[i] = null;
                    elementCDFields[i] = null;
                }
                continue;
            }
            if(elementCDFields[i]==null) {
                madeChanges = true;
                Field newField = cdRecord.createField(targetPVArray.getField());
                PVArray newArray = (PVArray)pvDataCreate.createPVField(pvArrayArray, newField);
                pvArrays[i] = newArray;
                Array array = (Array)targetPVArray.getField();
                Type elementType = array.getElementType();
                if(elementType.isScalar()) {
                    elementCDFields[i] = new BaseCDField(this,cdRecord,newArray);
                    break;
                }
                switch(elementType) {
                case pvArray:
                    elementCDFields[i] = new BaseCDArrayArray(this,cdRecord,(PVArrayArray)newArray);
                    break;
                case pvEnum:
                    elementCDFields[i] = new BaseCDEnumArray(this,cdRecord,(PVEnumArray)newArray);
                    break;
                case pvMenu:
                    elementCDFields[i] = new BaseCDMenuArray(this,cdRecord,(PVMenuArray)newArray);
                    break;
                case pvLink:
                    elementCDFields[i] = new BaseCDLinkArray(this,cdRecord,(PVLinkArray)newArray);
                    break;
                case pvStructure:
                    elementCDFields[i] = new BaseCDStructureArray(this,cdRecord,(PVStructureArray)newArray);
                    break;
                default:
                    throw new IllegalStateException("Logic error");
                }
                elementCDFields[i].dataPut(targetPVArray);
                elementCDFields[i].supportNamePut(targetPVArray);
            }
        }
        if(madeChanges) {
            pvArrayArray.put(0, pvArrays.length, pvArrays, 0);
        }
        return madeChanges;
    }
}
