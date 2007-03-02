/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;

import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;


/**
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
    public Channel getChannel() {
        return channel;
    }
    
    public ChannelFieldGroup getChannelFieldGroup() {
        return channelFieldGroup;   
    }
    
    public CDRecord getCDRecord() {
        return cdRecord;
    }
    public int getMaxPutsToField() {
        return cdStructure.getMaxNumPuts();
    }
    
    public void clearNumPuts() {
        cdRecord.getCDStructure().clearNumPuts();
    }
    
    public void initField(PVField targetPVField) {
        fieldPut(targetPVField);
    }
    
    public void fieldPut(PVField targetPVField) {
        CDField cdField = findCDField(targetPVField);
        Field field = targetPVField.getField();
        Type type = field.getType();
        if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) {
                
            }
        }
        cdField.fieldPut(targetPVField);
    }
    
    public void enumIndexPut(PVEnum targetPVEnum) {
        CDEnum cdEnum  = (CDEnum)findCDField(targetPVEnum);
        cdEnum.enumIndexPut(targetPVEnum);
    }
    
    public void enumChoicesPut(PVEnum targetPVEnum) {
        CDEnum cdEnum  = (CDEnum)findCDField(targetPVEnum);
        cdEnum.enumChoicesPut(targetPVEnum);
    }
    
    public void supportNamePut(PVField targetPVField) {
        CDField cdField = findCDField(targetPVField);
        cdField.supportNamePut(targetPVField);
    }
    
    public void configurationStructurePut(PVLink targetPVLink) {
        CDLink cdLink = (CDLink)findCDField(targetPVLink);
        if(cdLink==null) {
            throw new IllegalStateException("Logic error.");
        }
        cdLink.configurationStructurePut(targetPVLink);
    }
    
    public void beginPut(PVStructure targetPVStructure) {
        // nothing to do
    }
    
    public void endPut(PVStructure targetPVStructure) {
        // nothing to do
    }

    public void fieldPut(PVField requested,PVField targetPVField) {
        CDField cdField = findCDField(requested);
        cdField.fieldPut(requested, targetPVField);
    }
    
    public void enumIndexPut(PVField requested,PVEnum targetPVEnum) {
        CDField cdField = findCDField(requested);
        cdField.enumIndexPut(requested, targetPVEnum);
    }
    
    public void enumChoicesPut(PVField requested,PVEnum targetPVEnum) {
        CDField cdField = findCDField(requested);
        cdField.enumChoicesPut(requested, targetPVEnum);
    }
    
    public void supportNamePut(PVField requested,PVField targetPVField) {
        CDField cdField = findCDField(requested);
        cdField.supportNamePut(requested, targetPVField);
    }
    
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
