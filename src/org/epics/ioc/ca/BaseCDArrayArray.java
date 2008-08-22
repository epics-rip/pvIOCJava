/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.ArrayArrayData;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVArrayArray;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.Type;
/**
 * Base class for a CD (Channel Data) Array of Arrays.
 * @author mrk
 *
 */
public class BaseCDArrayArray extends BaseCDArray implements CDArrayArray{
    private PVArrayArray pvArrayArray;
    private CDArray[] elementCDArrays;
    private ArrayArrayData arrayArrayData = new ArrayArrayData();
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvArrayArray The pvArrayArray that this CDField references.
     */
    public BaseCDArrayArray(
        CDField parent,CDRecord cdRecord,PVArrayArray pvArrayArray,ChannelField channelField)
    {
        super(parent,cdRecord,pvArrayArray,channelField);
        this.pvArrayArray = pvArrayArray;
        createElementCDBArrays();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDArrays) {
            if(cdField==null) continue;
            cdField.clearNumPuts();
        }
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDArrayArray#getElementCDArrays()
     */
    public CDArray[] getElementCDArrays() {
        return elementCDArrays;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDArrayArray#findCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findCDField(PVField pvField) {
        int length = elementCDArrays.length;
        for(int i=0; i<length; i++) {
            CDArray cdArray = elementCDArrays[i];
            if(cdArray==null) continue;
            if(cdArray.getPVField()==pvField) return cdArray;
        }
        for(int i=0; i<length; i++) {
            CDArray cdArray = elementCDArrays[i];
            if(cdArray==null) continue;
            PVArray pvArray = cdArray.getPVArray();
            Type elementType = pvArray.getArray().getType();
            if(elementType==Type.pvArray) {
                CDArrayArray elementCD = (CDArrayArray)cdArray;
                CDField cdField = elementCD.findCDField(pvField);
                if(cdField!=null) return cdField;
            } else if(elementType==Type.pvStructure) {
                CDStructureArray elementCD = (CDStructureArray)cdArray;
                CDField cdField = elementCD.findCDField(pvField);
                if(cdField!=null) return cdField;
            }
            
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDArrayArray#findSourceCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findSourceCDField(PVField pvField) {
        int length = elementCDArrays.length;
        for(int i=0; i<length; i++) {
            CDArray cdArray = elementCDArrays[i];
            if(cdArray==null) continue;
            if(cdArray.getChannelField().getPVField()==pvField) return cdArray;
        }
        for(int i=0; i<length; i++) {
            CDArray cdArray = elementCDArrays[i];
            if(cdArray==null) continue;
            PVArray pvArray = cdArray.getPVArray();
            Type elementType = pvArray.getArray().getElementType();
            if(elementType==Type.pvArray) {
                CDArrayArray elementCD = (CDArrayArray)cdArray;
                CDField cdField = elementCD.findSourceCDField(pvField);
                if(cdField!=null) return cdField;
            } else if(elementType==Type.pvStructure) {
                CDStructureArray elementCD = (CDStructureArray)cdArray;
                CDField cdField = elementCD.findSourceCDField(pvField);
                if(cdField!=null) return cdField;
            }
            
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#get(org.epics.ioc.pv.PVField, boolean)
     */
    public void get(PVField toPVField,boolean postPut) {       
        if(super.getChannelField().getPVField()!=toPVField) {
            throw new IllegalStateException("Logic error");
        }
        if(super.getMaxNumPuts()<=0) return;
        PVArrayArray sourcePVArrayArray = (PVArrayArray)toPVField;
        int length = pvArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        sourcePVArrayArray.get(0, length, arrayArrayData);
        PVArray[] sourcePVArrays = arrayArrayData.data;
        boolean postSubField = postPut;
        if(super.getNumPuts()>0) postSubField = false;
        for(int i=0; i<length; i++) {
            PVArray pvArray = pvArrays[i];
            PVArray sourcePVArray = sourcePVArrays[i];
            if(pvArray==null||sourcePVArray==null) continue;
            CDField cdField = elementCDArrays[i];
            cdField.get(sourcePVArray,postSubField);
        }
        if(postPut && super.getNumPuts()>0) super.getChannelField().postPut();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void put(PVField pvField) {
        if(super.getChannelField().getPVField()!=pvField) {
            throw new IllegalStateException("Logic error");
        }
        PVArrayArray sourcePVArrayArray = (PVArrayArray)pvField;
        int length = sourcePVArrayArray.getLength();
        pvArrayArray.get(0, length, arrayArrayData);
        PVArray[] pvArrays = arrayArrayData.data;
        sourcePVArrayArray.get(0, length, arrayArrayData);
        PVArray[] sourceArrays = arrayArrayData.data;
        for(int i=0; i<length; i++) {
            PVArray sourcePVArray = sourceArrays[i];
            if(sourcePVArray==null) continue;
            CDField cdField = elementCDArrays[i];
            cdField.put(sourcePVArray);
        }
        pvArrayArray.put(0, pvArrays.length, pvArrays, 0);
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void put(PVField pvField,PVField pvSubField) {
        if(super.getChannelField().getPVField()!=pvField) {
            throw new IllegalStateException("Logic error");
        }
        CDField cdField = findSourceCDField(pvSubField);
        if(cdField==null) {
            throw new IllegalStateException("Logic error");
        }
        cdField.put(pvSubField);
    }   
    
    private void createElementCDBArrays() {
        ChannelField channelField = super.getChannelField();
        PVArrayArray sourcePVArrayArray = (PVArrayArray)channelField.getPVField();
        int length = sourcePVArrayArray.get(0,sourcePVArrayArray.getLength(), arrayArrayData);
        PVArray[] sourcePVArrays = arrayArrayData.data;
        elementCDArrays = new CDArray[length];       
        PVArray[] pvArrays = new PVArray[length];
        for(int i=0; i<length; i++) {
            PVArray pvArray = sourcePVArrays[i];
            if(pvArray==null) {
                elementCDArrays[i] = null;
                pvArrays[i] = null;
            } else {
                elementCDArrays[i] = createCDArray(pvArray);
                pvArrays[i] = elementCDArrays[i].getPVArray();
            }
        }
        pvArrayArray.put(0, length, pvArrays, 0);
    }
    
    private CDArray createCDArray(PVArray pvArray) {
        CDRecord cdRecord = super.getCDRecord();
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        Array array = (Array)pvArray.getField();
        Type elementType = array.getElementType();
        PVArray newPVArray = pvDataCreate.createPVArray(
            pvArrayArray,
            array,
            pvArray.getCapacity(),
            pvArray.isCapacityMutable());
        ChannelField channelField = super.getChannelField().findProperty(pvArray.getField().getFieldName());
        if(elementType.isScalar()) {
            return new BaseCDArray(this,cdRecord,newPVArray,channelField);
        }
        switch(elementType) {
        case pvArray:
            return new BaseCDArrayArray(
                this,cdRecord,(PVArrayArray)newPVArray,channelField);
        case pvStructure:
            return new BaseCDStructureArray(
                this,cdRecord,(PVStructureArray)newPVArray,channelField);
        default:
            throw new IllegalStateException("Logic error");
        }
    }
}
