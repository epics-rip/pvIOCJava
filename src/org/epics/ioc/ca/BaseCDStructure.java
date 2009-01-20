/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVDataCreate;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;
/**
 * Base class for a CDStructure (Channel Data Structure).
 * @author mrk
 *
 */
public class BaseCDStructure extends BaseCDField implements CDStructure {
    private CDField[] cdFields;
    private PVField[] pvFields;
    private PVStructure pvStructure;
    private ChannelField[] channelFields = null;
    private CDRecord cdRecord;
    
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvStructure The pvStructure that this CDField references.
     * @param channelFields The ChannelField array.
     */
    public BaseCDStructure(
        CDField parent,CDRecord cdRecord,PVStructure pvStructure,ChannelField[] channelFields)
    {
        super(parent,cdRecord,pvStructure,null);
        this.pvStructure = pvStructure;
        this.cdRecord = cdRecord;
        this.channelFields = channelFields;
        createFields();
        pvFields = this.pvStructure.getPVFields();
    }
    /**
     * Constructor.
     * @param parent The parent cdField.
     * @param cdRecord The cdRecord that contains this field.
     * @param pvStructure The pvStructure for this CDField.
     * @param channelField The channelField.
     */
    public BaseCDStructure(
        CDField parent,CDRecord cdRecord,PVStructure pvStructure,ChannelField channelField)
    {
        super(parent,cdRecord,pvStructure,channelField);
        this.pvStructure = pvStructure;
        this.cdRecord = cdRecord;
        PVStructure sourcePVStructure = (PVStructure)channelField.getPVField();
        PVField[] sourcePVFields = sourcePVStructure.getPVFields();
        int length = sourcePVFields.length;
        channelFields = new ChannelField[length];
        for(int i=0; i<length; i++) {
            String fieldName = sourcePVFields[i].getField().getFieldName();
            channelFields[i] = channelField.createChannelField(fieldName);
            
        }
        createFields(); 
        pvFields = this.pvStructure.getPVFields();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructure#findCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findCDField(PVField pvField) {
        if(pvStructure.getFullFieldName().equals(pvField.getFullFieldName())) return this;
        for(int i=0; i<cdFields.length; i++) {
            if(pvFields[i].getFullFieldName().equals(pvField.getFullFieldName())) return cdFields[i];
        }
        for(int i=0; i<cdFields.length; i++) {
            PVField pvF = pvFields[i];
            Type type = pvF.getField().getType();
            if(type==Type.structure) {
                CDStructure cdStructure = (CDStructure)cdFields[i];
                CDField cdField = cdStructure.findCDField(pvField);
                if(cdField!=null) return cdField;
            }
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructure#findSourceCDField(org.epics.ioc.pv.PVField)
     */
    public CDField findSourceCDField(PVField sourcePVField) {
        for(int i=0; i<cdFields.length; i++) {
            if(channelFields[i].getPVField()==sourcePVField) return cdFields[i];
        }
        for(int i=0; i<cdFields.length; i++) {
            PVField pvF = pvFields[i];
            Type type = pvF.getField().getType();
            if(type==Type.structure) {
                CDStructure cdStructure = (CDStructure)cdFields[i];
                CDField cdField = cdStructure.findSourceCDField(sourcePVField);
                if(cdField!=null) return cdField;
            }
        }
        return null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructure#getFieldCDFields()
     */
    public CDField[] getCDFields() {
        return cdFields;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDStructure#getPVStructure()
     */
    public PVStructure getPVStructure() {
        return pvStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#clearNumPuts()
     */
    public void clearNumPuts() {
        for(CDField cdField : cdFields) {
            cdField.clearNumPuts();
        }
        super.clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#get(org.epics.pvData.pv.PVField)
     */
    public void get(PVField pvField) {               
        if(super.getChannelField().getPVField()!=pvField) {
            throw new IllegalStateException("Logic error");
        }
        if(super.getMaxNumPuts()<=0) return;
        PVStructure targetPVStructure = (PVStructure)pvField;
        PVField[] targetPVFields = targetPVStructure.getPVFields();
        for(int i=0; i<targetPVFields.length; i++) {            
            CDField cdField = cdFields[i];
            PVField target = targetPVFields[i];
            cdField.get(target);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.BaseCDField#dataPut(org.epics.ioc.pv.PVField)
     */
    public void put(PVField pvField) {
        PVStructure targetPVStructure = (PVStructure)pvField;
        if(super.getChannelField().getPVField()!=targetPVStructure) {
            throw new IllegalStateException("Logic error");
        }
        PVField[] targetPVFields = targetPVStructure.getPVFields();
        for(int i=0; i<targetPVFields.length; i++) {            
            CDField cdField = cdFields[i];
            PVField target = targetPVFields[i];
            cdField.put(target);
        }
        super.incrementNumPuts();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDField#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void put(PVField requested, PVField target) {
        CDField targetCDField = findSourceCDField(target);
        if(targetCDField==null) {
            throw new IllegalStateException("Logic error");
        }
        targetCDField.put(target);
    }
    
    private void createFields() {        
        int length = channelFields.length;
        PVField[] pvFields = pvStructure.getPVFields();
        if(length!=pvFields.length) {
            throw new IllegalStateException("Logic error");
        }
        cdFields = new CDField[length];
        for(int i=0; i<length; i++) {
            PVField pvField = pvFields[i];
            Field field = pvField.getField();
            Type type = field.getType();
            switch(type) {
            case scalar:
                cdFields[i] = new BaseCDField(this,cdRecord,pvField,channelFields[i]);
                break;
            case scalarArray:
                 cdFields[i] = new BaseCDField(this,cdRecord,pvField,channelFields[i]);
                 break;
            case structure: {
                PVStructure myStructure = checkStructureField(i);
                //PVStructure myStructure = (PVStructure)pvField;
                cdFields[i] = new BaseCDStructure(
                    this,cdRecord,myStructure,channelFields[i]);
                }
                break;
            default:
                throw new IllegalStateException("Logic error");
            }
        }
        super.clearNumPuts();
    }
    
    private PVStructure checkStructureField(int index) {
        PVField[] pvFields = pvStructure.getPVFields();
        PVStructure pvStructure = (PVStructure)pvFields[index];
        PVStructure sourcePVStructure = (PVStructure)channelFields[index].getPVField();
        PVField[] my = pvStructure.getPVFields();
        PVField[] source = sourcePVStructure.getPVFields();
        int length = my.length;
        if(length==source.length) {
            boolean isOK = true;
            for(int j=0; j<length; j++) {
                if(my[j].getField()!=source[j].getField()) {
                    isOK = false;
                    break;
                }
            }
            if(isOK) return pvStructure;
        }
        PVDataCreate pvDataCreate = cdRecord.getPVDataCreate();
        PVStructure newPVStructure = pvDataCreate.createPVStructure(
                pvStructure.getParent(),
                pvStructure.getField().getFieldName(),
                pvStructure.getStructure().getFields());
        pvFields[index].replacePVField(newPVStructure);
        pvFields[index] = newPVStructure;
        for(int i=0; i<length; i++) {
            Field newField = source[i].getField();
            Type type = newField.getType();
            PVField newPVField = null;
            if(type==Type.scalar) {
                PVScalar pvScalar = (PVScalar)source[i];
                newPVField = pvDataCreate.createPVScalar(
                        newPVStructure,
                        pvScalar.getField().getFieldName(),
                        pvScalar.getScalar().getScalarType());
            } else  if(type==Type.scalarArray) {
                PVArray pvArray = (PVArray)source[i];
                newPVField = pvDataCreate.createPVArray(
                    newPVStructure,
                    pvArray.getField().getFieldName(),
                    pvArray.getArray().getElementType());
            } else {
                PVStructure pvStruct = (PVStructure)source[i];
                newPVField = pvDataCreate.createPVStructure(
                    newPVStructure,
                    pvStruct.getField().getFieldName(),
                    pvStruct);
            }
            my[i].replacePVField(newPVField);
        }
        
        return newPVStructure;
    }
}
