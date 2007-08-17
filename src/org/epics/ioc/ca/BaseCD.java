/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;

import org.epics.ioc.pv.*;


/**
 * Base class for CD.
 * @author mrk
 *
 */
public class BaseCD implements CD
{
    private Channel channel;
    private ChannelFieldGroup channelFieldGroup;
    private boolean supportAlso;
    private CDRecord cdRecord;
    private CDStructure cdStructure;
    private CDField[] cdFields;
    private Field[] targetFields;
    
    /**
     * Constructor.
     * @param channel The channel for which to create a CD.
     * @param channelFieldGroup The channelFieldGroup for whicg to cobstruct a CDRecord.
     * @param fieldCreate Factory to create Field introspection objects.
     * @param pvDataCreate Factory to create PVField objects.
     * @param supportAlso Should support be read/written?
     */
    public BaseCD(Channel channel,ChannelFieldGroup channelFieldGroup,
            FieldCreate fieldCreate,PVDataCreate pvDataCreate,boolean supportAlso)
    {
        this.channel = channel;
        this.supportAlso = supportAlso;
        this.channelFieldGroup = channelFieldGroup;
        List<ChannelField> channelFieldList = channelFieldGroup.getList();
        int length = channelFieldList.size();
        targetFields = new Field[length];
        for(int i=0; i<length; i++) {
            targetFields[i] = channelFieldList.get(i).getField();
        }
        cdRecord = new BaseCDRecord(fieldCreate,pvDataCreate,
            targetFields,channel.getChannelName(),"channelData",supportAlso);
        cdStructure = cdRecord.getCDStructure();
        cdFields = cdStructure.getFieldCDFields();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#getChannel()
     */
    public Channel getChannel() {
        return channel;
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#getChannelFieldGroup()
     */
    public ChannelFieldGroup getChannelFieldGroup() {
        return channelFieldGroup;   
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#getCDRecord()
     */
    public CDRecord getCDRecord() {
        return cdRecord;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#getMaxPutsToField()
     */
    public int getMaxPutsToField() {
        return cdStructure.getMaxNumPuts();
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#clearNumPuts()
     */
    public void clearNumPuts() {
        cdRecord.getCDStructure().clearNumPuts();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#dataPut(org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField targetPVField) {
        CDField cdField = findCDField(targetPVField);
        cdField.dataPut(targetPVField);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#supportNamePut(org.epics.ioc.pv.PVField)
     */
    public void supportNamePut(PVField targetPVField) {
        CDField cdField = findCDField(targetPVField);
        cdField.supportNamePut(targetPVField.getSupportName());
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#beginPut(org.epics.ioc.pv.PVStructure)
     */
    public void beginPut(PVStructure targetPVStructure) {
        // nothing to do
    }   
    public void endPut(PVStructure targetPVStructure) {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#dataPut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void dataPut(PVField requested,PVField targetPVField) {
        CDField cdField = findCDField(requested);
        cdField.dataPut(requested, targetPVField);
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CD#supportNamePut(org.epics.ioc.pv.PVField, org.epics.ioc.pv.PVField)
     */
    public void supportNamePut(PVField requested,PVField targetPVField) {
        if(!supportAlso) return;
        CDField cdField = findCDField(requested);
        cdField.supportNamePut(requested, targetPVField);
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
