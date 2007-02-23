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
public class BaseCDBArrayArray extends BaseCDBData implements CDBArrayArray{
    private PVArrayArray pvArrayArray;
    private CDBData[] elementCDBDatas;
    
    public BaseCDBArrayArray(
            CDBData parent,CDBRecord cdbRecord,PVArrayArray pvArrayArray)
        {
            super(parent,cdbRecord,pvArrayArray);
            this.pvArrayArray = pvArrayArray;
            createElementCDBArrays();
        }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBArrayArray#dataPut(org.epics.ioc.pv.PVArrayArray)
     */
    public void dataPut(PVArrayArray targetPVArrayArray) {
        CDBRecord cdbRecord = super.getCDBRecord();
        PVDataCreate pvDataCreate = cdbRecord.getPVDataCreate();
        int length = targetPVArrayArray.getLength();
        // discard old elementCDBDatas
        elementCDBDatas = new CDBData[length];
        ArrayArrayData arrayArrayData = new ArrayArrayData();
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetPVArrays = arrayArrayData.data;
        PVArray[] pvArrays = new PVArray[length];
        for(int i=0; i<length; i++) {
            PVArray targetPVArray = targetPVArrays[i];
            if(targetPVArray==null) {
                elementCDBDatas[i] = null;
                pvArrays[i] = null;
                continue;
            }
            Field newField = cdbRecord.createField(targetPVArray.getField());
            PVArray newArray = (PVArray)pvDataCreate.createData(pvArrayArray, newField);
            pvArrays[i] = newArray;
            Array array = (Array)targetPVArray.getField();
            Type elementType = array.getElementType();
            if(elementType.isScalar()) {
                elementCDBDatas[i] = new BaseCDBData(this,cdbRecord,newArray);
                break;
            }
            switch(elementType) {
            case pvArray:
                elementCDBDatas[i] = new BaseCDBArrayArray(this,cdbRecord,(PVArrayArray)newArray);
                break;
            case pvEnum:
                elementCDBDatas[i] = new BaseCDBEnumArray(this,cdbRecord,(PVEnumArray)newArray);
                break;
            case pvMenu:
                elementCDBDatas[i] = new BaseCDBMenuArray(this,cdbRecord,(PVMenuArray)newArray);
                break;
            case pvLink:
                elementCDBDatas[i] = new BaseCDBLinkArray(this,cdbRecord,(PVLinkArray)newArray);
                break;
            case pvStructure:
                elementCDBDatas[i] = new BaseCDBStructureArray(this,cdbRecord,(PVStructureArray)newArray);
                break;
            default:
                throw new IllegalStateException("Logic error");
            }
            elementCDBDatas[i].dataPut(targetPVArray);
            super.setNumPuts(elementCDBDatas[i].getNumPuts());
        }
        pvArrayArray.put(0, pvArrays.length, pvArrays, 0);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBArrayArray#getElementCDBArrays()
     */
    public CDBData[] getElementCDBArrays() {
        return elementCDBDatas;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDBArrayArray#replacePVArray()
     */
    public void replacePVArrayArray() {
        pvArrayArray = (PVArrayArray)super.getPVData();
        createElementCDBArrays();
    }

    public int dataPut(PVData requested,PVData targetPVData) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        int length = pvArrayArray.getLength();
        ArrayArrayData arrayArrayData = new ArrayArrayData();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            CDBData cdbData = elementCDBDatas[i];
            int result = cdbData.dataPut(targetArray, targetPVData);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int enumIndexPut(PVData requested,PVEnum targetPVEnum) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        int length = pvArrayArray.getLength();
        ArrayArrayData arrayArrayData = new ArrayArrayData();
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
            CDBData cdbData = elementCDBDatas[i];
            int result = cdbData.enumIndexPut(targetArray, targetPVEnum);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int enumChoicesPut(PVData requested,PVEnum targetPVEnum) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        int length = pvArrayArray.getLength();
        ArrayArrayData arrayArrayData = new ArrayArrayData();
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
            CDBData cdbData = elementCDBDatas[i];
            int result = cdbData.enumChoicesPut(targetArray, targetPVEnum);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int supportNamePut(PVData requested,PVData targetPVData) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        int length = pvArrayArray.getLength();
        ArrayArrayData arrayArrayData = new ArrayArrayData();
        pvArrayArray.get(0, length, arrayArrayData);
        targetPVArrayArray.get(0, length, arrayArrayData);
        PVArray[] targetArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray targetArray = targetArrays[i];
            if(targetArray==null) continue;
            CDBData cdbData = elementCDBDatas[i];
            int result = cdbData.supportNamePut(targetArray, targetPVData);
            if(result>0) return result;
        }
        return 0;
    }
    
    public int configurationStructurePut(PVData requested,PVLink targetPVLink) {
        PVArrayArray targetPVArrayArray = (PVArrayArray)requested;
        int length = pvArrayArray.getLength();
        ArrayArrayData arrayArrayData = new ArrayArrayData();
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
            CDBData cdbData = elementCDBDatas[i];
            int result = cdbData.configurationStructurePut(targetArray, targetPVLink);
            if(result>0) return result;
        }
        return 0;
    }
    
    private void createElementCDBArrays() {
        int length = pvArrayArray.getLength();
        elementCDBDatas = new CDBData[length];
        CDBRecord cdbRecord = super.getCDBRecord();
        ArrayArrayData arrayArrayData = new ArrayArrayData();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray pvArray = pvArrays[i];
            if(pvArray==null) {
                elementCDBDatas[i] = null;
            } else {
                Array array = (Array)pvArray.getField();
                Type elementType = array.getElementType();
                if(elementType.isScalar()) {
                    elementCDBDatas[i] = new BaseCDBData(this,cdbRecord,pvArray);
                    continue;
                }
                switch(elementType) {
                case pvArray:
                    elementCDBDatas[i] = new BaseCDBArrayArray(this,cdbRecord,(PVArrayArray)pvArray);
                    continue;
                case pvEnum:
                    elementCDBDatas[i] = new BaseCDBEnumArray(this,cdbRecord,(PVEnumArray)pvArray);
                    continue;
                case pvMenu:
                    elementCDBDatas[i] = new BaseCDBMenuArray(this,cdbRecord,(PVMenuArray)pvArray);
                    continue;
                case pvLink:
                    elementCDBDatas[i] = new BaseCDBLinkArray(this,cdbRecord,(PVLinkArray)pvArray);
                    continue;
                case pvStructure:
                    elementCDBDatas[i] = new BaseCDBStructureArray(this,cdbRecord,(PVStructureArray)pvArray);
                    continue;
                default:
                    throw new IllegalStateException("Logic error");
                }
            }
        }
    }
}
