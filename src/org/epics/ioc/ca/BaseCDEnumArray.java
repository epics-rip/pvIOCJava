/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Base class for a CD (Channel Data) Array of Enums.
 * @author mrk
 *
 */
public class BaseCDEnumArray extends BaseCDField implements CDNonScalarArray {
    private boolean supportAlso;
    private PVEnumArray pvEnumArray;
    private CDEnum[] elementCDEnums;
    private EnumArrayData enumArrayData = new EnumArrayData();
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvEnumArray The pvEnumArray that this CDField references.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDEnumArray(
        CDField parent,CDRecord cdRecord,PVEnumArray pvEnumArray,boolean supportAlso)
    {
        super(parent,cdRecord,pvEnumArray,supportAlso);
        this.supportAlso = supportAlso;
        this.pvEnumArray = pvEnumArray;
        createElementCDBEnums();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDEnums) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        if(supportAlso) {
            String supportName = targetPVField.getSupportName();
            if(supportName!=null) super.supportNamePut(supportName);
        }
        PVEnumArray targetPVEnumArray = (PVEnumArray)targetPVField;
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
            cdEnum.enumChoicesPut(targetPVEnum.getChoices());
            cdEnum.enumIndexPut(targetPVEnum.getIndex());
        }
        pvEnumArray.put(0, pvEnums.length, pvEnums, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#getElementCDFields()
     */
    public CDField[] getElementCDFields() {
        return elementCDEnums;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDNonScalarArray#replacePVArray()
     */
    public void replacePVArray() {
        pvEnumArray = (PVEnumArray)super.getPVField();
        createElementCDBEnums();
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean dataPut(PVField requested,PVField targetPVField) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        checkPVEnumArray(targetPVEnumArray);
        int length = targetPVEnumArray.getLength();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVField) {
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.dataPut(targetPVField);
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#enumIndexPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
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
                cdEnum.enumIndexPut(targetPVEnum.getIndex());
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#enumChoicesPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
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
                cdEnum.enumChoicesPut(targetPVEnum.getChoices());
                return true;
            }
        }
        throw new IllegalStateException("Logic error.");
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public boolean supportNamePut(PVField requested,PVField targetPVField) {
        if(!supportAlso) return false;
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        checkPVEnumArray(targetPVEnumArray);
        int length = targetPVEnumArray.getLength();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVField) {
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.supportNamePut(targetPVField.getSupportName());
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
                elementCDEnums[i] = new BaseCDEnum(this,dbRecord,pvEnum,supportAlso);
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
                elementCDEnums[i] = new BaseCDEnum(this,cdRecord,newEnum,supportAlso);
                CDEnum cdEnum = elementCDEnums[i];
                cdEnum.enumChoicesPut(targetPVEnum.getChoices());
                cdEnum.enumIndexPut(targetPVEnum.getIndex());
                cdEnum.supportNamePut(targetPVEnum.getSupportName());
            }
        }
        if(madeChanges) {
            pvEnumArray.put(0, pvEnums.length, pvEnums, 0);
        }
        return madeChanges;
    }
}
