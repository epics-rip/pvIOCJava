 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.ca.*;
import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.*;
import java.util.List;

/**
 * Factory to create link support.
 * @author mrk
 *
 */
public class CASupportFactory {
    /**
     * Create link support for Channel Access links.
     * @param dbStructure The field for which to create support.
     * @return A Support interface or null failure.
     */
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getSupportName();
        if(supportName.equals(processSupportName)) {
            return new ProcessSupport(dbStructure);
        } else if(supportName.equals(inputSupportName)) {
            return new InputSupport(dbStructure);
        } else if(supportName.equals(outputSupportName)) {
            return new OutputSupport(dbStructure);
        } else if(supportName.equals(monitorSupportName)) {
            return new MonitorSupport(dbStructure);
        } else if(supportName.equals(monitorNotifySupportName)) {
            return new MonitorNotifySupport(dbStructure);
        }
        dbStructure.getPVStructure().message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String processSupportName = "processSupport";
    private static final String inputSupportName = "inputSupport";
    private static final String outputSupportName = "outputSupport";
    private static final String monitorSupportName = "monitorSupport";
    private static final String monitorNotifySupportName = "monitorNotifySupport";

    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern periodPattern = Pattern.compile("[.]");
    
    private static PVInt getIndexField(PVStructure pvStructure,String fieldName) {
        Structure structure = pvStructure.getStructure();
        PVField[] pvFields = pvStructure.getPVFields();
        int index = structure.getFieldIndex(fieldName);
        if(index<0) {
            pvStructure.message("field " + fieldName + " does not exist", MessageType.error);
            return null;
        }
        PVField pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("field is not a structure", MessageType.error);
            return null;
        }
        pvStructure = (PVStructure)pvField;
        pvFields = pvStructure.getPVFields();
        structure = pvStructure.getStructure();
        index = structure.getFieldIndex("index");
        if(index<0) {
            pvStructure.message("field index does not exist", MessageType.error);
            return null;
        }
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvInt) {
            pvField.message("field is not an int", MessageType.error);
            return null;
        }
        return (PVInt)pvField;
    }
    
    private static class ProcessSupport extends AbstractSupport implements
    RecordProcessRequester,ProcessCallbackRequester,ProcessContinueRequester,
    ChannelProcessRequester,ChannelStateListener,ChannelFieldGroupListener
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private String channelRequesterName;
        private DBRecord dbRecord;
        private RecordProcess recordProcess;
        private AlarmSupport alarmSupport;
        private PVString pvnameAccess;
        
        private boolean isLocal;
        private boolean isConnected = false;

        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult requestResult = null;
        
        private boolean isRecordProcessRequester = false;
        private TimeStamp timeStamp = new TimeStamp();
        private DBRecord targetDBRecord = null;
        private RecordProcess targetRecordProcess = null;
      
        private Channel channel = null;
        private ChannelProcess channelProcess = null;

        private ProcessSupport(DBStructure dbStructure) {
            super(processSupportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
            channelRequesterName = pvStructure.getFullName();
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return channelRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,processSupportName)) return;
            dbRecord = dbStructure.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            pvnameAccess = pvStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,processSupportName)) return;
            isConnected = false;
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                pvStructure.message("pvname is not defined",MessageType.error);
                return;
            }
            String[]pvname = periodPattern.split(name,2);
            String recordName = pvname[0];
            IOCDB iocdb = IOCDBFactory.getMaster();
            targetDBRecord = iocdb.findRecord(recordName);
            if(targetDBRecord!=null) {
                targetRecordProcess = targetDBRecord.getRecordProcess();
                isRecordProcessRequester = targetRecordProcess.setRecordProcessRequester(this);
                if(!isRecordProcessRequester) {
                    if(!targetRecordProcess.canProcessSelf()) {
                        pvStructure.message("can not be recordProcessRequester",MessageType.warning);
                        return;
                    }
                }
                isLocal = true;
                isConnected = true;
            } else {
                isLocal = false;
                channel = ChannelFactory.createChannel(recordName, this, false);
                if(channel==null) {
                    pvStructure.message(
                        "Failed to create channel for " + recordName,
                        MessageType.error);
                    return;
                } 
                channelProcess = channel.createChannelProcess(this,true);
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(isLocal) {
                if(isRecordProcessRequester) {
                    targetRecordProcess.releaseRecordProcessRequester(this);
                }
                targetDBRecord = null;
                targetRecordProcess = null;
            } else {
                channelProcess = null;
                channel.destroy();
                channel = null;
            }
            setSupportState(SupportState.readyForStart);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,processSupportName)) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequester==null) {
                throw new IllegalStateException("supportProcessRequester is null");
            }
            this.supportProcessRequester = supportProcessRequester;
            if(!isConnected) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not connected",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.success);
            }
            if(isRecordProcessRequester) recordProcess.getTimeStamp(timeStamp);
            recordProcess.requestProcessCallback(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                if(isRecordProcessRequester) {
                    if(!targetRecordProcess.process(this, false,timeStamp)) {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvStructure.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                        return;
                    }
                } else {
                    if(!targetRecordProcess.processSelfRequest(this)) {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvStructure.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                        return;
                    }
                    targetRecordProcess.processSelfProcess(this, false);
                }
            } else {
                channelProcess.process();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProcessRequester#processDone(org.epics.ioc.util.RequestResult)
         */
        public void processDone(RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message,MessageType messageType) {
            dbRecord.lock();
            try {
                pvStructure.message(message, messageType);
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            dbRecord.lock();
            try {
                this.isConnected = isConnected;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void disconnect(Channel c) {
            dbRecord.lock();
            try {
                SupportState supportState = dbRecord.getDBStructure().getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // nothing to do         
        }
    }
    
    private static class InputSupport extends AbstractSupport implements
    RecordProcessRequester,ProcessCallbackRequester,ProcessContinueRequester,
    ChannelGetRequester,ChannelFieldGroupListener,ChannelStateListener
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private String channelRequesterName;
        private DBRecord dbRecord;
        private RecordProcess recordProcess;
        private AlarmSupport alarmSupport;
        private PVString pvnameAccess;
        private PVBoolean processAccess;
        private PVBoolean inheritSeverityAccess;
        
        private DBField valueDBField;
        
        private boolean process = false;
        private boolean inheritSeverity = false;
        private boolean isLocal;
        private boolean isConnected = false;
        
        private SupportProcessRequester supportProcessRequester;
        private RequestResult requestResult;   
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
             
        private TimeStamp timeStamp = new TimeStamp();
        private boolean isRecordProcessRequester = false;
        private DBRecord targetDBRecord;
        private RecordProcess targetRecordProcess;
        private PVField targetPVField;
        private PVInt targetPVSeverity;
        
        
        private String valueFieldName;
        private Channel channel;
        private ChannelGet channelGet;      
        private ChannelField valueChannelField;
        private ChannelField severityField;
        private ChannelFieldGroup channelFieldGroup;
        private int arrayLength = 0;
        private int arrayOffset = 0;
        
        private InputSupport(DBStructure dbStructure) {
            super(inputSupportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
            channelRequesterName = 
                pvStructure.getFullName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return channelRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,inputSupportName)) return;
            PVField pvField = pvStructure.findPropertyViaParent("value");
            if(pvField==null) {
                pvStructure.message("value field not found", MessageType.error);
                return;
            }
            valueDBField = dbStructure.getDBRecord().findDBField(pvField);
            dbRecord = dbStructure.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            pvnameAccess = pvStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            processAccess = pvStructure.getBooleanField("process");
            if(processAccess==null) return;
            inheritSeverityAccess = pvStructure.getBooleanField("inheritSeverity");
            if(inheritSeverityAccess==null) return;
            setSupportState(SupportState.readyForStart);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,inputSupportName)) return;
            isConnected = false; 
            inheritSeverity = inheritSeverityAccess.get();
            process = processAccess.get();
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                pvStructure.message("pvname is not defined",MessageType.error);
                return;
            }
            String[]pvname = periodPattern.split(name,2);
            String recordName = pvname[0];
            String fieldName = null;
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }           
            IOCDB iocdb = IOCDBFactory.getMaster();
            targetDBRecord = iocdb.findRecord(recordName);
            if(targetDBRecord!=null) {
                PVRecord pvRecord = targetDBRecord.getPVRecord();
                targetPVField = pvRecord.findProperty(fieldName);
                if(targetPVField==null) {
                    pvStructure.message("field " + fieldName + " not found", MessageType.error);
                    return;
                }
                if(inheritSeverity) {
                    PVField pvField = targetPVField.findProperty("alarm.severity.index");
                    if(pvField!=null) {
                        targetPVSeverity = (PVInt)pvField;
                    } else {
                        pvStructure.message("severity field not found",MessageType.error);
                        return;
                    }
                }
                if(process) {
                    targetRecordProcess = targetDBRecord.getRecordProcess();
                    isRecordProcessRequester = targetRecordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!targetRecordProcess.canProcessSelf()) {
                            pvStructure.message("can not process record",MessageType.warning);
                            targetDBRecord = null;
                            targetRecordProcess = null;
                            return;
                        }
                    }
                }
                if(!checkCompatibility(targetPVField.getField())) {
                    targetDBRecord = null;
                    return;
                }
                isLocal = true;
                isConnected = true;
            } else {
                valueFieldName = fieldName;
                isLocal = false;
                channel = ChannelFactory.createChannel(recordName,this, false);
                if(channel==null) {
                    pvStructure.message(
                            "Failed to create channel for " + recordName,
                            MessageType.error);
                    return;
                }
                if(channel.isConnected()) {
                    boolean prepareReturn = prepareForInput();
                    if(prepareReturn) {
                        isConnected = true;
                    } else {
                        isConnected = false;
                    }
                } else {
                    isConnected = false;
                }
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(isLocal) {
                if(isRecordProcessRequester) targetRecordProcess.releaseRecordProcessRequester(this);
                targetDBRecord = null;
                targetRecordProcess = null;
                isRecordProcessRequester = false;
            } else {
                channelGet = null;
                severityField = null;
                channelFieldGroup = null;
                channel.destroy();
                channel = null;
            }
            setSupportState(SupportState.readyForStart);
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,inputSupportName + ".process")) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
            }
            if(supportProcessRequester==null) {
                throw new IllegalStateException("supportProcessRequester is null");
            }
            this.supportProcessRequester = supportProcessRequester;
            requestResult = RequestResult.success;
            if(!isLocal) {
                if(!isConnected) {
                    if(alarmSupport!=null) alarmSupport.setAlarm(
                            pvStructure.getFullFieldName() + " not connected",
                            AlarmSeverity.major);
                    supportProcessRequester.supportProcessDone(RequestResult.success);
                    return;
                }
                recordProcess.requestProcessCallback(this);
                return;
            }
            if(isLocal&& !process) {
                getLocalData();
                processContinue();
                return;
            }
            if(isRecordProcessRequester) recordProcess.getTimeStamp(timeStamp);
            recordProcess.requestProcessCallback(this);
            return;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                if(isRecordProcessRequester){
                    if(!targetRecordProcess.process(this, false,timeStamp)) {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvStructure.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                    }
                } else if(process){
                    if(!targetRecordProcess.processSelfRequest(this)) {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvStructure.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                        return;
                    }
                    targetRecordProcess.processSelfProcess(this, false);
                    return;
                }
                getLocalData();
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            } else {
                arrayLength = -1;
                channelGet.get();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#ready()
         */
        public RequestResult ready() {
            throw new IllegalStateException(channelRequesterName + " why was ready called?");      
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
                if(alarmSupport!=null) alarmSupport.setAlarm("linkAlarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
             }
             supportProcessRequester.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
            getLocalData();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            dbRecord.lock();
            try {
                if(isConnected==this.isConnected) return;
                this.isConnected = false;
            } finally {
                dbRecord.unlock();
            }
            boolean prepareReturn = true;
            if(isConnected) {
                prepareReturn = prepareForInput();
                channelGet = channel.createChannelGet(channelFieldGroup,this, process,true);
                if(channelGet==null) {
                    isConnected = false;
                }
            }
            dbRecord.lock();
            try {
                if(isConnected&&!prepareReturn) isConnected = false;
                this.isConnected = isConnected;
                if(!isConnected) {
                    valueChannelField = null;
                    severityField = null;
                    channelFieldGroup = null;
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void disconnect(Channel c) {
            dbRecord.lock();
            try {
                SupportState supportState = dbRecord.getDBStructure().getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequester#nextGetField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
         */
        public boolean nextGetField(ChannelField channelField,PVField data) {
            if(channelField==severityField) {
                PVInt pvInt = (PVInt)data;
                int index = pvInt.get();
                alarmSeverity = AlarmSeverity.getSeverity(index);
                return false;
            }
            if(channelField!=valueChannelField) {
                pvStructure.message(
                    "Logic error in InputSupport field!=valueChannelField",
                    MessageType.fatalError);
            }
            Type targetType = data.getField().getType();
            PVField pvField = valueDBField.getPVField();
            Field valueField = pvField.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && targetType.isScalar()) {
                convert.copyScalar(data,pvField);
                valueDBField.postPut();
                return false;
            }
            if(targetType==Type.pvArray && valueType==Type.pvArray) {
                PVArray targetPVArray = (PVArray)data;
                PVArray valuePVArray = (PVArray)valueDBField;
                if(arrayLength<0) {
                    arrayLength = targetPVArray.getLength();
                    arrayOffset = 0;
                }
                int num = convert.copyArray(targetPVArray,arrayOffset,
                    valuePVArray,arrayOffset,arrayLength-arrayOffset);
                arrayOffset += num;
                if(arrayOffset<arrayLength) return true;
                valueDBField.postPut();
                return false;
            }
            if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                PVStructure targetPVStructure = (PVStructure)data;
                PVStructure valuePVStructure = (PVStructure)valueDBField;
                convert.copyStructure(targetPVStructure,valuePVStructure);
                valueDBField.postPut();
                return false;
            }
            pvStructure.message(
                    "Logic error in InputSupport: unsupported type",
                    MessageType.fatalError);
            return false;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequester#nextDelayedGetField(org.epics.ioc.pv.PVField)
         */
        public boolean nextDelayedGetField(PVField pvField) {
            // nothing to do
            return false;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequester#getDone(org.epics.ioc.util.RequestResult)
         */
        public void getDone(RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }      
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message,MessageType messageType) {
            dbRecord.lock();
            try {
                pvStructure.message(message, messageType);
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
        
        private void getLocalData() {
            dbRecord.lockOtherRecord(targetDBRecord);
            try {
                if(inheritSeverity) {
                    int index = targetPVSeverity.get();
                    alarmSeverity = AlarmSeverity.getSeverity(index);
                }
                Type targetType = targetPVField.getField().getType();
                PVField valuePVField = valueDBField.getPVField();
                Field valueField = valuePVField.getField();
                Type valueType = valueField.getType();
                if(valueType.isScalar() && targetType.isScalar()) {
                    convert.copyScalar(targetPVField,valuePVField);
                    valueDBField.postPut();
                    return;
                }
                if(targetType==Type.pvArray && valueType==Type.pvArray) {
                    PVArray targetPVArray = (PVArray)targetPVField;
                    PVArray valuePVArray = (PVArray)valueDBField.getPVField();
                    convert.copyArray(targetPVArray,0,
                        valuePVArray,0,targetPVArray.getLength());
                    valueDBField.postPut();
                    return;
                }
                if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                    PVStructure targetPVStructure = (PVStructure)targetPVField;
                    PVStructure valuePVStructure = (PVStructure)valueDBField.getPVField();
                    convert.copyStructure(targetPVStructure,valuePVStructure);
                    valueDBField.postPut();
                    return;
                }
                pvStructure.message(
                        "Logic error in InputSupport: unsupported type",
                        MessageType.fatalError);
            } finally {
                targetDBRecord.unlock();
            }
        }
        
        private boolean prepareForInput() {
            valueChannelField = channel.findField(valueFieldName);
            if(valueChannelField==null) {
                message(valueFieldName + " does not exist ",MessageType.error);
                return false;
            }
            if(!checkCompatibility(valueChannelField.getField())) {
                valueChannelField = null;
                return false;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(valueChannelField);
            if(inheritSeverity) {
                severityField = channel.findField("alarm.severity.index");
                if(severityField==null) {
                    channelFieldGroup = null;
                    valueChannelField = null;
                    message("severity.index does not exist ",MessageType.error);
                    return false;
                }
                Type type = severityField.getField().getType();
                if(type!=Type.pvInt) {
                    channelFieldGroup = null;
                    valueChannelField = null;
                    message("severity.index is not an int ",MessageType.error);
                    return false;
                }
                channelFieldGroup.addChannelField(severityField);
            }
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
            message("is not compatible with pvname " + pvnameAccess.get(),MessageType.error);
            return false;
        }
    }
        
    private static class OutputSupport extends AbstractSupport implements
    RecordProcessRequester,ProcessCallbackRequester,ProcessContinueRequester,
    ChannelPutRequester,
    ChannelFieldGroupListener,ChannelStateListener
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private String channelRequesterName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private AlarmSupport alarmSupport;
        private PVString pvnameAccess = null;
        private PVBoolean processAccess = null;
        
        private DBField valueDBField = null;
        
        private boolean process = false;
        private boolean isLocal;
        private boolean isConnected = false;
        
        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult requestResult = null;   
             
        private TimeStamp timeStamp = new TimeStamp();
        private DBRecord targetDBRecord = null;
        private boolean isRecordProcessRequester = false;
        private RecordProcess targetRecordProcess = null;
        private PVField targetPVField = null;
        private DBField targetDBField = null;
        
        
        private String valueFieldName = null;
        private Channel channel = null;
        private ChannelPut channelPut = null;
        private ChannelField valueChannelField = null;
        private ChannelFieldGroup putFieldGroup = null;
        private int arrayLength = 0;
        private int arrayOffset = 0;
        
        private OutputSupport(DBStructure dbStructure) {
            super(inputSupportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
            channelRequesterName = 
                pvStructure.getFullName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return channelRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,outputSupportName)) return;
            dbRecord = dbStructure.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            pvnameAccess = pvStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            processAccess = pvStructure.getBooleanField("process");
            if(processAccess==null) return;
            setSupportState(SupportState.readyForStart);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,outputSupportName)) return;
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
                return;
            }
            valueDBField = dbStructure.getDBRecord().findDBField(pvField);
            isConnected = false;
            process = processAccess.get();
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                pvStructure.message("pvname is not defined",MessageType.error);
                return;
            }
            String[]pvname = periodPattern.split(name,2);
            String recordName = pvname[0];
            String fieldName = null;
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }           
            IOCDB iocdb = IOCDBFactory.getMaster();
            targetDBRecord = iocdb.findRecord(recordName);
            if(targetDBRecord!=null) {        
                PVRecord targetPVRecord = targetDBRecord.getPVRecord();
                targetPVField = targetPVRecord.findProperty(fieldName);
                if(targetPVField==null) {
                    pvStructure.message("field " + fieldName + " not found", MessageType.error);
                    return;
                }
                targetDBField = targetDBRecord.findDBField(targetPVField);
                if(process) {
                    targetRecordProcess = targetDBRecord.getRecordProcess();
                    isRecordProcessRequester = targetRecordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!targetRecordProcess.canProcessSelf()) {
                            pvStructure.message("can not process",MessageType.warning);
                            return;
                        }
                    }
                }
                if(!checkCompatibility(targetPVField.getField())) {
                    targetDBRecord = null;
                    return;
                }
                isLocal = true;
                isConnected = true;
            } else {
                valueFieldName = fieldName;
                isLocal = false;
                channel = ChannelFactory.createChannel(recordName,this, false);
                if(channel==null) {
                    pvStructure.message(
                            "Failed to create channel for " + recordName,
                            MessageType.error);
                    return;
                }
                if(channel.isConnected()) {
                    boolean prepareReturn = prepareForOutput();
                    if(prepareReturn) {
                        isConnected = true;
                    } else {
                        isConnected = false;
                    }
                } else {
                    isConnected = false;
                }
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(isLocal) {
                if(isRecordProcessRequester) targetRecordProcess.releaseRecordProcessRequester(this);
                targetDBRecord = null;
                targetRecordProcess = null;
            } else {
                channelPut = null;
                putFieldGroup = null;
                channel.destroy();
                channel = null;
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,outputSupportName + ".process")) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
            }
            if(supportProcessRequester==null) {
                throw new IllegalStateException("supportProcessRequester is null");
            }
            this.supportProcessRequester = supportProcessRequester;
            if(!isLocal) {
                if(!isConnected) {
                    if(alarmSupport!=null) alarmSupport.setAlarm(
                            pvStructure.getFullFieldName() + " not connected",
                            AlarmSeverity.major);
                    supportProcessRequester.supportProcessDone(RequestResult.success);
                    return;
                }
                recordProcess.requestProcessCallback(this);
                return;
            }
            if(process) {
                if(isRecordProcessRequester) recordProcess.getTimeStamp(timeStamp);
                recordProcess.requestProcessCallback(this);
                return;
            }
            putLocalData();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                if(isRecordProcessRequester) {
                    if(!targetRecordProcess.setActive(this)) {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvStructure.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                        return;
                    }
                } else if(process){
                    if(!targetRecordProcess.processSelfRequest(this)) {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvStructure.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                        return;
                    }
                    targetRecordProcess.processSelfSetActive(this);
                }
                putLocalData();
                if(isRecordProcessRequester) {
                    targetRecordProcess.process(this, false, timeStamp);
                } else if(process){
                    targetRecordProcess.processSelfProcess(this, false);
                }
            } else {
                arrayLength = -1;
                channelPut.put();
            }
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
             supportProcessRequester.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            dbRecord.lock();
            try {
                if(isConnected==this.isConnected) return;
                this.isConnected = false;
            } finally {
                dbRecord.unlock();
            }
            boolean prepareReturn = true;
            if(isConnected) {
                prepareReturn = prepareForOutput();
            }
            dbRecord.lock();
            try {
                if(isConnected&&!prepareReturn) isConnected = false;
                this.isConnected = isConnected;
                if(!isConnected) {
                    valueChannelField = null;
                    putFieldGroup = null;
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void disconnect(Channel c) {
            dbRecord.lock();
            try {
                SupportState supportState = dbRecord.getDBStructure().getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequester#nextPutField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
         */
        public boolean nextPutField(ChannelField channelField,PVField data) {
            if(channelField!=valueChannelField) {
                pvStructure.message(
                    "Logic error in InputSupport field!=valueChannelField",
                    MessageType.fatalError);
            }
            Type targetType = data.getField().getType();
            PVField valuePVField = valueDBField.getPVField();
            Field valueField = valuePVField.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && targetType.isScalar()) {
                convert.copyScalar(valuePVField,data);
                return false;
            }
            if(targetType==Type.pvArray && valueType==Type.pvArray) {
                PVArray targetPVArray = (PVArray)data;
                PVArray valuePVArray = (PVArray)valuePVField;
                if(arrayLength<0) {
                    arrayLength = targetPVArray.getLength();
                    arrayOffset = 0;
                    targetPVArray.setLength(arrayLength);
                }
                int num = convert.copyArray(valuePVArray,arrayOffset,
                    targetPVArray,arrayOffset,arrayLength-arrayOffset);
                arrayOffset += num;
                if(arrayOffset<arrayLength) return true;
                return false;
            }
            if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                PVStructure targetPVStructure = (PVStructure)data;
                PVStructure valuePVStructure = (PVStructure)valuePVField;
                convert.copyStructure(valuePVStructure,targetPVStructure);
                return false;
            }
            pvStructure.message(
                    "Logic error in InputSupport: unsupported type",
                    MessageType.fatalError);
            return false;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequester#nextDelayedPutField(org.epics.ioc.pv.PVField)
         */
        public boolean nextDelayedPutField(PVField field) {
            // nothing to do
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequester#putDone(org.epics.ioc.util.RequestResult)
         */
        public void putDone(RequestResult requestResult) {
            // nothing to do
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message,MessageType messageType) {
            dbRecord.lock();
            try {
                pvStructure.message(message, messageType);
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
        
        private void putLocalData() {
            dbRecord.lockOtherRecord(targetDBRecord);
            try {
                Type targetType = targetPVField.getField().getType();
                PVField valuePVField = valueDBField.getPVField();
                Field valueField = valuePVField.getField();
                Type valueType = valueField.getType();
                if(valueType.isScalar() && targetType.isScalar()) {
                    convert.copyScalar(valuePVField,targetPVField);
                    targetDBField.postPut();
                    return;
                }
                if(targetType==Type.pvArray && valueType==Type.pvArray) {
                    PVArray targetPVArray = (PVArray)targetPVField;
                    PVArray valuePVArray = (PVArray)valuePVField;
                    convert.copyArray(valuePVArray,0,
                        targetPVArray,0,valuePVArray.getLength());
                    targetDBField.postPut();
                    return;
                }
                if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                    PVStructure targetPVStructure = (PVStructure)targetPVField;
                    PVStructure valuePVStructure = (PVStructure)valuePVField;
                    convert.copyStructure(valuePVStructure,targetPVStructure);
                    targetDBField.postPut();
                    return;
                }
                pvStructure.message(
                        "Logic error in OutputSupport: unsupported type",
                        MessageType.fatalError);
            } finally {
                targetDBRecord.unlock();
            }
        }
        
        private boolean prepareForOutput() {
            valueChannelField = channel.findField(valueFieldName);
            if(valueChannelField==null) {
                message(valueFieldName + " does not exist ",MessageType.error);
                return false;
            }
            if(!checkCompatibility(valueChannelField.getField())) {
                valueChannelField = null;
                return false;
            }
            putFieldGroup = channel.createFieldGroup(this);
            putFieldGroup.addChannelField(valueChannelField);            
            channelPut = channel.createChannelPut(putFieldGroup,this, process,true);
            return true;
        }
              
        private boolean checkCompatibility(Field targetField) {
            Type targetType = targetField.getType();
            PVField valuePVField = valueDBField.getPVField();
            Field valueField = valuePVField.getField();
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
            message("is not compatible with pvname " + pvnameAccess.get(),MessageType.error);
            return false;
        }
    }
    
    private enum MonitorType {
        put,
        change,
        absoluteChange,
        percentageChange;
        public static MonitorType getType(int value) {
            switch(value) {
            case 0: return MonitorType.put;
            case 1: return MonitorType.change;
            case 2: return MonitorType.absoluteChange;
            case 3: return MonitorType.percentageChange;
            }
            throw new IllegalArgumentException("MonitorType.getType) "
                + ((Integer)value).toString() + " is not a valid MonitorType");
        }
    }
    
    private static class MonitorNotifySupport extends AbstractSupport implements
    RecordProcessRequester,
    ChannelStateListener,
    ChannelMonitorNotifyRequester
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private DBRecord dbRecord;
        private String channelRequesterName = null;
        private RecordProcess recordProcess = null;
        private AlarmSupport alarmSupport;     
        
        private PVString pvnameAccess = null;
        private PVInt monitorTypeAccess = null;
        private PVDouble deadbandAccess = null;
        private PVBoolean onlyWhileProcessingAccess = null;
        
        private String recordName = null;
        private String fieldName = null;
        private MonitorType monitorType = null;
        private double deadband = 0.0;
        private boolean onlyWhileProcessing = false;
              
        private Channel channel = null;
        private ChannelMonitor channelMonitor = null;
        private ChannelField dataField = null;
        
        private boolean isActive;
       
        private MonitorNotifySupport(DBStructure dbStructure) {
            super(monitorNotifySupportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
            dbRecord = dbStructure.getDBRecord();
            channelRequesterName = 
                pvStructure.getFullName();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return channelRequesterName;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            if(!recordProcess.setRecordProcessRequester(this)) {
                super.message("notifySupport but record already has recordProcessor", MessageType.error);
                return;
            }
            isActive = false;
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            pvnameAccess = pvStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            monitorTypeAccess = getIndexField(pvStructure,"type");
            if(monitorTypeAccess==null) return;
            deadbandAccess = pvStructure.getDoubleField("deadband");
            if(deadbandAccess==null) return;
            onlyWhileProcessingAccess = pvStructure.getBooleanField("onlyWhileProcessing");
            if(onlyWhileProcessingAccess==null) return;
            setSupportState(SupportState.readyForStart);  
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,monitorSupportName)) return;
            // split pvname into record name and rest of name
            String[]pvname = periodPattern.split(pvnameAccess.get(),2);
            recordName = pvname[0];
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }
            channel = ChannelFactory.createChannel(recordName,this, false);
            if(channel==null) {
                pvStructure.message(
                        "Failed to create channel for " + recordName,
                        MessageType.error);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            int index = monitorTypeAccess.get();
            monitorType = MonitorType.getType(index);
            deadband = deadbandAccess.get();
            onlyWhileProcessing = onlyWhileProcessingAccess.get();
            channelMonitor = channel.createChannelMonitor(onlyWhileProcessing,false);
            if(channel.isConnected()) {
                channelStart();
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(channel!=null) channel.destroy();
            channel = null;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            isActive = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!channel.isConnected()) {
                    if(dataField!=null) {
                        channelMonitor.stop();
                        dataField = null;
                    }
                    return;
                }
                channelStart();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void disconnect(Channel c) {
            dbRecord.lock();
            try {
                SupportState supportState = dbRecord.getDBStructure().getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorNotifyRequester#dataModified(org.epics.ioc.ca.Channel)
         */
        public void monitorEvent() {
            if(isActive) {
                dbRecord.lock();
                try {
                    alarmSupport.setAlarm(
                        "monitorNotify event but record already active", AlarmSeverity.minor);
                } finally {
                    dbRecord.unlock();
                }
                return;
            }
            recordProcess.process(this, false, null);
        } 
        
        private void channelStart() {
            dataField = channel.findField(fieldName);
            if(dataField==null) {
                pvStructure.message(
                    "fieldName " + fieldName
                    + " is not in record " + recordName,
                    MessageType.error);
                return;
            }            
            switch(monitorType) {
            case put:
                channelMonitor.lookForPut(dataField, true); break;
            case change:
                channelMonitor.lookForChange(dataField, true); break;
            case absoluteChange:
                channelMonitor.lookForAbsoluteChange(dataField, deadband); break;
            case percentageChange:
                channelMonitor.lookForPercentageChange(dataField, deadband); break;
            }
            String threadName = pvStructure.getFullName();
            channelMonitor.start((ChannelMonitorNotifyRequester)this, threadName, ScanPriority.low);
        }
    }
    
    private static class MonitorSupport extends AbstractSupport implements
    ChannelStateListener,
    ChannelFieldGroupListener,
    ChannelMonitorRequester,
    RecordProcessRequester
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        private DBRecord dbRecord = null;
        private String channelRequesterName = null;
        private RecordProcess recordProcess = null;
        private AlarmSupport alarmSupport;      
        
        private PVString pvnameAccess = null;
        private PVInt monitorTypeAccess = null;
        private PVDouble deadbandAccess = null;
        private PVBoolean onlyWhileProcessingAccess = null;
        private PVInt queueSizeAccess = null;
        private PVBoolean reportOverrunAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;   
        
        private DBField valueDBField = null;
        
        private String recordName = null;
        private String fieldName = null;
        private MonitorType monitorType = null;
        private double deadband = 0.0;
        private boolean onlyWhileProcessing = false;
        private int queueSize = 0;
        private boolean reportOverrun = false;
        private boolean isRecordProcessRequester = false;
        private boolean process = false;
        private boolean inheritSeverity = false;
        private boolean valueChanged = false;
              
        private Channel channel = null;
        private boolean isLocal = false;
        private DBRecord targetDBRecord = null;
        private ChannelMonitor channelMonitor = null;
        private ChannelField targetChannelField = null;
        private ChannelField severityChannelField = null;
        private ChannelFieldGroup channelFieldGroup = null;
        
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
        private int numberOverrun = 0;
        private ReentrantLock processLock = null;
        private Condition processCondition = null;
        private boolean processDone;
        
        private MonitorSupport(DBStructure dbStructure) {
            super(monitorSupportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
            dbRecord = dbStructure.getDBRecord();
            channelRequesterName = pvStructure.getFullName();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return channelRequesterName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            pvnameAccess = pvStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            monitorTypeAccess = getIndexField(pvStructure,"type");
            if(monitorTypeAccess==null) return;
            deadbandAccess = pvStructure.getDoubleField("deadband");
            if(deadbandAccess==null) return;
            onlyWhileProcessingAccess = pvStructure.getBooleanField("onlyWhileProcessing");
            if(onlyWhileProcessingAccess==null) return;
            queueSizeAccess = pvStructure.getIntField("queueSize");
            if(queueSizeAccess==null) return;
            reportOverrunAccess = pvStructure.getBooleanField("reportOverrun");
            if(reportOverrunAccess==null) return;
            processAccess = pvStructure.getBooleanField("process");
            if(processAccess==null) return;
            inheritSeverityAccess = pvStructure.getBooleanField("inheritSeverity");
            if(inheritSeverityAccess==null) return;
            setSupportState(SupportState.readyForStart);  
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,monitorSupportName)) return;
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
                return;
            }
            valueDBField = dbStructure.getDBRecord().findDBField(pvField);
            // split pvname into record name and rest of name
            String[]pvname = periodPattern.split(pvnameAccess.get(),2);
            recordName = pvname[0];
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }
            channel = ChannelFactory.createChannel(recordName,this, false);
            if(channel==null) {
                pvStructure.message(
                        "Failed to create channel for " + recordName,
                        MessageType.error);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            isLocal = channel.isLocal();
            if(isLocal) {
                IOCDB iocdb = IOCDBFactory.getMaster();
                targetDBRecord = iocdb.findRecord(recordName);
                if(targetDBRecord==null) {
                    throw new IllegalStateException("logic error"); 
                }
            }
            int index = monitorTypeAccess.get();
            monitorType = MonitorType.getType(index);
            deadband = deadbandAccess.get();
            onlyWhileProcessing = onlyWhileProcessingAccess.get();
            queueSize = queueSizeAccess.get();
            if(queueSize<=1) {
                pvStructure.message("queueSize being put to 2", MessageType.warning);
                queueSize = 2;
            }
            reportOverrun = reportOverrunAccess.get();
            process = processAccess.get();
            if(process) {
                isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                if(!isRecordProcessRequester) {
                    if(!recordProcess.canProcessSelf()) {
                        pvStructure.message("process may fail",
                                MessageType.warning);
                    }
                }
                processLock = new ReentrantLock();
                processCondition = processLock.newCondition();
            }
            inheritSeverity = inheritSeverityAccess.get();
            if(!process && inheritSeverity) {
                pvStructure.message("inheritSeverity ignored", MessageType.warning);
                inheritSeverity = false;
            }
            channelMonitor = channel.createChannelMonitor(onlyWhileProcessing,false);
            if(channel.isConnected()) {
                channelStart();
            }
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
            isRecordProcessRequester = false;
            if(channel!=null) channel.destroy();
            channel = null;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.SupportListener)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,monitorSupportName + ".process")) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(!channel.isConnected()) {
                if(alarmSupport!=null) alarmSupport.setAlarm("Support not connected",
                    AlarmSeverity.invalid);
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            if(valueChanged) valueDBField.postPut();
            if(alarmSeverity!=AlarmSeverity.none) {
                if(alarmSupport!=null) alarmSupport.setAlarm("link Alarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
            } else if(numberOverrun>0) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                    "missed " + Integer.toString(numberOverrun) + " notifications",
                    AlarmSeverity.none);
                numberOverrun = 0;
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!channel.isConnected()) {
                    severityChannelField = null;
                    targetChannelField = null;
                    channelFieldGroup = null;
                    return;
                }
                channelStart();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void disconnect(Channel c) {
            dbRecord.lock();
            try {
                SupportState supportState = dbRecord.getDBStructure().getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#dataOverrun(int)
         */
        public void dataOverrun(int number) {
            if(!reportOverrun) return;
            if(process) {
                numberOverrun = number;
                return;
            }
            dbRecord.lock();
            try {
                pvStructure.message(
                    "missed " + Integer.toString(number) + " notifications",
                    MessageType.warning);
            } finally {
                dbRecord.unlock();
            }
        }                       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorRequester#monitorData(org.epics.ioc.ca.CDField)
         */
        public void monitorCD(CD cD) {
            if(isLocal) {
                targetDBRecord.lockOtherRecord(dbRecord);
            } else {
                dbRecord.lock();
            }
            try {
                boolean valueChanged = false;
                ChannelFieldGroup channelFieldGroup = cD.getChannelFieldGroup();
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                CDStructure cdStructure = cD.getCDRecord().getCDStructure();
                CDField[] cdbFields = cdStructure.getCDFields();
                for(int i=0;i<cdbFields.length; i++) {
                    CDField cdField = cdbFields[i];
                    PVField targetPVField = cdField.getPVField();
                    ChannelField channelField = channelFieldList.get(i);
                    if(channelField==severityChannelField) {
                        PVInt targetPVInt = (PVInt)targetPVField;
                        alarmSeverity = AlarmSeverity.getSeverity(targetPVInt.get());
                        continue;
                    }
                    if(channelField!=targetChannelField) {
                        pvStructure.message(
                                "Logic error",
                                MessageType.fatalError);
                        continue;
                    }
                    Type targetType = channelField.getField().getType();
                    PVField valuePVField = valueDBField.getPVField();
                    Field valueField = valuePVField.getField();
                    Type valueType = valueField.getType();
                    if(valueType.isScalar() && targetType.isScalar()) {
                        convert.copyScalar(targetPVField,valuePVField);
                        valueChanged = true;
                        continue;
                    }
                    if(targetType==Type.pvArray && valueType==Type.pvArray) {
                        PVArray targetPVArray = (PVArray)targetPVField;
                        PVArray valuePVArray = (PVArray)valuePVField;
                        convert.copyArray(targetPVArray,0,
                            valuePVArray,0,targetPVArray.getLength());
                        valueChanged = true;
                        continue;
                    }
                    if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                        PVStructure targetPVStructure = (PVStructure)targetPVField;
                        PVStructure valuePVStructure = (PVStructure)valuePVField;
                        convert.copyStructure(targetPVStructure,valuePVStructure);
                        valueChanged = true;
                        continue;
                    }
                    pvStructure.message(
                            "Logic error in MonitorSupport: unsupported type",
                            MessageType.fatalError);
                }
                if(process) {
                    if(valueChanged) this.valueChanged = valueChanged;
                    if(isRecordProcessRequester) {
                        processDone = false;
                        recordProcess.process(this, false, null);
                    }
                } else if(valueChanged){
                    valueDBField.postPut();
                }
            } finally {
                dbRecord.unlock();
            }
            if(process && !processDone) {
                //  wait for completion
                try {
                    processLock.lock();
                    try {
                        while(!processDone) processCondition.await();
                    }finally {
                        processLock.unlock();
                    }
                } catch(InterruptedException e) {}
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
         */
        public void recordProcessComplete() {
            processLock.lock();
            try {
                processDone = true;
                processCondition.signal();
            } finally {
                processLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
        }
        
        private String checkCompatibility() {
            Type targetType = targetChannelField.getField().getType();
            PVField valuePVField = valueDBField.getPVField();
            Field valueField = valuePVField.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && targetType.isScalar()) {
                if(convert.isCopyScalarCompatible(targetChannelField.getField(),valueField)) return null;
            } else if(targetType==Type.pvArray && valueType==Type.pvArray) {
                Array targetArray = (Array)targetChannelField.getField();
                Array valueArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(targetArray,valueArray)) return null;
            } else if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure targetStructure = (Structure)targetChannelField;
                Structure valueStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(targetStructure,valueStructure)) return null;
            }
            String errorMessage = 
                "is not compatible with pvname " + pvnameAccess.get();
            channel = null;
            return errorMessage;
        }
        
        private void channelStart() {
            targetChannelField = channel.findField(fieldName);
            if(targetChannelField==null) {
                pvStructure.message(
                    "fieldName " + fieldName
                    + " is not in record " + recordName,
                    MessageType.error);
                return;
            }
            String errorMessage = checkCompatibility();
            if(errorMessage!=null) {
                pvStructure.message(errorMessage,MessageType.error);
                return;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(targetChannelField);           
            switch(monitorType) {
            case put:
                channelMonitor.lookForPut(targetChannelField, true); break;
            case change:
                channelMonitor.lookForChange(targetChannelField, true); break;
            case absoluteChange:
                channelMonitor.lookForAbsoluteChange(targetChannelField, deadband); break;
            case percentageChange:
                channelMonitor.lookForPercentageChange(targetChannelField, deadband); break;
            }
            if(inheritSeverityAccess.get()) {
                severityChannelField = channel.findField("alarm.severity.index");
                if(severityChannelField!=null) {
                    channelFieldGroup.addChannelField(severityChannelField);
                    channelMonitor.lookForPut(severityChannelField, true);
                } else {
                    severityChannelField = null;
                }
            }
            String threadName = pvStructure.getFullName();
            channelMonitor.start((ChannelMonitorRequester)this, queueSize, threadName, ScanPriority.low);
        }
    }
}
