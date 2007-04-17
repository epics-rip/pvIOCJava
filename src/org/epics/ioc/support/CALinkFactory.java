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
public class CALinkFactory {
    /**
     * Create link support for Channel Access links.
     * @param dbLink The field for which to create support.
     * @return A LinkSupport interface or null failure.
     */
    public static LinkSupport create(DBLink dbLink) {
        String supportName = dbLink.getSupportName();
        if(supportName.equals(processLinkSupportName)) {
            return new ProcessLink(dbLink);
        } else if(supportName.equals(inputLinkSupportName)) {
            return new InputLink(dbLink);
        } else if(supportName.equals(outputLinkSupportName)) {
            return new OutputLink(dbLink);
        } else if(supportName.equals(monitorLinkSupportName)) {
            return new MonitorLink(dbLink);
        } else if(supportName.equals(monitorNotifyLinkSupportName)) {
            return new MonitorNotifyLink(dbLink);
        }
        dbLink.getPVLink().message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String processLinkSupportName = "processLink";
    private static final String inputLinkSupportName = "inputLink";
    private static final String outputLinkSupportName = "outputLink";
    private static final String monitorLinkSupportName = "monitorLink";
    private static final String monitorNotifyLinkSupportName = "monitorNotifyLink";

    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern periodPattern = Pattern.compile("[.]");
    
    private static class ProcessLink extends AbstractLinkSupport implements
    RecordProcessRequester,ProcessCallbackRequester,ProcessContinueRequester,
    ChannelProcessRequester,
    ChannelGetRequester,ChannelStateListener,ChannelFieldGroupListener
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private String channelRequesterName;
        private DBRecord dbRecord;
        private RecordProcess recordProcess;
        private AlarmSupport alarmSupport;
        private PVStructure configStructure;
        private PVString pvnameAccess;
        private PVBoolean inheritSeverityAccess;
        
        private boolean inheritSeverity = false;
        private boolean isLocal;
        private boolean isConnected = false;

        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult requestResult = null;
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
        
        private boolean isRecordProcessRequester = false;
        private TimeStamp timeStamp = new TimeStamp();
        private DBRecord targetDBRecord = null;
        private RecordProcess targetRecordProcess = null;
        private PVEnum targetPVSeverity = null;
      
        private Channel channel = null;
        private ChannelProcess channelProcess = null;
        private ChannelGet channelGet = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup channelFieldGroup = null;

        private ProcessLink(DBLink dbLink) {
            super(processLinkSupportName,dbLink);
            this.dbLink = dbLink;
            pvLink = dbLink.getPVLink();
            channelRequesterName = pvLink.getFullName();
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
            if(!super.checkSupportState(SupportState.readyForInitialize,processLinkSupportName)) return;
            dbRecord = dbLink.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
            configStructure = super.getConfigStructure("processLink", true);
            if(configStructure==null) return;
            pvnameAccess = configStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            inheritSeverityAccess = configStructure.getBooleanField("inheritSeverity");
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
            if(!super.checkSupportState(SupportState.readyForStart,processLinkSupportName)) return;
            isConnected = false;
            inheritSeverity = inheritSeverityAccess.get();
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                pvLink.message("pvname is not defined",MessageType.error);
                return;
            }
            String[]pvname = periodPattern.split(name,2);
            String recordName = pvname[0];
            IOCDB iocdb = IOCDBFactory.getMaster();
            targetDBRecord = iocdb.findRecord(recordName);
            if(targetDBRecord!=null) {
                if(inheritSeverity) {
                    PVAccess pvAccess = PVAccessFactory.createPVAccess(targetDBRecord.getPVRecord());
                    if(pvAccess.findField("severity")==AccessSetResult.thisRecord) {
                        targetPVSeverity = (PVEnum)pvAccess.getField();
                    } else {
                        pvLink.message("severity field not found",MessageType.error);
                        return;
                    }
                }
                targetRecordProcess = targetDBRecord.getRecordProcess();
                isRecordProcessRequester = targetRecordProcess.setRecordProcessRequester(this);
                if(!isRecordProcessRequester) {
                    if(!targetRecordProcess.canProcessSelf()) {
                        pvLink.message("can not be recordProcessRequester",MessageType.warning);
                        return;
                    }
                }
                isLocal = true;
                isConnected = true;
            } else {
                isLocal = false;
                channel = ChannelFactory.createChannel(recordName, this, false);
                if(channel==null) {
                    pvLink.message(
                        "Failed to create channel for " + recordName,
                        MessageType.error);
                    return;
                }
                if(inheritSeverity) {
                    channelGet = channel.createChannelGet(null, this, true, true);
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
                } else {
                    channelProcess = channel.createChannelProcess(this,true);
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
                if(isRecordProcessRequester) {
                    targetRecordProcess.releaseRecordProcessRequester(this);
                }
                targetDBRecord = null;
                targetRecordProcess = null;
            } else {
                channelProcess = null;
                channelGet = null;
                severityField = null;
                channelFieldGroup = null;
                channel.destroy();
                channel = null;
            }
            setSupportState(SupportState.readyForStart);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,processLinkSupportName + ".process")) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvLink.getFullFieldName() + " not ready",
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
                        pvLink.getFullFieldName() + " not connected",
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
                    targetRecordProcess.process(this, false,timeStamp);
                } else {
                    if(targetRecordProcess.processSelfRequest(this)) {
                        targetRecordProcess.processSelfProcess(this, false);
                    } else {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvLink.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                    }
                }
            } else {
                if(inheritSeverity) {
                    channelGet.get();
                } else {
                    channelProcess.process();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
            if(inheritSeverity) {
                if(requestResult==RequestResult.success) {
                    int index = targetPVSeverity.getIndex();
                    alarmSeverity = AlarmSeverity.getSeverity(index);
                } else {
                    alarmSeverity = AlarmSeverity.invalid;
                }
            }
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
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
               if(alarmSupport!=null) alarmSupport.setAlarm("linkAlarm", alarmSeverity);
               alarmSeverity = AlarmSeverity.none;
            }
            supportProcessRequester.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            // nothing to do
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequester#nextGetField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
         */
        public boolean nextGetField(ChannelField channelField, PVField pvField) {
            if(channelField!=severityField) {
                throw new IllegalStateException(channelRequesterName + "Logic error");  
            }
            PVEnum pvEnum = (PVEnum)channelField;
            int index = pvEnum.getIndex();
            alarmSeverity = AlarmSeverity.getSeverity(index);
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequester#nextDelayedGetField(org.epics.ioc.pv.PVField)
         */
        public boolean nextDelayedGetField(PVField pvField) {
            // Nothing to do.
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
                pvLink.message(message, messageType);
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
                if(isConnected==this.isConnected) return;
                this.isConnected = false;
            } finally {
                dbRecord.unlock();
            }
            boolean prepareReturn = true;
            if(isConnected) {
                prepareReturn = prepareForInput();
            }
            dbRecord.lock();
            try {
                if(isConnected&&!prepareReturn) isConnected = false;
                this.isConnected = isConnected;
                if(!isConnected) {
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
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // nothing to do         
        }
        
        private boolean prepareForInput() {
            ChannelFindFieldResult result = channel.findField("severity");
            if(result!=ChannelFindFieldResult.thisChannel) {
                message("field severity does not exist",MessageType.error);
                return false;
            }
            severityField = channel.getChannelField();
            Type type = severityField.getField().getType();
            if(type!=Type.pvEnum) {
                severityField = null;
                message("field severity is not an enum",MessageType.error);
                return false;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(severityField);
            return true;
        }
    }
    
    private static class InputLink extends AbstractLinkSupport implements
    RecordProcessRequester,ProcessCallbackRequester,ProcessContinueRequester,
    ChannelGetRequester,ChannelFieldGroupListener,ChannelStateListener
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private String channelRequesterName;
        private DBRecord dbRecord;
        private RecordProcess recordProcess;
        private AlarmSupport alarmSupport;
        private PVStructure configStructure;
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
        private PVEnum targetPVSeverity;
        
        
        private String valueFieldName;
        private Channel channel;
        private ChannelGet channelGet;      
        private ChannelField valueChannelField;
        private ChannelField severityField;
        private ChannelFieldGroup channelFieldGroup;
        private int arrayLength = 0;
        private int arrayOffset = 0;
        
        private InputLink(DBLink dbLink) {
            super(inputLinkSupportName,dbLink);
            this.dbLink = dbLink;
            pvLink = dbLink.getPVLink();
            channelRequesterName = 
                pvLink.getFullName();
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
            if(!super.checkSupportState(SupportState.readyForInitialize,inputLinkSupportName)) return;
            dbRecord = dbLink.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
            configStructure = super.getConfigStructure("inputLink", true);
            if(configStructure==null) return;
            pvnameAccess = configStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            processAccess = configStructure.getBooleanField("process");
            if(processAccess==null) return;
            inheritSeverityAccess = configStructure.getBooleanField("inheritSeverity");
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
            if(!super.checkSupportState(SupportState.readyForStart,inputLinkSupportName)) return;
            isConnected = false;
            if(valueDBField==null) {
                pvLink.message(
                    "Logic Error: InputLink.start called before setField",
                    MessageType.error);
                setSupportState(SupportState.zombie);
                return;
            }
            inheritSeverity = inheritSeverityAccess.get();
            process = processAccess.get();
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                pvLink.message("pvname is not defined",MessageType.error);
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
            forever:
            while(true) {
                DBRecord dbRecord = iocdb.findRecord(recordName);
                if(dbRecord==null) {
                    targetDBRecord = null;
                    break;
                }
                PVAccess pvAccess = PVAccessFactory.createPVAccess(dbRecord.getPVRecord());
                switch(pvAccess.findField(fieldName)) {
                case otherRecord:
                    recordName = pvAccess.getOtherRecord();
                    fieldName = pvAccess.getOtherField();
                    break;
                case thisRecord:
                    targetDBRecord = iocdb.findRecord(recordName);
                    if(targetDBRecord==null) {
                        throw new IllegalStateException(channelRequesterName + "logic error?"); 
                    }
                    targetPVField = pvAccess.getField();
                    if(targetPVField==null) {
                        throw new IllegalStateException(channelRequesterName + "logic error?"); 
                    }
                    if(inheritSeverity) {
                        pvAccess.findField("");
                        if(pvAccess.findField("severity")==AccessSetResult.thisRecord) {
                            targetPVSeverity = (PVEnum)pvAccess.getField();
                        } else {
                            pvLink.message("severity field not found",MessageType.error);
                            return;
                        }
                    }
                    break forever;
                case notFound:
                    targetDBRecord = null;
                    break forever;
                }
            }
            if(targetDBRecord!=null) {
                if(process) {
                    targetRecordProcess = targetDBRecord.getRecordProcess();
                    isRecordProcessRequester = targetRecordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!targetRecordProcess.canProcessSelf()) {
                            pvLink.message("can not process record",MessageType.warning);
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
                    pvLink.message(
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
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            valueDBField = dbField;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,inputLinkSupportName + ".process")) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvLink.getFullFieldName() + " not ready",
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
                            pvLink.getFullFieldName() + " not connected",
                            AlarmSeverity.major);
                    supportProcessRequester.supportProcessDone(RequestResult.success);
                    return;
                }
                recordProcess.requestProcessCallback(this);
                return;
            }
            if(isLocal&& !process) {
                getLocalData();
                supportProcessRequester.supportProcessDone(RequestResult.success);
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
                    targetRecordProcess.process(this, false,timeStamp);
                } else {
                    if(targetRecordProcess.processSelfRequest(this)) {
                        targetRecordProcess.processSelfProcess(this, false);
                    } else {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvLink.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        getLocalData();
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                    }
                }
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
                PVEnum pvEnum = (PVEnum)data;
                int index = pvEnum.getIndex();
                alarmSeverity = AlarmSeverity.getSeverity(index);
                return false;
            }
            if(channelField!=valueChannelField) {
                pvLink.message(
                    "Logic error in InputLink field!=valueChannelField",
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
            pvLink.message(
                    "Logic error in InputLink: unsupported type",
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
                pvLink.message(message, messageType);
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
                    int index = targetPVSeverity.getIndex();
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
                pvLink.message(
                        "Logic error in InputLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                targetDBRecord.unlock();
            }
        }
        
        private boolean prepareForInput() {
            ChannelFindFieldResult result = channel.findField(valueFieldName);
            if(result!=ChannelFindFieldResult.thisChannel) {
                message(valueFieldName + " does not exist ",MessageType.error);
                return false;
            }
            valueChannelField = channel.getChannelField();
            if(!checkCompatibility(valueChannelField.getField())) {
                valueChannelField = null;
                return false;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(valueChannelField);
            if(inheritSeverity) {
                result = channel.findField("severity");
                if(result!=ChannelFindFieldResult.thisChannel) {
                    channelFieldGroup = null;
                    valueChannelField = null;
                    message(" severity does not exist ",MessageType.error);
                    return false;
                }
                severityField = channel.getChannelField();
                Type type = severityField.getField().getType();
                if(type!=Type.pvEnum) {
                    channelFieldGroup = null;
                    valueChannelField = null;
                    message(" severity is not an enum ",MessageType.error);
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
        
    private static class OutputLink extends AbstractLinkSupport implements
    RecordProcessRequester,ProcessCallbackRequester,ProcessContinueRequester,
    ChannelPutGetRequester,
    ChannelFieldGroupListener,ChannelStateListener
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private String channelRequesterName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private AlarmSupport alarmSupport;
        private PVStructure configStructure = null;
        private PVString pvnameAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private DBField valueDBField = null;
        
        private boolean process = false;
        private boolean inheritSeverity = false;
        private boolean isLocal;
        private boolean isConnected = false;
        
        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult requestResult = null;   
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
             
        private TimeStamp timeStamp = new TimeStamp();
        private DBRecord targetDBRecord = null;
        private boolean isRecordProcessRequester = false;
        private RecordProcess targetRecordProcess = null;
        private PVField targetPVField = null;
        private DBField targetDBField = null;
        private PVEnum targetPVSeverity = null;
        
        
        private String valueFieldName = null;
        private Channel channel = null;
        private ChannelPut channelPut = null;
        private ChannelPutGet channelPutGet = null;
        private ChannelField valueChannelField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup putFieldGroup = null;
        private ChannelFieldGroup getFieldGroup = null;
        private int arrayLength = 0;
        private int arrayOffset = 0;
        
        private OutputLink(DBLink dbLink) {
            super(inputLinkSupportName,dbLink);
            this.dbLink = dbLink;
            pvLink = dbLink.getPVLink();
            channelRequesterName = 
                pvLink.getFullName();
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
            if(!super.checkSupportState(SupportState.readyForInitialize,inputLinkSupportName)) return;
            dbRecord = dbLink.getDBRecord();
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
            configStructure = super.getConfigStructure("outputLink", true);
            if(configStructure==null) return;
            pvnameAccess = configStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            processAccess = configStructure.getBooleanField("process");
            if(processAccess==null) return;
            inheritSeverityAccess = configStructure.getBooleanField("inheritSeverity");
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
            if(!super.checkSupportState(SupportState.readyForStart,inputLinkSupportName)) return;
            isConnected = false;
            if(valueDBField==null) {
                pvLink.message(
                    "Logic Error: InputLink.start called before setField",
                    MessageType.error);
                setSupportState(SupportState.zombie);
                return;
            }
            inheritSeverity = inheritSeverityAccess.get();
            process = processAccess.get();
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                pvLink.message("pvname is not defined",MessageType.error);
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
            forever:
            while(true) {
                DBRecord dbRecord = iocdb.findRecord(recordName);
                if(dbRecord==null) {
                    targetDBRecord = null;
                    break;
                }
                PVAccess pvAccess = PVAccessFactory.createPVAccess(dbRecord.getPVRecord());
                switch(pvAccess.findField(fieldName)) {
                case otherRecord:
                    recordName = pvAccess.getOtherRecord();
                    fieldName = pvAccess.getOtherField();
                    break;
                case thisRecord:
                    targetDBRecord = iocdb.findRecord(recordName);
                    if(targetDBRecord==null) {
                        throw new IllegalStateException(channelRequesterName + " logic error?"); 
                    }
                    targetPVField = pvAccess.getField();
                    if(targetPVField==null) {
                        throw new IllegalStateException(channelRequesterName + " logic error?"); 
                    }
                    targetDBField = targetDBRecord.findDBField(targetPVField);
                    if(targetPVField==null) {
                        throw new IllegalStateException(channelRequesterName + " logic error?");
                    }
                    if(inheritSeverity) {
                        pvAccess.findField("");
                        if(pvAccess.findField("severity")==AccessSetResult.thisRecord) {
                            targetPVSeverity = (PVEnum)pvAccess.getField();
                        } else {
                            pvLink.message("severity field not found",MessageType.error);
                            return;
                        }
                    }
                    break forever;
                case notFound:
                    targetDBRecord = null;
                    break forever;
                }
            }
            if(targetDBRecord!=null) {
                if(process) {
                    targetRecordProcess = targetDBRecord.getRecordProcess();
                    isRecordProcessRequester = targetRecordProcess.setRecordProcessRequester(this);
                    if(!isRecordProcessRequester) {
                        if(!recordProcess.canProcessSelf()) {
                            pvLink.message("can not process",MessageType.warning);
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
                    pvLink.message(
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
                severityField = null;
                putFieldGroup = null;
                getFieldGroup = null;
                channel.destroy();
                channel = null;
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            valueDBField = dbField;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,inputLinkSupportName + ".process")) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvLink.getFullFieldName() + " not ready",
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
                            pvLink.getFullFieldName() + " not connected",
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
                    targetRecordProcess.setActive(this);
                } else {
                    if(targetRecordProcess.processSelfRequest(this)) {
                        targetRecordProcess.processSelfSetActive(this);
                    } else {
                        if(alarmSupport!=null) alarmSupport.setAlarm(
                                pvLink.getFullFieldName() + "could not process record",
                                AlarmSeverity.major);
                        putLocalData();
                        supportProcessRequester.supportProcessDone(RequestResult.failure);
                        return;
                    }
                }
                putLocalData();
                if(isRecordProcessRequester) {
                    targetRecordProcess.process(this, false, timeStamp);
                } else {
                    targetRecordProcess.processSelfProcess(this, false);
                }
            } else {
                arrayLength = -1;
                if(inheritSeverity) {
                    channelPutGet.putGet();
                } else {
                    channelPut.put();
                }
            }
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
            if(inheritSeverity) {
                if(requestResult==RequestResult.success) {
                    int index = targetPVSeverity.getIndex();
                    alarmSeverity = AlarmSeverity.getSeverity(index);
                } else {
                    alarmSeverity = AlarmSeverity.invalid;
                }
            }
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
                    severityField = null;
                    putFieldGroup = null;
                    getFieldGroup = null;
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
            if(channelField==severityField) {
                PVEnum pvEnum = (PVEnum)data;
                int index = pvEnum.getIndex();
                alarmSeverity = AlarmSeverity.getSeverity(index);
                return false;
            }
            if(channelField!=valueChannelField) {
                pvLink.message(
                    "Logic error in InputLink field!=valueChannelField",
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
            pvLink.message(
                    "Logic error in InputLink: unsupported type",
                    MessageType.fatalError);
            return false;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequester#nextGetField(org.epics.ioc.ca.ChannelField, org.epics.ioc.pv.PVField)
         */
        public boolean nextGetField(ChannelField channelField, PVField pvField) {
            if(channelField!=severityField) {
                throw new IllegalStateException(channelRequesterName + "Logic error");  
            }
            PVEnum pvEnum = (PVEnum)channelField;
            int index = pvEnum.getIndex();
            alarmSeverity = AlarmSeverity.getSeverity(index);
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
         * @see org.epics.ioc.ca.ChannelPutRequester#nextDelayedPutField(org.epics.ioc.pv.PVField)
         */
        public boolean nextDelayedPutField(PVField field) {
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
                pvLink.message(message, messageType);
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
                pvLink.message(
                        "Logic error in OutputLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                targetDBRecord.unlock();
            }
        }
        
        private boolean prepareForOutput() {
            ChannelFindFieldResult result = channel.findField(valueFieldName);
            if(result!=ChannelFindFieldResult.thisChannel) {
                message(valueFieldName + " does not exist ",MessageType.error);
                return false;
            }
            valueChannelField = channel.getChannelField();
            if(!checkCompatibility(valueChannelField.getField())) {
                valueChannelField = null;
                return false;
            }
            putFieldGroup = channel.createFieldGroup(this);
            putFieldGroup.addChannelField(valueChannelField);
            if(inheritSeverity) {
                channel.findField("");
                result = channel.findField("severity");
                if(result!=ChannelFindFieldResult.thisChannel) {
                    putFieldGroup = null;
                    valueChannelField = null;
                    message(" severity does not exist ",MessageType.error);
                    return false;
                }
                severityField = channel.getChannelField();
                Type type = severityField.getField().getType();
                if(type!=Type.pvEnum) {
                    putFieldGroup = null;
                    valueChannelField = null;
                    message(" severity is not an enum ",MessageType.error);
                    return false;
                }
                getFieldGroup = channel.createFieldGroup(this);
                getFieldGroup.addChannelField(severityField);
            }
            if(inheritSeverity) {
                channelPutGet = channel.createChannelPutGet(
                    putFieldGroup, getFieldGroup, this, process,true);
            } else {
                channelPut = channel.createChannelPut(
                    putFieldGroup,this, process,true);
            }
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
        change,
        deltaChange,
        percentageChange
    }
    
    private static class MonitorNotifyLink extends AbstractLinkSupport implements
    RecordProcessRequester,
    ChannelStateListener,
    ChannelMonitorNotifyRequester
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private DBRecord dbRecord;
        private String channelRequesterName = null;
        private RecordProcess recordProcess = null;
        private AlarmSupport alarmSupport;
        private PVStructure configStructure = null;       
        
        private PVString pvnameAccess = null;
        private PVEnum monitorTypeAccess = null;
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
       
        private MonitorNotifyLink(DBLink dbLink) {
            super(monitorNotifyLinkSupportName,dbLink);
            this.dbLink = dbLink;
            pvLink = dbLink.getPVLink();
            dbRecord = dbLink.getDBRecord();
            channelRequesterName = 
                pvLink.getFullName();
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
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorLinkSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            if(!recordProcess.setRecordProcessRequester(this)) {
                super.message("notifyLink but record already has recordProcessor", MessageType.error);
                return;
            }
            isActive = false;
            alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
            configStructure = super.getConfigStructure("monitorNotifyLink", true);
            if(configStructure==null) return;
            pvnameAccess = configStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            monitorTypeAccess = configStructure.getEnumField("type");
            if(monitorTypeAccess==null) return;
            String[] choices = monitorTypeAccess.getChoices();
            if(choices.length!=3
            || !choices[0].equals("change")
            || !choices[1].equals("deltaChange")
            || !choices[2].equals("percentageChange") ) {
                pvLink.message("field type is not a valid enum", MessageType.error);
                return;
            }
            deadbandAccess = configStructure.getDoubleField("deadband");
            if(deadbandAccess==null) return;
            onlyWhileProcessingAccess = configStructure.getBooleanField("onlyWhileProcessing");
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
            if(!super.checkSupportState(SupportState.readyForStart,monitorLinkSupportName)) return;
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
                pvLink.message(
                        "Failed to create channel for " + recordName,
                        MessageType.error);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            int index = monitorTypeAccess.getIndex();
            String type = monitorTypeAccess.getChoices()[index];
            monitorType = MonitorType.valueOf(type);
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
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            // nothing to do
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
            ChannelFindFieldResult result = channel.findField(fieldName);
            if(result!=ChannelFindFieldResult.thisChannel) {
                pvLink.message(
                    "fieldName " + fieldName
                    + " is not in record " + recordName,
                    MessageType.error);
                return;
            }
            dataField = channel.getChannelField();
            if(!dataField.getField().getType().isNumeric()) {
                channelMonitor.lookForChange(dataField, true);
            } else {
                switch(monitorType) {
                case change:
                    channelMonitor.lookForChange(dataField, true); break;
                case deltaChange:
                    channelMonitor.lookForAbsoluteChange(dataField, deadband); break;
                case percentageChange:
                    channelMonitor.lookForPercentageChange(dataField, deadband); break;
                }
            }
            String threadName = pvLink.getFullName();
            channelMonitor.start((ChannelMonitorNotifyRequester)this, threadName, ScanPriority.low);
        }
    }
    
    private static class MonitorLink extends AbstractLinkSupport implements
    ChannelStateListener,
    ChannelFieldGroupListener,
    ChannelMonitorRequester,
    RecordProcessRequester
    {
        private DBLink dbLink;
        private PVLink pvLink;
        private DBRecord dbRecord = null;
        private String channelRequesterName = null;
        private RecordProcess recordProcess = null;
        private AlarmSupport alarmSupport;
        private PVStructure configStructure = null;       
        
        private PVString pvnameAccess = null;
        private PVMenu monitorTypeAccess = null;
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
        
        private MonitorLink(DBLink dbLink) {
            super(monitorLinkSupportName,dbLink);
            this.dbLink = dbLink;
            pvLink = dbLink.getPVLink();
            dbRecord = dbLink.getDBRecord();
            channelRequesterName = pvLink.getFullName();
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
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorLinkSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
            configStructure = super.getConfigStructure("monitorLink", true);
            if(configStructure==null) return;
            pvnameAccess = configStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            monitorTypeAccess = configStructure.getMenuField("type","monitorType");
            if(monitorTypeAccess==null) return;
            deadbandAccess = configStructure.getDoubleField("deadband");
            if(deadbandAccess==null) return;
            onlyWhileProcessingAccess = configStructure.getBooleanField("onlyWhileProcessing");
            if(onlyWhileProcessingAccess==null) return;
            queueSizeAccess = configStructure.getIntField("queueSize");
            if(queueSizeAccess==null) return;
            reportOverrunAccess = configStructure.getBooleanField("reportOverrun");
            if(reportOverrunAccess==null) return;
            processAccess = configStructure.getBooleanField("process");
            if(processAccess==null) return;
            inheritSeverityAccess = configStructure.getBooleanField("inheritSeverity");
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
            if(!super.checkSupportState(SupportState.readyForStart,monitorLinkSupportName)) return;
            if(valueDBField==null) {
                pvLink.message(
                        "Logic Error: MonitorLink.start called before setField",
                        MessageType.fatalError);
                setSupportState(SupportState.zombie);
                return;
            }           
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
                pvLink.message(
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
            int index = monitorTypeAccess.getIndex();
            String type = monitorTypeAccess.getChoices()[index];
            monitorType = MonitorType.valueOf(type);
            deadband = deadbandAccess.get();
            onlyWhileProcessing = onlyWhileProcessingAccess.get();
            queueSize = queueSizeAccess.get();
            if(queueSize<=1) {
                pvLink.message("queueSize being change to 2", MessageType.warning);
                queueSize = 2;
            }
            reportOverrun = reportOverrunAccess.get();
            process = processAccess.get();
            if(process) {
                isRecordProcessRequester = recordProcess.setRecordProcessRequester(this);
                if(!isRecordProcessRequester) {
                    if(!recordProcess.canProcessSelf()) {
                        configStructure.message("process may fail",
                                MessageType.warning);
                    }
                }
                processLock = new ReentrantLock();
                processCondition = processLock.newCondition();
            }
            inheritSeverity = inheritSeverityAccess.get();
            if(!process && inheritSeverity) {
                configStructure.message("inheritSeverity ignored", MessageType.warning);
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
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.db.DBField)
         */
        public void setField(DBField dbField) {
            valueDBField = dbField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#process(org.epics.ioc.process.LinkListener)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!super.checkSupportState(SupportState.ready,monitorLinkSupportName + ".process")) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvLink.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.failure);
                return;
            }
            if(!channel.isConnected()) {
                if(alarmSupport!=null) alarmSupport.setAlarm("Link not connected",
                    AlarmSeverity.invalid);
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
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
                pvLink.message(
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
                ChannelFieldGroup channelFieldGroup = cD.getChannelFieldGroup();
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                CDStructure cdStructure = cD.getCDRecord().getCDStructure();
                CDField[] cdbFields = cdStructure.getFieldCDFields();
                for(int i=0;i<cdbFields.length; i++) {
                    CDField cdField = cdbFields[i];
                    PVField targetPVField = cdField.getPVField();
                    ChannelField channelField = channelFieldList.get(i);
                    if(channelField==severityChannelField) {
                        PVEnum targetPVEnum = (PVEnum)targetPVField;
                        alarmSeverity = AlarmSeverity.getSeverity(targetPVEnum.getIndex());
                        continue;
                    }
                    if(channelField!=targetChannelField) {
                        pvLink.message(
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
                        valueDBField.postPut();
                        continue;
                    }
                    if(targetType==Type.pvArray && valueType==Type.pvArray) {
                        PVArray targetPVArray = (PVArray)targetPVField;
                        PVArray valuePVArray = (PVArray)valuePVField;
                        convert.copyArray(targetPVArray,0,
                            valuePVArray,0,targetPVArray.getLength());
                        valueDBField.postPut();
                        continue;
                    }
                    if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                        PVStructure targetPVStructure = (PVStructure)targetPVField;
                        PVStructure valuePVStructure = (PVStructure)valuePVField;
                        convert.copyStructure(targetPVStructure,valuePVStructure);
                        valueDBField.postPut();
                        continue;
                    }
                    pvLink.message(
                            "Logic error in MonitorLink: unsupported type",
                            MessageType.fatalError);
                }
                if(process) {
                    if(isRecordProcessRequester) {
                        processDone = false;
                        recordProcess.process(this, false, null);
                    }
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
                Array targetArray = (Array)targetChannelField;
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
            ChannelFindFieldResult result = channel.findField(fieldName);
            if(result!=ChannelFindFieldResult.thisChannel) {
                pvLink.message(
                    "fieldName " + fieldName
                    + " is not in record " + recordName,
                    MessageType.error);
                return;
            }
            targetChannelField = channel.getChannelField();
            String errorMessage = checkCompatibility();
            if(errorMessage!=null) {
                pvLink.message(errorMessage,MessageType.error);
                return;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(targetChannelField);
            if(!targetChannelField.getField().getType().isNumeric()) {
                channelMonitor.lookForChange(targetChannelField, true);
            } else {
                switch(monitorType) {
                case change:
                    channelMonitor.lookForChange(targetChannelField, true); break;
                case deltaChange:
                    channelMonitor.lookForAbsoluteChange(targetChannelField, deadband); break;
                case percentageChange:
                    channelMonitor.lookForPercentageChange(targetChannelField, deadband); break;
                }
            }
            if(inheritSeverityAccess.get()) {
                result = channel.findField("severity");
                if(result==ChannelFindFieldResult.thisChannel) {
                    severityChannelField = channel.getChannelField();
                    channelFieldGroup.addChannelField(severityChannelField);
                    channelMonitor.lookForChange(severityChannelField, true);
                } else {
                    severityChannelField = null;
                }
            }
            String threadName = pvLink.getFullName();
            channelMonitor.start((ChannelMonitorRequester)this, queueSize, threadName, ScanPriority.low);
        }
    }
}
