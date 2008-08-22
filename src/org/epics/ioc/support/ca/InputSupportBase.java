/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import java.util.List;

import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDFactory;
import org.epics.ioc.ca.CDField;
import org.epics.ioc.ca.CDGet;
import org.epics.ioc.ca.CDGetRequester;
import org.epics.ioc.ca.CDStructure;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Implementation for a channel access input link.
 * @author mrk
 *
 */
public class InputSupportBase extends AbstractLinkSupport
implements CDGetRequester,
ProcessCallbackRequester,ProcessContinueRequester
{
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    private PVBoolean processAccess = null;  
    private DBField valueDBField;
    
    private String channelFieldName = null;
    private boolean process = false;
    private boolean valueChanged = false;
    private boolean valueIndexChanged = false;
    private int newValueIndex = 0;
    private boolean valueChoicesChanged = false;
    private String[] newValueChoices = null;
    private StringArrayData stringArrayData = null;
    private boolean[] propertyValueChanged = null;
    
    private boolean isReady = false;
    private SupportProcessRequester supportProcessRequester = null;
    
    private CD cd = null;
    private CDGet cdGet;      
    private ChannelField valueChannelField;
    private ChannelField valueIndexChannelField = null;
    private ChannelField valueChoicesChannelField = null;
    private ChannelField alarmChannelField;
    private ChannelFieldGroup channelFieldGroup;
    private ChannelField[] propertyChannelFields = null;
    
    private RequestResult requestResult;   
    private AlarmSeverity alarmSeverity = AlarmSeverity.none;
    private String alarmMessage = null;
    
    
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param dbStructure The dbStructure for the field being supported.
     */
    public InputSupportBase(String supportName,DBStructure dbStructure) {
        super(supportName,dbStructure);
    }
   
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#initialize()
     */
    public void initialize() {           
        super.initialize();
        if(super.getSupportState()!=SupportState.readyForStart) return;
        processAccess = pvStructure.getBooleanField("process");
        if(processAccess==null) {
            uninitialize();
            return;
        }
        DBField dbParent = dbStructure.getParent();
        PVField pvField = null;
        while(dbParent!=null) {
            PVField pvParent = dbParent.getPVField();
            pvField = pvProperty.findProperty(pvParent,"value");
            if(pvField!=null) break;
            dbParent = dbParent.getParent();
        }
        if(pvField==null) {
            pvStructure.message("value field not found", MessageType.error);
            uninitialize();
            return;
        }
        valueDBField = dbStructure.getDBRecord().findDBField(pvField);
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        super.start();
        if(super.getSupportState()!=SupportState.ready) return;
        process = processAccess.get();
        super.connect();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#stop()
     */
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        disconnect();
        super.stop();
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!isReady) {
            if(alarmSupport!=null) alarmSupport.setAlarm(
                    pvStructure.getFullFieldName() + " not connected",
                    AlarmSeverity.major);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
        return;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            channelFieldName = channel.getFieldName();
            if(channelFieldName==null) channelFieldName = "value";
            if(!prepareForInput()) return;
            dbRecord.lock();
            try {
                isReady = true;
            } finally {
                dbRecord.unlock();
            }
        } else {
            dbRecord.lock();
            try {
                isReady = false;
            } finally {
                dbRecord.unlock();
            }
            disconnect();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
     */
    public void processCallback() {
        requestResult = RequestResult.success;
        alarmSeverity = AlarmSeverity.none;
        cd.clearNumPuts();
        cdGet.get(cd);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        post();
        if(alarmIsProperty) {
            if(alarmSupport!=null) alarmSupport.setAlarm(alarmMessage, alarmSeverity);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDGetRequester#getDone(org.epics.ioc.util.RequestResult)
     */
    public void getDone(RequestResult requestResult) {
        dbRecord.lock();
        try {
            ChannelFieldGroup channelFieldGroup = cd.getChannelFieldGroup();
            List<ChannelField> channelFieldList = channelFieldGroup.getList();
            CDStructure cdStructure = cd.getCDRecord().getCDStructure();
            CDField[] cdFields = cdStructure.getCDFields();
            for(int i=0;i<cdFields.length; i++) {
                CDField cdField = cdFields[i];
                ChannelField channelField = channelFieldList.get(i);
                PVField targetPVField = cdField.getPVField();
                if(channelField==alarmChannelField) {
                    PVStructure pvStructure = (PVStructure)targetPVField;
                    PVInt targetPVInt = (PVInt)pvProperty.findProperty(pvStructure,"severity.index");
                    alarmSeverity = AlarmSeverity.getSeverity(targetPVInt.get());
                    PVString pvString = (PVString)pvProperty.findProperty(pvStructure,"message");
                    alarmMessage = pvString.get();
                    continue;
                }
                if(cdField.getMaxNumPuts()==0) continue;
                if(channelField==valueChannelField) {
                    if(copy(targetPVField,valueDBField.getPVField())) valueChanged = true;
                } else if(channelField==valueIndexChannelField){
                    PVInt pvInt = (PVInt)targetPVField;
                    newValueIndex = pvInt.get();
                    valueIndexChanged = true;
                } else if(channelField==valueChoicesChannelField){
                    PVStringArray pvStringArray = (PVStringArray)targetPVField;
                    if(stringArrayData==null) stringArrayData = new StringArrayData();
                    int len = pvStringArray.getLength();
                    pvStringArray.get(0, len, stringArrayData);
                    valueChoicesChanged = true;
                    newValueChoices = stringArrayData.data;
                } else if(gotAdditionalPropertys){
                    boolean foundIt = false;
                    for(int j=0; j<propertyChannelFields.length; j++) {
                        ChannelField propertyChannelField = propertyChannelFields[j];
                        if(channelField==propertyChannelField) {
                            if(copy(targetPVField,propertyPVFields[j])) {
                                propertyValueChanged[j] = true;
                            }
                            foundIt = true;
                            break;
                        }
                    }
                    if(!foundIt) {
                        pvStructure.message(
                                "Logic error in InputSupport",
                                MessageType.error);
                    }
                } else {
                    pvStructure.message(
                            "Logic error in InputSupport",
                            MessageType.fatalError);
                }
            }
            if(requestResult!=RequestResult.success) {
                alarmSeverity = AlarmSeverity.invalid;
                alarmMessage = "get request failed";
            }
        } finally {
            dbRecord.unlock();
        }
        
        this.requestResult = requestResult;
        recordProcess.processContinue(this);
    }
    
    
    private boolean prepareForInput() {
        channelFieldGroup = channel.createFieldGroup(this);
        if(valueIsEnumerated) {
            String name = channelFieldName + "." + "index";
            valueIndexChannelField = channel.createChannelField(name);
            if(valueIndexChannelField==null) {
                pvStructure.message(
                        name
                        + " is not in channel " + channel.getChannelName(),
                        MessageType.error);
                return false;
            }
            channelFieldGroup.addChannelField(valueIndexChannelField);
            name = channelFieldName + "." + "choices";
            valueChoicesChannelField = channel.createChannelField(name);
            if(valueChoicesChannelField==null) {
                pvStructure.message(
                        name
                        + " is not in channel " + channel.getChannelName(),
                        MessageType.error);
                return false;
            }
            channelFieldGroup.addChannelField(valueChoicesChannelField);
        } else {
            valueChannelField = channel.createChannelField(channelFieldName);
            if(valueChannelField==null) {
                pvStructure.message(
                        "channelFieldName " + channelFieldName
                        + " is not in channel " + channel.getChannelName(),
                        MessageType.error);
                return false;
            }
            if(!checkCompatibility(valueChannelField.getField()))return false;
            channelFieldGroup.addChannelField(valueChannelField);
        }
        if(alarmIsProperty) {
            alarmChannelField = channel.createChannelField("alarm");
            if(alarmChannelField!=null) {
                channelFieldGroup.addChannelField(alarmChannelField);
            } else {
                alarmChannelField = null;
            }
        }
        if(gotAdditionalPropertys) {
            int length = propertyPVFields.length;
            propertyValueChanged = new boolean[length];
            propertyChannelFields = new ChannelField[length];
            for(int i=0; i<length; i++) {
                propertyValueChanged[i] = false;
                ChannelField channelField = channel.createChannelField(
                    propertyPVFields[i].getField().getFieldName());
                channelFieldGroup.addChannelField(channelField);
                propertyChannelFields[i] = channelField;
            }
        }
        cd = CDFactory.createCD(channel, channelFieldGroup);
        cdGet = cd.createCDGet(this, process);
        return true;
    }
            
    private boolean checkCompatibility(Field targetField) {
        Type targetType = targetField.getType();
        Field valueField = valueDBField.getPVField().getField();
        Type valueType = valueField.getType();
        if(valueType.isScalar() && targetType.isScalar()) {
            if(convert.isCopyScalarCompatible(targetField,valueField)) return true;
        } else if(targetType==Type.pvArray && valueType==Type.pvArray) {
            Array targetArray = (Array)targetField;
            Array valueArray = (Array)valueField;
            if(convert.isCopyArrayCompatible(targetArray,valueArray)) return true;
        } else if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
            Structure targetStructure = (Structure)targetField;
            Structure valueStructure = (Structure)valueField;
            if(convert.isCopyStructureCompatible(targetStructure,valueStructure)) return true;
        }
        message("is not compatible with pvname " + pvnamePVString.get(),MessageType.error);
        return false;
    }
     
    private boolean copy(PVField fromPVField,PVField toPVField) {
        Type fromType = fromPVField.getField().getType();
        Type toType = toPVField.getField().getType();
        if(fromType.isScalar() && toType.isScalar()) {
            convert.copyScalar(fromPVField, toPVField);
        } else if(fromType==Type.pvArray && toType==Type.pvArray) {
            PVArray fromPVArray = (PVArray)fromPVField;
            PVArray toPVArray = (PVArray)toPVField;
            int arrayLength = fromPVArray.getLength();
            int num = convert.copyArray(fromPVArray,0,toPVArray,0,arrayLength);
            if(num!=arrayLength) message(
                "length " + arrayLength + " but only copied " + num,
                MessageType.warning);;
        } else if(fromType==Type.pvStructure && toType==Type.pvStructure) {
            PVStructure fromPVStructure = (PVStructure)fromPVField;
            PVStructure toPVStructure = (PVStructure)toPVField;
            convert.copyStructure(fromPVStructure,toPVStructure);
        } else {
            message(
                "Logic error: unsupported type",
                MessageType.error);
            return false;
        }
        return true;
    }
    
    private void post() {
        if(valueChanged) {
            valueDBField.postPut();
            valueChanged = false;
        }
        if(valueIndexChanged) {
            valueIndexPVInt.put(newValueIndex);
            valueIndexDBField.postPut();
            valueIndexChanged = false;
            newValueIndex = 0;
        }
        if(valueChoicesChanged) {
            valueChoicesPVStringArray.put(0, newValueChoices.length, newValueChoices, 0);
            valueChoicesDBField.postPut();
            valueChoicesChanged = false;
            newValueChoices = null;
        }
        if(gotAdditionalPropertys){
            for(int j=0; j<propertyChannelFields.length; j++) {
                if(propertyValueChanged[j]) {
                    propertyDBFields[j].postPut();
                    propertyValueChanged[j] = false;
                }
            }
        }
    }
    
    private void disconnect() {
        isReady = false;
        if(cdGet!=null) cdGet.destroy();
        cdGet = null;
        alarmChannelField = null;
        valueChannelField = null;
        valueIndexChannelField = null;
        valueChoicesChannelField = null;
        channelFieldGroup = null;
    }
}
