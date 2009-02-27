/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.PVField;
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
     * @param channelField The channelField.
     * @param channelFields The ChannelField array.
     */
    public BaseCDStructure(
        CDField parent,CDRecord cdRecord,PVStructure pvStructure,ChannelField channelField,ChannelField[] channelFields)
    {
        super(parent,cdRecord,pvStructure,channelField);
        this.pvStructure = pvStructure;
        this.cdRecord = cdRecord;
        this.channelFields = channelFields;
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
     * @see org.epics.ioc.ca.BaseCDField#putSubfield(org.epics.pvData.pv.PVField)
     */
    public void putSubfield(PVField target) {
        CDField targetCDField = findSourceCDField(target);
        if(targetCDField==null) {
            return;
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
            ChannelField channelField = channelFields[i];
            PVField pvField = pvFields[i];
            Field field = pvField.getField();
            Type type = field.getType();
            switch(type) {
            case scalar:
            case scalarArray:
                 cdFields[i] = new BaseCDField(this,cdRecord,pvField,channelField);
                 break;
            case structure: {
                ChannelField[] subFields = createChannelFields(channelField);
                cdFields[i] = new BaseCDStructure(
                    this,cdRecord,(PVStructure)pvField,channelField,subFields);
                }
                break;
            default:
                throw new IllegalStateException("Logic error");
            }
            
        }
        super.clearNumPuts();
    }
    
    private ChannelField[] createChannelFields(ChannelField channelField) {
        PVStructure sourcePVStructure = (PVStructure)channelField.getPVField();
        PVField[] sourcePVFields = sourcePVStructure.getPVFields();
        int length = sourcePVFields.length;
        ChannelField[] channelFields = new ChannelField[length];
        for(int i=0; i<length; i++) {
            String fieldName = sourcePVFields[i].getField().getFieldName();
            channelFields[i] = channelField.createChannelField(fieldName);
            
        }
        return channelFields;
    }
   
}
