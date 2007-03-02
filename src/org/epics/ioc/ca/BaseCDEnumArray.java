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
public class BaseCDEnumArray extends BaseCDField implements CDEnumArray {
    private PVEnumArray pvEnumArray;
    private CDEnum[] elementCDEnums;
    private EnumArrayData enumArrayData = new EnumArrayData();
    
    public BaseCDEnumArray(
        CDField parent,CDRecord cdRecord,PVEnumArray pvEnumArray)
    {
        super(parent,cdRecord,pvEnumArray);
        this.pvEnumArray = pvEnumArray;
        createElementCDBEnums();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    @Override
    public void clearNumPuts() {
        for(CDField cdField : elementCDEnums) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnumArray#dataPut(org.epics.ioc.pv.PVEnumArray)
     */
    public void dataPut(PVEnumArray targetPVEnumArray) {
        if(checkPVEnumArray(targetPVEnumArray)) {
            super.incrementNumPuts();
            return;
        }
        int length = targetPVEnumArray.getLength();
        pvEnumArray.get(0, length, enumArrayData);
        PVEnum[] pvEnums = enumArrayData.data;
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetPVEnum = targetEnums[i];
            if(targetPVEnum==null) continue;
            CDEnum cdEnum = elementCDEnums[i];
            cdEnum.enumChoicesPut(targetPVEnum);
            cdEnum.enumIndexPut(targetPVEnum);
        }
        pvEnumArray.put(0, pvEnums.length, pvEnums, 0);
        super.incrementNumPuts();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnumArray#getElementCDBEnums()
     */
    public CDEnum[] getElementCDEnums() {
        return elementCDEnums;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDEnumArray#replacePVArray()
     */
    public void replacePVEnumArray() {
        pvEnumArray = (PVEnumArray)super.getPVField();
        createElementCDBEnums();
    }
    
    public boolean fieldPut(PVField requested,PVField targetPVField) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        checkPVEnumArray(targetPVEnumArray);
        int length = targetPVEnumArray.getLength();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVField) {
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.fieldPut(targetPVField);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public boolean enumIndexPut(PVField requested,PVEnum targetPVEnum) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        checkPVEnumArray(targetPVEnumArray);
        int length = targetPVEnumArray.getLength();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVEnum) {
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.enumIndexPut(targetPVEnum);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public boolean enumChoicesPut(PVField requested,PVEnum targetPVEnum) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        checkPVEnumArray(targetPVEnumArray);
        int length = targetPVEnumArray.getLength();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVEnum) {
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.enumChoicesPut(targetPVEnum);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public boolean supportNamePut(PVField requested,PVField targetPVField) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        checkPVEnumArray(targetPVEnumArray);
        int length = targetPVEnumArray.getLength();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVField) {
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.supportNamePut(targetPVField);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }

    private void createElementCDBEnums() {
        int length = pvEnumArray.getLength();
        elementCDEnums = new CDEnum[length];
        CDRecord dbRecord = super.getCDRecord();
        pvEnumArray.get(0, length, enumArrayData);
        PVEnum[] pvEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum pvEnum = pvEnums[i];
            if(pvEnum==null) {
                elementCDEnums[i] = null;
            } else {
                elementCDEnums[i] = new BaseCDEnum(this,dbRecord,pvEnum);
            }
        }
    }
    
    private boolean checkPVEnumArray(PVEnumArray targetPVEnumArray) {
        boolean madeChanges = false;
        int length = targetPVEnumArray.getLength();
        if(elementCDEnums.length<length) {
            madeChanges = true;
            CDEnum[] newDatas = new CDEnum[length];
            for(int i=0;i<elementCDEnums.length; i++) {
                newDatas[i] = elementCDEnums[i];
            }
            elementCDEnums = newDatas;
        }
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        pvEnumArray.get(0, length, enumArrayData);
        PVEnum[] pvEnums = enumArrayData.data;
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetPVEnum = targetEnums[i];
            if(targetPVEnum==null) {
                if(pvEnums[i]!=null) {
                    madeChanges = true;
                    pvEnums[i] = null;
                    elementCDEnums[i] = null;
                }
                continue;
            }
            if(elementCDEnums[i]==null) {
                madeChanges = true;
                Field newField = cdRecord.createField(targetPVEnum.getField());
                PVEnum newEnum = (PVEnum)pvDataCreate.createPVField(pvEnumArray, newField);
                pvEnums[i] = newEnum;
                elementCDEnums[i] = new BaseCDEnum(this,cdRecord,newEnum);
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.enumChoicesPut(targetPVEnum);
                cdEnum.enumIndexPut(targetPVEnum);
                cdEnum.supportNamePut(targetPVEnum);
            }
        }
        if(madeChanges) {
            pvEnumArray.put(0, pvEnums.length, pvEnums, 0);
        }
        return madeChanges;
    }
}
