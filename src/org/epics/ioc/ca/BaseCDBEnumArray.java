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
public class BaseCDBEnumArray extends BaseCDBData implements CDBEnumArray {
    private PVEnumArray pvEnumArray;
    private CDBEnum[] elementCDBEnums;
    
    public BaseCDBEnumArray(
            CDBData parent,CDBRecord cdbRecord,PVEnumArray pvEnumArray)
        {
            super(parent,cdbRecord,pvEnumArray);
            this.pvEnumArray = pvEnumArray;
            createElementCDBEnums();
        }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnumArray#dataPut(org.epics.ioc.pv.PVEnumArray)
     */
    public void dataPut(PVEnumArray targetPVEnumArray) {
        int length = targetPVEnumArray.getLength();
        if(elementCDBEnums.length<length) {
            CDBEnum[] newDatas = new CDBEnum[length];
            for(int i=0;i<elementCDBEnums.length; i++) {
                newDatas[i] = elementCDBEnums[i];
            }
            elementCDBEnums = newDatas;
        }
        CDBRecord cdbRecord = super.getCDBRecord();
        PVDataCreate pvDataCreate = cdbRecord.getPVDataCreate();
        EnumArrayData enumArrayData = new EnumArrayData();
        pvEnumArray.get(0, length, enumArrayData);
        PVEnum[] pvEnums = enumArrayData.data;
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetPVEnum = targetEnums[i];
            if(targetPVEnum==null) {
                elementCDBEnums[i] = null;
                continue;
            }
            if(elementCDBEnums[i]==null) {
                Field newField = cdbRecord.createField(targetPVEnum.getField());
                PVEnum newEnum = (PVEnum)pvDataCreate.createData(pvEnumArray, newField);
                pvEnums[i] = newEnum;
                elementCDBEnums[i] = new BaseCDBEnum(this,cdbRecord,newEnum);
            }
            CDBEnum cdbEnum = elementCDBEnums[i];
            cdbEnum.enumChoicesPut(targetPVEnum);
            cdbEnum.enumIndexPut(targetPVEnum);
            super.setNumPuts(cdbEnum.getNumChoicesPut());
            super.setNumPuts(cdbEnum.getNumIndexPuts());
        }
        pvEnumArray.put(0, pvEnums.length, pvEnums, 0);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnumArray#getElementCDBEnums()
     */
    public CDBEnum[] getElementCDBEnums() {
        return elementCDBEnums;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBEnumArray#replacePVArray()
     */
    public void replacePVEnumArray() {
        pvEnumArray = (PVEnumArray)super.getPVData();
        createElementCDBEnums();
    }
    
    public int dataPut(PVData requested,PVData targetPVData) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        int length = targetPVEnumArray.getLength();
        EnumArrayData enumArrayData = new EnumArrayData();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVData) {
                CDBEnum cdbEnum = elementCDBEnums[i];
                cdbEnum.dataPut(targetPVData);
                return cdbEnum.getNumPuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public int enumIndexPut(PVData requested,PVEnum targetPVEnum) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        int length = targetPVEnumArray.getLength();
        EnumArrayData enumArrayData = new EnumArrayData();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVEnum) {
                CDBEnum cdbEnum = elementCDBEnums[i];
                cdbEnum.enumIndexPut(targetPVEnum);
                return cdbEnum.getNumIndexPuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public int enumChoicesPut(PVData requested,PVEnum targetPVEnum) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        int length = targetPVEnumArray.getLength();
        EnumArrayData enumArrayData = new EnumArrayData();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVEnum) {
                CDBEnum cdbEnum = elementCDBEnums[i];
                cdbEnum.enumChoicesPut(targetPVEnum);
                return cdbEnum.getNumChoicesPut();
            }
        }
        throw new IllegalStateException("Logic error.");
    }
    
    public int supportNamePut(PVData requested,PVData targetPVData) {
        PVEnumArray targetPVEnumArray = (PVEnumArray)requested;
        int length = targetPVEnumArray.getLength();
        EnumArrayData enumArrayData = new EnumArrayData();
        targetPVEnumArray.get(0, length, enumArrayData);
        PVEnum[] targetEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum targetEnum = targetEnums[i];
            if(targetEnum==targetPVData) {
                CDBEnum cdbEnum = elementCDBEnums[i];
                cdbEnum.supportNamePut(targetPVData);
                return cdbEnum.getNumSupportNamePuts();
            }
        }
        throw new IllegalStateException("Logic error.");
    }

    private void createElementCDBEnums() {
        int length = pvEnumArray.getLength();
        elementCDBEnums = new CDBEnum[length];
        CDBRecord dbRecord = super.getCDBRecord();
        EnumArrayData enumArrayData = new EnumArrayData();
        pvEnumArray.get(0, length, enumArrayData);
        PVEnum[] pvEnums = enumArrayData.data;
        for(int i=0; i<length; i++) {
            PVEnum pvEnum = pvEnums[i];
            if(pvEnum==null) {
                elementCDBEnums[i] = null;
            } else {
                elementCDBEnums[i] = new BaseCDBEnum(this,dbRecord,pvEnum);
            }
        }
    }
}
