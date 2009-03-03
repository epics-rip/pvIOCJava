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
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Type;

/**
 * Implementation for a channel access input link.
 * @author mrk
 *
 */
public class InputLinkBase extends AbstractLink
implements CDGetRequester,Runnable,ProcessContinueRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvField The field being supported.
     */
    public InputLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
        executor = ExecutorFactory.create(pvField.getFullName(), ThreadPriority.lower);
    }
    
    private Executor executor = null;
    private PVBoolean processAccess = null;  
    private PVField valuePVField;
    
    private String channelFieldName = null;
    private boolean process = false;
    private boolean valueChanged = false;
    private boolean valueIndexChanged = false;
    private int newValueIndex = 0;
    private boolean valueChoicesChanged = false;
    private String[] newValueChoices = null;
    private StringArrayData stringArrayData = null;
    
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
    
    
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {   
        super.initialize(recordSupport);
        if(super.getSupportState()!=SupportState.readyForStart) return;
        processAccess = pvStructure.getBooleanField("process");
        if(processAccess==null) {
            uninitialize();
            return;
        }
        PVStructure pvParent = pvStructure.getParent();
        valuePVField = null;
        while(pvParent!=null) {
            valuePVField = pvParent.getSubField("value");
            if(valuePVField!=null) break;
            pvParent = pvParent.getParent();
        }
        if(valuePVField==null) {
            pvStructure.message("value field not found", MessageType.error);
            uninitialize();
            return;
        }
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
        executor.execute(this);
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
            pvRecord.lock();
            try {
                isReady = true;
            } finally {
                pvRecord.unlock();
            }
        } else {
            pvRecord.lock();
            try {
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
            disconnect();
        }
    }
   
    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        requestResult = RequestResult.success;
        alarmSeverity = AlarmSeverity.none;
        cd.clearNumPuts();
        cdGet.get(cd);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        alarmSupport.beginProcess();
        post();
        if(alarmIsProperty) alarmSupport.setAlarm(alarmMessage, alarmSeverity);
        supportProcessRequester.supportProcessDone(requestResult);
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDGetRequester#getDone(org.epics.ioc.util.RequestResult)
     */
    public void getDone(RequestResult requestResult) {
        pvRecord.lock();
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
                    PVInt targetPVInt = (PVInt)pvStructure.getSubField("severity.index");
                    alarmSeverity = AlarmSeverity.getSeverity(targetPVInt.get());
                    PVString pvString = (PVString)pvStructure.getSubField("message");
                    alarmMessage = pvString.get();
                    continue;
                }
                if(cdField.getMaxNumPuts()==0) continue;
                if(channelField==valueChannelField) {
                    if(copy(targetPVField,valuePVField)) valueChanged = true;
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
                            copy(targetPVField,propertyPVFields[j]);
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
            pvRecord.unlock();
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
            propertyChannelFields = new ChannelField[length];
            for(int i=0; i<length; i++) {
                String fieldName = propertyPVFields[i].getField().getFieldName();
                ChannelField channelField = channel.createChannelField(fieldName);
                if(channelField==null) {
                    pvStructure.message(
                            "propertyName " + fieldName
                            + " is not in channel " + channel.getChannelName(),
                            MessageType.error);
                    return false;
                }
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
        Field valueField = valuePVField.getField();
        Type valueType = valueField.getType();
        if(valueType==Type.scalar && targetType==Type.scalar) {
            if(convert.isCopyScalarCompatible((Scalar)targetField,(Scalar)valueField)) return true;
        } else if(targetType==Type.scalarArray && valueType==Type.scalarArray) {
            Array targetArray = (Array)targetField;
            Array valueArray = (Array)valueField;
            if(convert.isCopyArrayCompatible(targetArray,valueArray)) return true;
        } else if(targetType==Type.structure && valueType==Type.structure) {
            Structure targetStructure = (Structure)targetField;
            Structure valueStructure = (Structure)valueField;
            if(convert.isCopyStructureCompatible(targetStructure,valueStructure)) return true;
        }
        message("is not compatible with pvname " + pvnamePV.get(),MessageType.error);
        return false;
    }
     
    private boolean copy(PVField fromPVField,PVField toPVField) {
        Type fromType = fromPVField.getField().getType();
        Type toType = toPVField.getField().getType();
        if(fromType==Type.scalar && toType==Type.scalar) {
            convert.copyScalar((PVScalar)fromPVField,(PVScalar)toPVField);
        } else if(fromType==Type.scalarArray && toType==Type.scalarArray) {
            PVArray fromPVArray = (PVArray)fromPVField;
            PVArray toPVArray = (PVArray)toPVField;
            int arrayLength = fromPVArray.getLength();
            int num = convert.copyArray(fromPVArray,0,toPVArray,0,arrayLength);
            if(num!=arrayLength) message(
                "length " + arrayLength + " but only copied " + num,
                MessageType.warning);;
        } else if(fromType==Type.structure && toType==Type.structure) {
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
            valueChanged = false;
        }
        if(valueIndexChanged) {
            valueIndexPV.put(newValueIndex);
            valueIndexPV.postPut();
            valueIndexChanged = false;
            newValueIndex = 0;
        }
        if(valueChoicesChanged) {
            valueChoicesPV.put(0, newValueChoices.length, newValueChoices, 0);
            valueChoicesPV.postPut();
            valueChoicesChanged = false;
            newValueChoices = null;
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
