/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDFactory;
import org.epics.ioc.ca.CDField;
import org.epics.ioc.ca.CDPut;
import org.epics.ioc.ca.CDPutRequester;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.process.ProcessCallbackRequester;
import org.epics.ioc.process.ProcessContinueRequester;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Implementation for a channel access output link.
 * @author mrk
 *
 */
public class OutputSupportBase extends AbstractLinkSupport
implements ProcessCallbackRequester,ProcessContinueRequester,CDPutRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param dbStructure The dbStructure for the field being supported.
     */
    public OutputSupportBase(String supportName,DBStructure dbStructure) {
        super(supportName,dbStructure);
    }
    private PVBoolean processAccess = null;
    private DBField valueDBField = null;
    
    private boolean process = false;
    
    private boolean isReady = false;
    
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult = null;   
                 
    private String channelFieldName = null;
    private CD cd = null;
    private CDPut cdPut = null;
    private ChannelField valueChannelField = null;
    private ChannelFieldGroup putFieldGroup = null;
    
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            channelFieldName = channel.getFieldName();
            if(channelFieldName==null) channelFieldName = "value";
            if(!prepareForOutput()) return;;
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
            if(cdPut!=null) cd.destroy(cdPut);
            cdPut = null;
            valueChannelField = null;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#initialize()
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
            pvField = pvParent.findProperty("value");
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
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#start()
     */
    public void start() {
        super.start();
        if(super.getSupportState()!=SupportState.ready) return;
        process = processAccess.get();
        super.connect();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#stop()
     */
    public void stop() {
        if(cdPut!=null) cd.destroy(cdPut);
        cdPut = null;
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
        CDField[] cdFields = cd.getCDRecord().getCDStructure().getCDFields();
        CDField cdField = cdFields[0];
        PVField data = cdField.getPVField();
        Type targetType = data.getField().getType();
        PVField valuePVField = valueDBField.getPVField();
        Field valueField = valuePVField.getField();
        Type valueType = valueField.getType();
        if(valueType.isScalar() && targetType.isScalar()) {
            convert.copyScalar(valuePVField,data);
            cdField.incrementNumPuts();
        } else if(targetType==Type.pvArray && valueType==Type.pvArray) {
            PVArray targetPVArray = (PVArray)data;
            PVArray valuePVArray = (PVArray)valuePVField;
                int arrayLength = valuePVArray.getLength();
            int num = convert.copyArray(valuePVArray,0,targetPVArray,0,arrayLength);
            if(num!=arrayLength) message(
                    "length " + arrayLength + " but only copied " + num,
                    MessageType.warning);
            cdField.incrementNumPuts();
        } else if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
            PVStructure targetPVStructure = (PVStructure)data;
            PVStructure valuePVStructure = (PVStructure)valuePVField;
            convert.copyStructure(valuePVStructure,targetPVStructure);
            cdField.incrementNumPuts();
        } else {
            message(
                "Logic error in OutputSupport: unsupported type",
                MessageType.fatalError);
            if(alarmSupport!=null) alarmSupport.setAlarm(
                    pvStructure.getFullFieldName()
                    + "Logic error in OutputSupport: unsupported type",
                    AlarmSeverity.major);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
        return;
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
     */
    public void processCallback() {
        cdPut.put(cd);            
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        if(requestResult!=RequestResult.success) {
            if(alarmSupport!=null) alarmSupport.setAlarm(
                    pvStructure.getFullFieldName() + ": put request failed",
                    AlarmSeverity.major);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDPutRequester#getDone(org.epics.ioc.util.RequestResult)
     */
    public void getDone(RequestResult requestResult) {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDPutRequester#putDone(org.epics.ioc.util.RequestResult)
     */
    public void putDone(RequestResult requestResult) {
        this.requestResult = requestResult;
        recordProcess.processContinue(this);
    }      
            
    private boolean prepareForOutput() {
        valueChannelField = channel.createChannelField(channelFieldName);
        if(valueChannelField==null) {
            message(channelFieldName + " does not exist ",MessageType.error);
            return false;
        }
        if(!checkCompatibility(valueChannelField.getField())) {
            valueChannelField = null;
            return false;
        }
        putFieldGroup = channel.createFieldGroup(this);
        
        putFieldGroup.addChannelField(valueChannelField); 
        cd = CDFactory.createCD(channel, putFieldGroup);
        cdPut = cd.createCDPut(this, process);
        return true;
    }
          
    private boolean checkCompatibility(Field targetField) {
        Type targetType = targetField.getType();
        PVField valuePVField = valueDBField.getPVField();
        Field valueField = valuePVField.getField();
        Type valueType = valueField.getType();
        if(valueType.isScalar() && targetType.isScalar()) {
            if(convert.isCopyScalarCompatible(targetField,valueField)) return true;
        } else if(valueType==Type.pvArray && targetType==Type.pvArray) {
            Array targetArray = (Array)targetField;
            Array valueArray = (Array)valueField;
            if(convert.isCopyArrayCompatible(targetArray,valueArray)) return true;
        } else if(valueType==Type.pvStructure && targetType==Type.pvStructure) {
            Structure targetStructure = (Structure)targetField;
            Structure valueStructure = (Structure)valueField;
            if(convert.isCopyStructureCompatible(targetStructure,valueStructure)) return true;
        }
        message("is not compatible with pvname " + pvnamePVString.get(),MessageType.error);
        return false;
    }
}
