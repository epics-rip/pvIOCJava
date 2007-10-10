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
public class BaseCDArrayArray extends BaseCDArray implements CDArrayArray{
    private boolean supportAlso;
    private PVArrayArray pvArrayArray;
    private CDArray[] elementCDArrays;
    private ArrayArrayData arrayArrayData = new ArrayArrayData();
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvArrayArray The pvArrayArray that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDArrayArray(
        CDField parent,CDRecord cdRecord,PVArrayArray pvArrayArray,boolean supportAlso)
    {
        super(parent,cdRecord,pvArrayArray,supportAlso);
        this.supportAlso = supportAlso;
        this.pvArrayArray = pvArrayArray;
        createElementCDBArrays();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDArrays) cdField.clearNumPuts();
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
            CDField cdField = elementCDArrays[i];
            cdField.dataPut(targetPVArray);
        }
        pvArrayArray.put(0, pvArrays.length, pvArrays, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDArrayArray#getElementCDArrays()
     */
    public CDArray[] getElementCDArrays() {
        return elementCDArrays;
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
            CDField cdField = elementCDArrays[i];
            if(cdField.dataPut(targetArray, targetPVField)) {
                super.setMaxNumPuts(cdField.getMaxNumPuts());
                return true;
            }
        }
        return false;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested,PVField targetPVField) {
        if(!supportAlso) return false;
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        checkPVArrayArray(targetPVArrayArray);
        int length = pvArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            CDField cdField = elementCDArrays[i];
            if(cdField.supportNamePut(targetArray, targetPVField)) return true;
        }
        return false;
    }  
    
    private void createElementCDBArrays() {
        int length = pvArrayArray.getLength();
        elementCDArrays = new CDArray[length];
        CDRecord cdRecord = super.getCDRecord();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray pvArray = pvArrays[i];
            if(pvArray==null) {
                elementCDArrays[i] = null;
            } else {
                Array array = (Array)pvArray.getField();
                Type elementType = array.getElementType();
                if(elementType.isScalar()) {
                    elementCDArrays[i] = new BaseCDArray(this,cdRecord,pvArray,supportAlso);
                    continue;
                }
                switch(elementType) {
                case pvArray:
                    elementCDArrays[i] = new BaseCDArrayArray(this,cdRecord,(PVArrayArray)pvArray,supportAlso);
                    continue;
                case pvStructure:
                    elementCDArrays[i] = new BaseCDStructureArray(this,cdRecord,(PVStructureArray)pvArray,supportAlso);
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
        if(elementCDArrays.length<length) {
            madeChanges = true;
            CDArray[] newDatas = new CDArray[length];
            for(int i=0;i<elementCDArrays.length; i++) {
                newDatas[i] = elementCDArrays[i];
            }
            elementCDArrays = newDatas;
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
                    elementCDArrays[i] = null;
                }
                continue;
            }
            if(elementCDArrays[i]==null) {
                madeChanges = true;
                Field newField = cdRecord.createField(targetPVArray.getField());
                PVArray newArray = (PVArray)pvDataCreate.createPVField(pvArrayArray, newField);
                pvArrays[i] = newArray;
                Array array = (Array)targetPVArray.getField();
                Type elementType = array.getElementType();
                if(elementType.isScalar()) {
                    elementCDArrays[i] = new BaseCDArray(this,cdRecord,newArray,supportAlso);
                    break;
                }
                switch(elementType) {
                case pvArray:
                    elementCDArrays[i] = new BaseCDArrayArray(this,cdRecord,(PVArrayArray)newArray,supportAlso);
                    break;
                case pvStructure:
                    elementCDArrays[i] = new BaseCDStructureArray(this,cdRecord,(PVStructureArray)newArray,supportAlso);
                    break;
                default:
                    throw new IllegalStateException("Logic error");
                }
                elementCDArrays[i].dataPut(targetPVArray);
                elementCDArrays[i].supportNamePut(targetPVArray.getSupportName());
            }
        }
        if(madeChanges) {
            pvArrayArray.put(0, pvArrays.length, pvArrays, 0);
        }
        return madeChanges;
    }
}
