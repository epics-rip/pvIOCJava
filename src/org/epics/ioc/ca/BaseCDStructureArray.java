/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVDataCreate;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.StructureArrayData;
/**
 * Base class for a CD (Channel Data) Array of Structures.
 * @author mrk
 *
 */
public class BaseCDStructureArray extends BaseCDArray implements CDStructureArray{
    private PVStructureArray pvStructureArray;
    private CDStructure[] elementCDStructures;
    private StructureArrayData structureArrayData = new StructureArrayData();

    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvStructureArray The pvStructureArray that this CDField references.
     */
    public BaseCDStructureArray(
        CDField parent,
        CDRecord cdRecord,
        PVStructureArray pvStructureArray,
        ChannelField channelField)
    {
        super(parent,cdRecord,pvStructureArray,channelField);
        this.pvStructureArray = pvStructureArray;
        createElementCDBStructures();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#findCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findCDField(PVField pvField) {
        int length = elementCDStructures.length;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            if(cdStructure.getPVField()==pvField) return cdStructure;
        }
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            CDField result = cdStructure.findCDField(pvField);
            if(result!=null) return result;
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#findSourceCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findSourceCDField(PVField sourcePVField) {
        int length = elementCDStructures.length;
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            if(cdStructure.getChannelField().getPVField()==sourcePVField) return cdStructure;
        }
        for(int i=0; i<length; i++) {
            CDStructure cdStructure = elementCDStructures[i];
            CDField result = cdStructure.findSourceCDField(sourcePVField);
            if(result!=null) return result;
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#getElementCDStructures()
     */
    public CDStructure[] getElementCDStructures() {
        return elementCDStructures;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructureArray#getSourcePVStructureArray()
     */
    public PVStructureArray getSourcePVStructureArray() {
        return pvStructureArray;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : elementCDStructures) cdField.clearNumPuts();
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#get(org.epics.ioc.pv.PVField, boolean)
     */
    public void get(PVField pvField,boolean postPut) {
        if(super.getChannelField().getPVField()!=pvField) {
            throw new IllegalStateException("Logic error");
        }
        if(super.getMaxNumPuts()<=0) return;
        int length = pvStructureArray.getLength();
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        boolean postSubField = postPut;
        if(super.getNumPuts()>0) postSubField = false;
        for(int i=0; i<length; i++) {
            PVStructure pvStructure = pvStructures[i];
            if(pvStructure==null) continue;
            CDStructure cdStructure = (CDStructure)elementCDStructures[i];
            cdStructure.get(cdStructure.getChannelField().getPVField(),postSubField);
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
        PVStructureArray sourcePVStructureArray = (PVStructureArray)pvField;        
        int length = sourcePVStructureArray.getLength();
        if(length!=elementCDStructures.length) {
            throw new IllegalStateException("Logic error");
        }
        sourcePVStructureArray.get(0, length, structureArrayData);
        PVStructure[] sourceStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure sourcePVStructure = sourceStructures[i];
            if(sourcePVStructure==null) continue;
            CDStructure cdStructure = (CDStructure)elementCDStructures[i];
            if(cdStructure==null) {
                throw new IllegalStateException("Logic error");
            }
            cdStructure.put(sourcePVStructure);
        }
        super.incrementNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void put(PVField requested,PVField targetPVField) {
        if(super.getChannelField().getPVField()!=requested) {
            throw new IllegalStateException("Logic error");
        }
        CDField cdField = findSourceCDField(targetPVField);
        if(cdField==null) {
            throw new IllegalStateException("Logic error");
        }
        cdField.put(targetPVField);
    }   
   
    private void createElementCDBStructures() {
        CDRecord cdRecord = super.getCDRecord();
        ChannelField channelField = super.getChannelField();
        PVStructureArray sourcePVStructureArray = (PVStructureArray)channelField.getPVField();
        int length = sourcePVStructureArray.get(0,
            sourcePVStructureArray.getLength(), structureArrayData);
        PVStructure[] sourcePVStructures = structureArrayData.data;
        int num = pvStructureArray.get(0, pvStructureArray.getLength(), structureArrayData);
        if(num!=length) {            
            PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
            Field[] newFields = new Field[length];
            PVStructure[] newPVStructures = new PVStructure[length];
            PVStructureArray newPVStructureArray = (PVStructureArray)pvDataCreate.createPVArray(
                pvStructureArray.getParent(),
                sourcePVStructureArray.getField(), sourcePVStructureArray.getCapacity(),
                sourcePVStructureArray.isCapacityMutable());
            for(int i=0; i<length; i++) {
                if(sourcePVStructures[i]==null) {
                    newFields[i] = null;
                    newPVStructures[i] = null;
                } else {
                    newFields[i] = sourcePVStructures[i].getField();
                    newPVStructures[i] = (PVStructure)pvDataCreate.createPVField(
                            newPVStructureArray, newFields[i]);
                }
            }
            newPVStructureArray.put(0, length, newPVStructures, 0);
            pvStructureArray = newPVStructureArray;
        }
        elementCDStructures = new CDStructure[length];
        pvStructureArray.get(0, length, structureArrayData);
        PVStructure[] pvStructures = structureArrayData.data;
        for(int i=0; i<length; i++) {
            PVStructure pvStructure = pvStructures[i];
            if(pvStructure==null) {
                elementCDStructures[i] = null;
            } else {
                String fieldName = "[" + i + "]";
                ChannelField newChannelField = channelField.createChannelField(fieldName);
if(newChannelField==null) {
    throw new IllegalStateException("Logic error");
}
                elementCDStructures[i] = new BaseCDStructure(this,cdRecord,pvStructure,newChannelField);
            }
        }
        super.clearNumPuts();
    }
}
