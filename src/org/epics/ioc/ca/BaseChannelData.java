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
    private CDBRecord cdbRecord;
    private CDBStructure cdbStructure;
    private CDBData[] cdbDatas;
    private Field[] targetFields;
    private int maxPutsToField = 0;
    
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
        cdbRecord = new BaseCDBRecord(fieldCreate,pvDataCreate,
            targetFields,channel.getChannelName(),"channelData");
        cdbStructure = cdbRecord.getCDBStructure();
        cdbDatas = cdbStructure.getFieldCDBDatas();
    }
    public Channel getChannel() {
        return channel;
    }
    
    public ChannelFieldGroup getChannelFieldGroup() {
        return channelFieldGroup;   
    }
    
    public CDBRecord getCDBRecord() {
        return cdbRecord;
    }
    public int getMaxPutsToField() {
        return maxPutsToField;
    }
    
    public void clearNumPuts() {
        cdbRecord.getCDBStructure().clearNumPuts();
        maxPutsToField = 0;
    }
    
    public void initData(PVData targetPVData) {
        dataPut(targetPVData);
    }
    
    public void dataPut(PVData targetPVData) {
        CDBData cdbData = findCDBData(targetPVData);
        Field field = targetPVData.getField();
        Type type = field.getType();
        if(type==Type.pvArray) {
            Array array = (Array)field;
            Type elementType = array.getElementType();
            if(!elementType.isScalar()) {
                
            }
        }
        cdbData.dataPut(targetPVData);
        int numPuts = cdbData.getNumPuts();
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void enumIndexPut(PVEnum targetPVEnum) {
        CDBEnum cdbEnum  = (CDBEnum)findCDBData(targetPVEnum);
        cdbEnum.enumIndexPut(targetPVEnum);
        int numPuts = cdbEnum.getNumIndexPuts();
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void enumChoicesPut(PVEnum targetPVEnum) {
        CDBEnum cdbEnum  = (CDBEnum)findCDBData(targetPVEnum);
        cdbEnum.enumChoicesPut(targetPVEnum);
        int numPuts = cdbEnum.getNumChoicesPut();
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void supportNamePut(PVData targetPVData) {
        CDBData cdbData = findCDBData(targetPVData);
        cdbData.supportNamePut(targetPVData);
        int numPuts = cdbData.getNumSupportNamePuts();
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void configurationStructurePut(PVLink targetPVLink) {
        CDBLink cdbLink = (CDBLink)findCDBData(targetPVLink);
        if(cdbLink==null) {
            throw new IllegalStateException("Logic error.");
        }
        cdbLink.configurationStructurePut(targetPVLink);
        int numPuts = cdbLink.getNumConfigurationStructurePuts();
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void beginPut(PVStructure targetPVStructure) {
        // nothing to do
    }
    
    public void endPut(PVStructure targetPVStructure) {
        // nothing to do
    }

    public void dataPut(PVData requested,PVData targetPVData) {
        CDBData cdbData = findCDBData(requested);
        int numPuts = cdbData.dataPut(requested, targetPVData);
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void enumIndexPut(PVData requested,PVEnum targetPVEnum) {
        CDBData cdbData = findCDBData(requested);
        int numPuts = cdbData.enumIndexPut(requested, targetPVEnum);
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void enumChoicesPut(PVData requested,PVEnum targetPVEnum) {
        CDBData cdbData = findCDBData(requested);
        int numPuts = cdbData.enumChoicesPut(requested, targetPVEnum);
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void supportNamePut(PVData requested,PVData targetPVData) {
        CDBData cdbData = findCDBData(requested);
        int numPuts = cdbData.supportNamePut(requested, targetPVData);
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    
    public void configurationStructurePut(PVData requested,PVLink targetPVLink) {
        CDBData cdbData = findCDBData(requested);
        int numPuts = cdbData.configurationStructurePut(requested, targetPVLink);
        if(numPuts>maxPutsToField) {
            maxPutsToField = numPuts;
        }
    }
    public String toString() {
        return toString(0);
    }
    public String toString(int indentLevel) {
        return String.format(
                "maxPutsToField %d ",maxPutsToField);
    }
    private CDBData findCDBData(PVData targetPVData) {
        Field targetField = targetPVData.getField();
        for(int i=0; i<targetFields.length; i++) {
            if(targetField==targetFields[i]) {
                return cdbDatas[i];
            }
        }
        throw new IllegalStateException("Logic error.");
    }    
}
