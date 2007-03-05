/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;

import org.epics.ioc.pv.*;


/**
 * Base class for ChannelData.
 * @author mrk
 *
 */
public class BaseChannelData implements ChannelData
{
    private Channel channel;
    private ChannelFieldGroup channelFieldGroup;
    private CDRecord cdRecord;
    private CDStructure cdStructure;
    private CDField[] cdFields;
    private Field[] targetFields;
    
    /**
     * Constructor.
     * @param channel The channel for which to create a ChannelData.
     * @param channelFieldGroup The channelFieldGroup for whicg to cobstruct a CDRecord.
     * @param fieldCreate Factory to create Field introspection objects.
     * @param pvDataCreate Factory to create PVField objects.
     */
    public BaseChannelData(Channel channel,ChannelFieldGroup channelFieldGroup,
            FieldCreate fieldCreate,PVDataCreate pvDataCreate)
    {
        this.channel = channel;
        this.channelFieldGroup = channelFieldGroup;
        List<ChannelField> channelFieldList = channelFieldGroup.getList();
        int length = channelFieldList.size();
        targetFields = new Field[length];
        for(int i=0; i<length; i++) {
            targetFields[i] = channelFieldList.get(i).getField();
        }
        cdRecord = new BaseCDRecord(fieldCreate,pvDataCreate,
            targetFields,channel.getChannelName(),"channelData");
        cdStructure = cdRecord.getCDStructure();
        cdFields = cdStructure.getFieldCDFields();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#getChannel()
     */
    public Channel getChannel() {
        return channel;
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#getChannelFieldGroup()
     */
    public ChannelFieldGroup getChannelFieldGroup() {
        return channelFieldGroup;   
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#getCDRecord()
     */
    public CDRecord getCDRecord() {
        return cdRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#getMaxPutsToField()
     */
    public int getMaxPutsToField() {
        return cdStructure.getMaxNumPuts();
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#clearNumPuts()
     */
    public void clearNumPuts() {
        cdRecord.getCDStructure().clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        CDField cdField = findCDField(targetPVField);
        Field field = targetPVField.getField();
        Type type = field.getType();
        if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) {
                
            }
        }
        cdField.dataPut(targetPVField);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#enumIndexPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumIndexPut(PVEnum targetPVEnum) {
        CDEnum cdEnum  = (CDEnum)findCDField(targetPVEnum);
        cdEnum.enumIndexPut(targetPVEnum);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#enumChoicesPut(org.epics.ioc.pv.PVEnum)
     */
    public void enumChoicesPut(PVEnum targetPVEnum) {
        CDEnum cdEnum  = (CDEnum)findCDField(targetPVEnum);
        cdEnum.enumChoicesPut(targetPVEnum);
    }   
    public void supportNamePut(PVField targetPVField) {
        CDField cdField = findCDField(targetPVField);
        cdField.supportNamePut(targetPVField);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#configurationStructurePut(org.epics.ioc.pv.PVLink)
     */
    public void configurationStructurePut(PVLink targetPVLink) {
        CDLink cdLink = (CDLink)findCDField(targetPVLink);
        if(cdLink==null) {
            throw new IllegalStateException("Logic error.");
        }
        cdLink.configurationStructurePut(targetPVLink);
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#beginPut(org.epics.ioc.pv.PVStructure)
     */
    public void beginPut(PVStructure targetPVStructure) {
        // nothing to do
    }   
    public void endPut(PVStructure targetPVStructure) {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField requested,PVField targetPVField) {
        CDField cdField = findCDField(requested);
        cdField.dataPut(requested, targetPVField);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#enumIndexPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public void enumIndexPut(PVField requested,PVEnum targetPVEnum) {
        CDField cdField = findCDField(requested);
        cdField.enumIndexPut(requested, targetPVEnum);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#enumChoicesPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVEnum)
     */
    public void enumChoicesPut(PVField requested,PVEnum targetPVEnum) {
        CDField cdField = findCDField(requested);
        cdField.enumChoicesPut(requested, targetPVEnum);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void supportNamePut(PVField requested,PVField targetPVField) {
        CDField cdField = findCDField(requested);
        cdField.supportNamePut(requested, targetPVField);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelData#configurationStructurePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVLink)
     */
    public void configurationStructurePut(PVField requested,PVLink targetPVLink) {
        CDField cdField = findCDField(requested);
        cdField.configurationStructurePut(requested, targetPVLink);
    }
    
    private CDField findCDField(PVField targetPVField) {
        Field targetField = targetPVField.getField();
        for(int i=0; i<targetFields.length; i++) {
            if(targetField==targetFields[i]) {
                return cdFields[i];
            }
        }
        throw new IllegalStateException("Logic error.");
    }    
}
