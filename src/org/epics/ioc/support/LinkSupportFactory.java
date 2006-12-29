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

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.*;

/**
 * Factory to create link support.
 * @author mrk
 *
 */
public class LinkSupportFactory {
    /**
     * Create link support.
     * @param pvLink The field for which to create support.
     * @return A LinkSupport interface or null failure.
     */
    public static LinkSupport create(PVLink pvLink) {
        String supportName = pvLink.getSupportName();
        if(supportName.equals(processLinkSupportName)) {
            return new ProcessLink(pvLink);
        } else if(supportName.equals(inputLinkSupportName)) {
            return new InputLink(pvLink);
        } else if(supportName.equals(outputLinkSupportName)) {
            return new OutputLink(pvLink);
        } else if(supportName.equals(monitorLinkSupportName)) {
            return new MonitorLink(pvLink);
        } else if(supportName.equals(monitorNotifyLinkSupportName)) {
            return new MonitorNotifyLink(pvLink);
        }
        pvLink.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String processLinkSupportName = "processLink";
    private static final String inputLinkSupportName = "inputLink";
    private static final String outputLinkSupportName = "outputLink";
    private static final String monitorLinkSupportName = "monitorLink";
    private static final String monitorNotifyLinkSupportName = "monitorNotifyLink";

    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern periodPattern = Pattern.compile("[.]");
    
    private static class ProcessLink extends AbstractSupport
    implements LinkSupport,
    RecordProcessRequestor,ProcessCallbackRequestor,ProcessContinueRequestor,
    ChannelProcessRequestor,
    ChannelGetRequestor,ChannelStateListener,ChannelFieldGroupListener
    {
        private PVLink pvLink = null;
        private String channelRequestorName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private PVStructure configStructure = null;
        private PVString pvnameAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private boolean inheritSeverity = false;
        private boolean isLocal;
        private boolean isConnected = false;

        private SupportProcessRequestor supportProcessRequestor = null;
        private RequestResult requestResult = null;
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
        
        private TimeStamp timeStamp = new TimeStamp();
        private DBRecord linkDBRecord = null;
        private RecordProcess linkRecordProcess = null;
        private PVEnum pvSeverity = null;
      
        private Channel channel = null;
        private ChannelProcess channelProcess = null;
        private ChannelGet channelGet = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup channelFieldGroup = null;

        private ProcessLink(PVLink pvLink) {
            super(processLinkSupportName,(DBData)pvLink);
            this.pvLink = pvLink;
            channelRequestorName = 
                pvLink.getPVRecord().getRecordName()
                + pvLink.getFullFieldName();
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,processLinkSupportName)) return;
            dbRecord = (DBRecord)pvLink.getPVRecord();
            recordProcess = dbRecord.getRecordProcess();
            configStructure = super.getConfigStructure("processLink");
            if(configStructure==null) return;
            pvnameAccess = super.getString(configStructure,"pvname");
            if(pvnameAccess==null) return;
            inheritSeverityAccess = super.getBoolean(configStructure,"inheritSeverity");
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
            linkDBRecord = iocdb.findRecord(recordName);
            if(linkDBRecord!=null) {
                if(inheritSeverity) {
                    DBAccess dbAccess = iocdb.createAccess(recordName);
                    if(dbAccess.setField("severity")==AccessSetResult.thisRecord) {
                        pvSeverity = (PVEnum)dbAccess.getField();
                    } else {
                        pvLink.message("severity field not found",MessageType.error);
                        return;
                    }
                }
                linkRecordProcess = linkDBRecord.getRecordProcess();
                boolean result = linkRecordProcess.setRecordProcessRequestor(this);
                if(!result) {
                    pvLink.message("record already has processor", MessageType.error);
                    linkDBRecord = null;
                    return;
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
                    channelGet = channel.createChannelGet(this, true);
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
                    channelProcess = channel.createChannelProcess(this);
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
                linkRecordProcess.releaseRecordProcessRequestor(this);
                linkDBRecord = null;
                linkRecordProcess = null;
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
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,processLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        pvLink.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
                return;
            }
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("supportProcessRequestor is null");
            }
            this.supportProcessRequestor = supportProcessRequestor;
            if(isConnected) {
                recordProcess.getTimeStamp(timeStamp);
                recordProcess.requestProcessCallback(this);
                return;
            }
            recordProcess.setStatusSeverity(
                pvLink.getFullFieldName() + " not connected",
                AlarmSeverity.major);
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequestor#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                linkRecordProcess.process(this, false,timeStamp);               
            } else {
                if(inheritSeverity) {
                    channelGet.get(channelFieldGroup);
                } else {
                    channelProcess.process();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
            if(inheritSeverity) {
                if(requestResult==RequestResult.success) {
                    int index = pvSeverity.getIndex();
                    alarmSeverity = AlarmSeverity.getSeverity(index);
                } else {
                    alarmSeverity = AlarmSeverity.invalid;
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
               recordProcess.setStatusSeverity("linkAlarm", alarmSeverity);
               alarmSeverity = AlarmSeverity.none;
            }
            supportProcessRequestor.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData field) {
            // nothing to do
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextGetData(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetData(ChannelField field, PVData data) {
            if(field!=severityField) {
                throw new IllegalStateException(channelRequestorName + "Logic error");  
            }
            PVEnum pvEnum = (PVEnum)data;
            int index = pvEnum.getIndex();
            alarmSeverity = AlarmSeverity.getSeverity(index);
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // Nothing to do.
            return false;
        }
        
        public void getDone(RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProcessRequestor#processDone(org.epics.ioc.util.RequestResult)
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
                SupportState supportState = dbRecord.getSupport().getSupportState();
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
            ChannelSetFieldResult result = channel.setField("severity");
            if(result!=ChannelSetFieldResult.thisChannel) {
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
    
    private static class InputLink extends AbstractSupport
    implements LinkSupport,
    RecordProcessRequestor,ProcessCallbackRequestor,ProcessContinueRequestor,
    ChannelGetRequestor,ChannelFieldGroupListener,ChannelStateListener
    {
        private PVLink pvLink;
        private String channelRequestorName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private PVStructure configStructure = null;
        private PVString pvnameAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private PVData valueData = null;
        
        private boolean process = false;
        private boolean inheritSeverity = false;
        private boolean isLocal;
        private boolean isConnected = false;
        
        private SupportProcessRequestor supportProcessRequestor = null;
        private RequestResult requestResult = null;   
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
             
        private TimeStamp timeStamp = new TimeStamp();
        private DBRecord linkDBRecord = null;
        private RecordProcess linkRecordProcess = null;
        private PVData pvData = null;
        private PVEnum pvSeverity = null;
        
        
        private String valueFieldName = null;
        private Channel channel = null;
        private ChannelGet channelGet = null;       
        private ChannelField valueField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup channelFieldGroup = null;
        private int arrayLength = 0;
        private int arrayOffset = 0;
        
        /**
         * Constructor for InputLink.
         * @param pvLink The field for which to create support.
         */
        public InputLink(PVLink pvLink) {
            super(inputLinkSupportName,(DBData)pvLink);
            this.pvLink = pvLink;
            channelRequestorName = 
                pvLink.getPVRecord().getRecordName()
                + pvLink.getFullFieldName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,inputLinkSupportName)) return;
            dbRecord = (DBRecord)pvLink.getPVRecord();
            recordProcess = dbRecord.getRecordProcess();
            configStructure = super.getConfigStructure("inputLink");
            if(configStructure==null) return;
            pvnameAccess = super.getString(configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = super.getBoolean(configStructure, "process");
            if(processAccess==null) return;
            inheritSeverityAccess = super.getBoolean(configStructure,"inheritSeverity");
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
            if(valueData==null) {
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
                DBAccess dbAccess = iocdb.createAccess(recordName);
                if(dbAccess==null) {
                    linkDBRecord = null;
                    break;
                }
                switch(dbAccess.setField(fieldName)) {
                case otherRecord:
                    recordName = dbAccess.getOtherRecord();
                    fieldName = dbAccess.getOtherField();
                    break;
                case thisRecord:
                    linkDBRecord = iocdb.findRecord(recordName);
                    if(linkDBRecord==null) {
                        throw new IllegalStateException(channelRequestorName + "logic error?"); 
                    }
                    pvData = dbAccess.getField();
                    if(pvData==null) {
                        throw new IllegalStateException(channelRequestorName + "logic error?"); 
                    }
                    if(inheritSeverity) {
                        dbAccess.setField("");
                        if(dbAccess.setField("severity")==AccessSetResult.thisRecord) {
                            pvSeverity = (PVEnum)dbAccess.getField();
                        } else {
                            pvLink.message("severity field not found",MessageType.error);
                            return;
                        }
                    }
                    break forever;
                case notFound:
                    linkDBRecord = null;
                    break forever;
                }
            }
            if(linkDBRecord!=null) {
                if(process) {
                    linkRecordProcess = linkDBRecord.getRecordProcess();
                    boolean result = linkRecordProcess.setRecordProcessRequestor(this);
                    if(!result) {
                        pvLink.message("record already has processor", MessageType.error);
                        linkDBRecord = null;
                        return;
                    }
                }
                if(!checkCompatibility(pvData.getField())) {
                    linkDBRecord = null;
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
                channelGet = channel.createChannelGet(this,process);
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
                if(process) linkRecordProcess.releaseRecordProcessRequestor(this);
                linkDBRecord = null;
                linkRecordProcess = null;
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
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            valueData = data;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,inputLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        pvLink.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
            }
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("supportProcessRequestor is null");
            }
            this.supportProcessRequestor = supportProcessRequestor;
            if(isConnected) {
                if(isLocal && !process) {
                    dbRecord.lockOtherRecord(linkDBRecord);
                    try {
                        getLocalData();
                    } finally {
                        linkDBRecord.unlock();
                    }
                    supportProcessRequestor.supportProcessDone(RequestResult.success);
                    return;
                }
                recordProcess.getTimeStamp(timeStamp);
                recordProcess.requestProcessCallback(this);
                return;
            }
            recordProcess.setStatusSeverity(
                pvLink.getFullFieldName() + " not connected",
                AlarmSeverity.major);
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequestor#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                linkRecordProcess.process(this, false,timeStamp);
            } else {
                arrayLength = -1;
                channelGet.get(channelFieldGroup);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#ready()
         */
        public RequestResult ready() {
            throw new IllegalStateException(channelRequestorName + " why was ready called?");      
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
                recordProcess.setStatusSeverity("linkAlarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
             }
             supportProcessRequestor.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
            linkDBRecord.lockOtherRecord(dbRecord);
            try {
                getLocalData();
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
                    valueField = null;
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
                SupportState supportState = dbRecord.getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#newData(org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetData(ChannelField field,PVData data) {
            if(field==severityField) {
                PVEnum pvEnum = (PVEnum)data;
                int index = pvEnum.getIndex();
                alarmSeverity = AlarmSeverity.getSeverity(index);
                return false;
            }
            if(field!=valueField) {
                pvLink.message(
                    "Logic error in InputLink field!=valueField",
                    MessageType.fatalError);
            }
            Type linkType = data.getField().getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                convert.copyScalar(data,valueData);
                return false;
            }
            if(linkType==Type.pvArray && valueType==Type.pvArray) {
                PVArray linkArrayData = (PVArray)data;
                PVArray recordArrayData = (PVArray)valueData;
                if(arrayLength<0) {
                    arrayLength = linkArrayData.getLength();
                    arrayOffset = 0;
                }
                int num = convert.copyArray(linkArrayData,arrayOffset,
                    recordArrayData,arrayOffset,arrayLength-arrayOffset);
                arrayOffset += num;
                if(arrayOffset<arrayLength) return true;
                return false;
            }
            if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                PVStructure linkStructureData = (PVStructure)data;
                PVStructure recordStructureData = (PVStructure)valueData;
                convert.copyStructure(linkStructureData,recordStructureData);
                return false;
            }
            pvLink.message(
                    "Logic error in InputLink: unsupported type",
                    MessageType.fatalError);
            return false;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // nothing to do
            return false;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#getDone(org.epics.ioc.util.RequestResult)
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
            dbRecord.lockOtherRecord(linkDBRecord);
            try {
                if(inheritSeverity) {
                    int index = pvSeverity.getIndex();
                    alarmSeverity = AlarmSeverity.getSeverity(index);
                }
                Type linkType = pvData.getField().getType();
                Field valueField = valueData.getField();
                Type valueType = valueField.getType();
                if(valueType.isScalar() && linkType.isScalar()) {
                    convert.copyScalar(pvData,valueData);
                    return;
                }
                if(linkType==Type.pvArray && valueType==Type.pvArray) {
                    PVArray linkArrayData = (PVArray)pvData;
                    PVArray recordArrayData = (PVArray)valueData;
                    convert.copyArray(linkArrayData,0,
                        recordArrayData,0,linkArrayData.getLength());
                    return;
                }
                if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                    PVStructure linkStructureData = (PVStructure)pvData;
                    PVStructure recordStructureData = (PVStructure)valueData;
                    convert.copyStructure(linkStructureData,recordStructureData);
                    return;
                }
                pvLink.message(
                        "Logic error in InputLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                linkDBRecord.unlock();
            }
        }
        
        private boolean prepareForInput() {
            ChannelSetFieldResult result = channel.setField(valueFieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
                message(valueFieldName + " does not exist ",MessageType.error);
                return false;
            }
            valueField = channel.getChannelField();
            if(!checkCompatibility(valueField.getField())) {
                valueField = null;
                return false;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(valueField);
            if(inheritSeverity) {
                result = channel.setField("severity");
                if(result!=ChannelSetFieldResult.thisChannel) {
                    channelFieldGroup = null;
                    valueField = null;
                    message(" severity does not exist ",MessageType.error);
                    return false;
                }
                severityField = channel.getChannelField();
                Type type = severityField.getField().getType();
                if(type!=Type.pvEnum) {
                    channelFieldGroup = null;
                    valueField = null;
                    message(" severity is not an enum ",MessageType.error);
                    return false;
                }
                channelFieldGroup.addChannelField(severityField);
            }
            return true;
        }
        
        
        
        private boolean checkCompatibility(Field linkField) {
            Type linkType = linkField.getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(linkField,valueField)) return true;
            } else if(linkType==Type.pvArray && valueType==Type.pvArray) {
                Array linkArray = (Array)linkField;
                Array recordArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return true;
            } else if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure linkStructure = (Structure)linkField;
                Structure recordStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return true;
            }
            message("is not compatible with pvname " + pvnameAccess.get(),MessageType.error);
            return false;
        }
    }
    
    
    private static class OutputLink extends AbstractSupport
    implements LinkSupport,
    RecordProcessRequestor,ProcessCallbackRequestor,ProcessContinueRequestor,
    ChannelPutGetRequestor,
    ChannelFieldGroupListener,ChannelStateListener
    {
        private PVLink pvLink;
        private String channelRequestorName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private PVStructure configStructure = null;
        private PVString pvnameAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private PVData valueData = null;
        
        private boolean process = false;
        private boolean inheritSeverity = false;
        private boolean isLocal;
        private boolean isConnected = false;
        
        private SupportProcessRequestor supportProcessRequestor = null;
        private RequestResult requestResult = null;   
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
             
        private TimeStamp timeStamp = new TimeStamp();
        private DBRecord linkDBRecord = null;
        private RecordProcess linkRecordProcess = null;
        private PVData pvData = null;
        private PVEnum pvSeverity = null;
        
        
        private String valueFieldName = null;
        private Channel channel = null;
        private ChannelPut channelPut = null;
        private ChannelPutGet channelPutGet = null;
        private ChannelField valueField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup putFieldGroup = null;
        private ChannelFieldGroup getFieldGroup = null;
        private int arrayLength = 0;
        private int arrayOffset = 0;
        
        /**
         * Constructor for InputLink.
         * @param pvLink The field for which to create support.
         */
        public OutputLink(PVLink pvLink) {
            super(inputLinkSupportName,(DBData)pvLink);
            this.pvLink = pvLink;
            channelRequestorName = 
                pvLink.getPVRecord().getRecordName()
                + pvLink.getFullFieldName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,inputLinkSupportName)) return;
            dbRecord = (DBRecord)pvLink.getPVRecord();
            recordProcess = dbRecord.getRecordProcess();
            configStructure = super.getConfigStructure("outputLink");
            if(configStructure==null) return;
            pvnameAccess = super.getString(configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = super.getBoolean(configStructure,"process");
            if(processAccess==null) return;
            inheritSeverityAccess = super.getBoolean(configStructure,"inheritSeverity");
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
            if(valueData==null) {
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
                DBAccess dbAccess = iocdb.createAccess(recordName);
                if(dbAccess==null) {
                    linkDBRecord = null;
                    break;
                }
                switch(dbAccess.setField(fieldName)) {
                case otherRecord:
                    recordName = dbAccess.getOtherRecord();
                    fieldName = dbAccess.getOtherField();
                    break;
                case thisRecord:
                    linkDBRecord = iocdb.findRecord(recordName);
                    if(linkDBRecord==null) {
                        throw new IllegalStateException(channelRequestorName + "logic error?"); 
                    }
                    pvData = dbAccess.getField();
                    if(pvData==null) {
                        throw new IllegalStateException(channelRequestorName + "logic error?"); 
                    }
                    if(inheritSeverity) {
                        dbAccess.setField("");
                        if(dbAccess.setField("severity")==AccessSetResult.thisRecord) {
                            pvSeverity = (PVEnum)dbAccess.getField();
                        } else {
                            pvLink.message("severity field not found",MessageType.error);
                            return;
                        }
                    }
                    break forever;
                case notFound:
                    linkDBRecord = null;
                    break forever;
                }
            }
            if(linkDBRecord!=null) {
                if(process) {
                    linkRecordProcess = linkDBRecord.getRecordProcess();
                    boolean result = linkRecordProcess.setRecordProcessRequestor(this);
                    if(!result) {
                        pvLink.message("record already has processor", MessageType.error);
                        linkDBRecord = null;
                        return;
                    }
                }
                if(!checkCompatibility(pvData.getField())) {
                    linkDBRecord = null;
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
                if(inheritSeverity) {
                    channelPutGet = channel.createChannelPutGet(this, process);
                } else {
                    channelPut = channel.createChannelPut(this,process);
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
                if(process) linkRecordProcess.releaseRecordProcessRequestor(this);
                linkDBRecord = null;
                linkRecordProcess = null;
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
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            valueData = data;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,inputLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        pvLink.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
            }
            if(supportProcessRequestor==null) {
                throw new IllegalStateException("supportProcessRequestor is null");
            }
            this.supportProcessRequestor = supportProcessRequestor;
            if(isConnected) {
                if(isLocal && !process) {
                    dbRecord.lockOtherRecord(linkDBRecord);
                    try {
                        putLocalData();
                    } finally {
                        linkDBRecord.unlock();
                    }
                    supportProcessRequestor.supportProcessDone(RequestResult.success);
                    return;
                }
                recordProcess.getTimeStamp(timeStamp);
                recordProcess.requestProcessCallback(this);
                return;
            }
            recordProcess.setStatusSeverity(
                pvLink.getFullFieldName() + " not connected",
                AlarmSeverity.major);
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequestor#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                linkRecordProcess.setActive(this);
                putLocalData();
                linkRecordProcess.process(this, false, timeStamp);
            } else {
                arrayLength = -1;
                if(inheritSeverity) {
                    channelPutGet.putGet(putFieldGroup, getFieldGroup);
                } else {
                    channelPut.put(putFieldGroup);
                }
            }
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
                recordProcess.setStatusSeverity("linkAlarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
             }
             supportProcessRequestor.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            this.requestResult = requestResult;
            if(inheritSeverity) {
                if(requestResult==RequestResult.success) {
                    int index = pvSeverity.getIndex();
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
                    valueField = null;
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
                SupportState supportState = dbRecord.getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#newData(org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextPutData(ChannelField field,PVData data) {
            if(field==severityField) {
                PVEnum pvEnum = (PVEnum)data;
                int index = pvEnum.getIndex();
                alarmSeverity = AlarmSeverity.getSeverity(index);
                return false;
            }
            if(field!=valueField) {
                pvLink.message(
                    "Logic error in InputLink field!=valueField",
                    MessageType.fatalError);
            }
            Type linkType = data.getField().getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                convert.copyScalar(data,valueData);
                return false;
            }
            if(linkType==Type.pvArray && valueType==Type.pvArray) {
                PVArray linkArrayData = (PVArray)data;
                PVArray recordArrayData = (PVArray)valueData;
                if(arrayLength<0) {
                    arrayLength = linkArrayData.getLength();
                    arrayOffset = 0;
                    linkArrayData.setLength(arrayLength);
                }
                int num = convert.copyArray(recordArrayData,arrayOffset,
                    linkArrayData,arrayOffset,arrayLength-arrayOffset);
                arrayOffset += num;
                if(arrayOffset<arrayLength) return true;
                return false;
            }
            if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                PVStructure linkStructureData = (PVStructure)data;
                PVStructure recordStructureData = (PVStructure)valueData;
                convert.copyStructure(linkStructureData,recordStructureData);
                return false;
            }
            pvLink.message(
                    "Logic error in InputLink: unsupported type",
                    MessageType.fatalError);
            return false;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextGetData(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetData(ChannelField field, PVData data) {
            if(field!=severityField) {
                throw new IllegalStateException(channelRequestorName + "Logic error");  
            }
            PVEnum pvEnum = (PVEnum)data;
            int index = pvEnum.getIndex();
            alarmSeverity = AlarmSeverity.getSeverity(index);
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // nothing to do
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#nextDelayedPutData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedPutData(PVData data) {
            // nothing to do
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelGetRequestor#getDone(org.epics.ioc.util.RequestResult)
         */
        public void getDone(RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelPutRequestor#putDone(org.epics.ioc.util.RequestResult)
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
            dbRecord.lockOtherRecord(linkDBRecord);
            try {
                Type linkType = pvData.getField().getType();
                Field valueField = valueData.getField();
                Type valueType = valueField.getType();
                if(valueType.isScalar() && linkType.isScalar()) {
                    convert.copyScalar(valueData,pvData);
                    return;
                }
                if(linkType==Type.pvArray && valueType==Type.pvArray) {
                    PVArray linkArrayData = (PVArray)pvData;
                    PVArray recordArrayData = (PVArray)valueData;
                    convert.copyArray(recordArrayData,0,
                        linkArrayData,0,recordArrayData.getLength());
                    return;
                }
                if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                    PVStructure linkStructureData = (PVStructure)pvData;
                    PVStructure recordStructureData = (PVStructure)valueData;
                    convert.copyStructure(recordStructureData,linkStructureData);
                    return;
                }
                pvLink.message(
                        "Logic error in OutputLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                linkDBRecord.unlock();
            }
        }
        
        private boolean prepareForOutput() {
            ChannelSetFieldResult result = channel.setField(valueFieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
                message(valueFieldName + " does not exist ",MessageType.error);
                return false;
            }
            valueField = channel.getChannelField();
            if(!checkCompatibility(valueField.getField())) {
                valueField = null;
                return false;
            }
            putFieldGroup = channel.createFieldGroup(this);
            putFieldGroup.addChannelField(valueField);
            if(inheritSeverity) {
                channel.setField("");
                result = channel.setField("severity");
                if(result!=ChannelSetFieldResult.thisChannel) {
                    putFieldGroup = null;
                    valueField = null;
                    message(" severity does not exist ",MessageType.error);
                    return false;
                }
                severityField = channel.getChannelField();
                Type type = severityField.getField().getType();
                if(type!=Type.pvEnum) {
                    putFieldGroup = null;
                    valueField = null;
                    message(" severity is not an enum ",MessageType.error);
                    return false;
                }
                getFieldGroup = channel.createFieldGroup(this);
                getFieldGroup.addChannelField(severityField);
            }
            return true;
        }
              
        private boolean checkCompatibility(Field linkField) {
            Type linkType = linkField.getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(linkField,valueField)) return true;
            } else if(linkType==Type.pvArray && valueType==Type.pvArray) {
                Array linkArray = (Array)linkField;
                Array recordArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return true;
            } else if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure linkStructure = (Structure)linkField;
                Structure recordStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return true;
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
    
    private static class MonitorNotifyLink extends AbstractSupport
    implements LinkSupport,
    ChannelStateListener,
    ChannelMonitorNotifyRequestor
    {
        private PVLink pvLink = null;
        private DBRecord dbRecord = null;
        private String channelRequestorName = null;
        private RecordProcess recordProcess = null;
        private PVStructure configStructure = null;       
        
        private PVString pvnameAccess = null;
        private PVEnum monitorTypeAccess = null;
        private PVDouble deadbandAccess = null;
        private PVBoolean onlyWhileProcessingAccess = null;
        
        private PVBoolean pvNotify = null;
        
        private String recordName = null;
        private String fieldName = null;
        private MonitorType monitorType = null;
        private double deadband = 0.0;
        private boolean onlyWhileProcessing = false;
              
        private Channel channel = null;
        private ChannelMonitor channelMonitor = null;
        private ChannelField dataField = null;
        /**
         * Constructor for MonitorLink.
         * @param pvLink The field for which to create support.
         */
        public MonitorNotifyLink(PVLink pvLink) {
            super(monitorNotifyLinkSupportName,(DBData)pvLink);
            this.pvLink = pvLink;
            DBData dbData = (DBData)pvLink;
            dbRecord = dbData.getDBRecord();
            channelRequestorName = dbRecord.getRecordName() + pvLink.getFullFieldName();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorLinkSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            configStructure = super.getConfigStructure("monitorNotifyLink");
            if(configStructure==null) return;
            pvnameAccess = super.getString(configStructure,"pvname");
            if(pvnameAccess==null) return;
            monitorTypeAccess = super.getEnum(configStructure,"type");
            if(monitorTypeAccess==null) return;
            String[] choices = monitorTypeAccess.getChoices();
            if(choices.length!=3
            || !choices[0].equals("change")
            || !choices[1].equals("deltaChange")
            || !choices[2].equals("percentageChange") ) {
                pvLink.message("field type is not a valid enum", MessageType.error);
                return;
            }
            deadbandAccess = super.getDouble(configStructure,"deadband");
            if(deadbandAccess==null) return;
            onlyWhileProcessingAccess = super.getBoolean(configStructure,"onlyWhileProcessing");
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
            if(pvNotify==null) {
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
            int index = monitorTypeAccess.getIndex();
            String type = monitorTypeAccess.getChoices()[index];
            monitorType = MonitorType.valueOf(type);
            deadband = deadbandAccess.get();
            onlyWhileProcessing = onlyWhileProcessingAccess.get();
            channelMonitor = channel.createChannelMonitor(onlyWhileProcessing);
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
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,monitorLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        pvLink.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
                return;
            }
            if(!channel.isConnected()) {
                recordProcess.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                supportProcessRequestor.supportProcessDone(RequestResult.success);
                return;
            }
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            Type type = data.getField().getType();
            if(type!=Type.pvBoolean) throw new IllegalStateException("setField must be boolean"); 
            pvNotify = (PVBoolean)data;
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
                SupportState supportState = dbRecord.getSupport().getSupportState();
                if(supportState!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorNotifyRequestor#dataModified(org.epics.ioc.ca.Channel)
         */
        public void monitorEvent() {
            dbRecord.lock();
            try {
                pvNotify.put(true);
            } finally {
                dbRecord.unlock();
            }
        }   
        private void channelStart() {
            ChannelSetFieldResult result = channel.setField(fieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
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
            String threadName = dbRecord.getRecordName() + pvLink.getFullFieldName();
            channelMonitor.start((ChannelMonitorNotifyRequestor)this, threadName, ScanPriority.low);
        }
    }
    
    private static class MonitorLink extends AbstractSupport
    implements LinkSupport,
    ChannelStateListener,
    ChannelFieldGroupListener,
    ChannelMonitorRequestor,
    RecordProcessRequestor
    {
        private PVLink pvLink = null;
        private DBRecord dbRecord = null;
        private String channelRequestorName = null;
        private RecordProcess recordProcess = null;
        private PVStructure configStructure = null;       
        
        private PVString pvnameAccess = null;
        private PVEnum monitorTypeAccess = null;
        private PVDouble deadbandAccess = null;
        private PVBoolean onlyWhileProcessingAccess = null;
        private PVInt queueSizeAccess = null;
        private PVBoolean reportOverrunAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;   
        
        private PVData valueData = null;
        
        private String recordName = null;
        private String fieldName = null;
        private MonitorType monitorType = null;
        private double deadband = 0.0;
        private boolean onlyWhileProcessing = false;
        private int queueSize = 0;
        private boolean reportOverrun = false;
        private boolean process = false;
        private boolean inheritSeverity = false;
              
        private Channel channel = null;
        private boolean isLocal = false;
        private DBRecord linkDBRecord = null;
        private ChannelMonitor channelMonitor = null;
        private ChannelField dataField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup channelFieldGroup = null;
        
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
        private int numberOverrun = 0;
        private ReentrantLock processLock = null;
        private Condition processCondition = null;
        private boolean processDone;
        /**
         * Constructor for MonitorLink.
         * @param pvLink The field for which to create support.
         */
        public MonitorLink(PVLink pvLink) {
            super(monitorLinkSupportName,(DBData)pvLink);
            this.pvLink = pvLink;
            channelRequestorName = pvLink.getPVRecord().getRecordName() + pvLink.getFullFieldName();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorLinkSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            configStructure = super.getConfigStructure("monitorLink");
            if(configStructure==null) return;
            pvnameAccess = super.getString(configStructure,"pvname");
            if(pvnameAccess==null) return;
            monitorTypeAccess = super.getEnum(configStructure,"type");
            if(monitorTypeAccess==null) return;
            String[] choices = monitorTypeAccess.getChoices();
            if(choices.length!=3
            || !choices[0].equals("change")
            || !choices[1].equals("deltaChange")
            || !choices[2].equals("percentageChange") ) {
                pvLink.message("field type is not a valid enum", MessageType.error);
                return;
            }
            deadbandAccess = super.getDouble(configStructure,"deadband");
            if(deadbandAccess==null) return;
            onlyWhileProcessingAccess = super.getBoolean(configStructure,"onlyWhileProcessing");
            if(onlyWhileProcessingAccess==null) return;
            queueSizeAccess = super.getInt(configStructure,"queueSize");
            if(queueSizeAccess==null) return;
            reportOverrunAccess = super.getBoolean(configStructure,"reportOverrun");
            if(reportOverrunAccess==null) return;
            processAccess = super.getBoolean(configStructure,"process");
            if(processAccess==null) return;
            inheritSeverityAccess = super.getBoolean(configStructure,"inheritSeverity");
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
            if(valueData==null) {
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
                linkDBRecord = iocdb.findRecord(recordName);
                if(linkDBRecord==null) {
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
                boolean isProcessor = recordProcess.setRecordProcessRequestor(this);
                if(!isProcessor) {
                    configStructure.message("process is not possible", MessageType.error);
                    process = false;
                } else {
                    processLock = new ReentrantLock();
                    processCondition = processLock.newCondition();
                }
            }
            inheritSeverity = inheritSeverityAccess.get();
            if(!process && inheritSeverity) {
                configStructure.message("inheritSeverity ignored", MessageType.warning);
                inheritSeverity = false;
            }
            channelMonitor = channel.createChannelMonitor(onlyWhileProcessing);
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
         * @see org.epics.ioc.process.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            valueData = data;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.LinkSupport#process(org.epics.ioc.process.LinkListener)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,monitorLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        pvLink.getFullFieldName() + " not ready",
                        AlarmSeverity.major);
                supportProcessRequestor.supportProcessDone(RequestResult.failure);
                return;
            }
            if(!channel.isConnected()) {
                recordProcess.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                supportProcessRequestor.supportProcessDone(RequestResult.success);
                return;
            }
            if(alarmSeverity!=AlarmSeverity.none) {
                recordProcess.setStatusSeverity("link Alarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
            } else if(numberOverrun>0) {
                recordProcess.setStatusSeverity(
                    "missed " + Integer.toString(numberOverrun) + " notifications",
                    AlarmSeverity.none);
                numberOverrun = 0;
            }
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelStateListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!channel.isConnected()) {
                    severityField = null;
                    dataField = null;
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
                SupportState supportState = dbRecord.getSupport().getSupportState();
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
         * @see org.epics.ioc.ca.ChannelMonitorRequestor#dataOverrun(int)
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
         * @see org.epics.ioc.ca.ChannelMonitorRequestor#monitorData(org.epics.ioc.ca.ChannelData)
         */
        public void monitorData(ChannelData channelData) {
            if(isLocal) {
                linkDBRecord.lockOtherRecord(dbRecord);
            } else {
                dbRecord.lock();
            }
            try {
                List<ChannelField> channelFieldList = channelData.getChannelFieldList();
                List<PVData>dataFieldList = channelData.getPVDataList();
                for(int i=0; i <channelFieldList.size(); i++) {
                    ChannelField field = channelFieldList.get(i);
                    PVData data = dataFieldList.get(i);
                    if(field==severityField) {
                        PVEnum pvEnum = (PVEnum)data;
                        alarmSeverity = AlarmSeverity.getSeverity(pvEnum.getIndex());
                        continue;
                    }
                    if(field!=dataField) {
                        pvLink.message(
                                "Logic error in MonitorLink field!=channelField",
                                MessageType.fatalError);
                        continue;
                    }
                    Type linkType = data.getField().getType();
                    Field valueField = valueData.getField();
                    Type valueType = valueField.getType();
                    if(valueType.isScalar() && linkType.isScalar()) {
                        convert.copyScalar(data,valueData);
                        continue;
                    }
                    if(linkType==Type.pvArray && valueType==Type.pvArray) {
                        PVArray linkArrayData = (PVArray)data;
                        PVArray recordArrayData = (PVArray)valueData;
                        convert.copyArray(linkArrayData,0,
                            recordArrayData,0,linkArrayData.getLength());
                        continue;
                    }
                    if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                        PVStructure linkStructureData = (PVStructure)data;
                        PVStructure recordStructureData = (PVStructure)valueData;
                        convert.copyStructure(linkStructureData,recordStructureData);
                        continue;
                    }
                    pvLink.message(
                            "Logic error in MonitorLink: unsupported type",
                            MessageType.fatalError);
                }
                if(process) {
                    processDone = false;
                    recordProcess.process(this, false, null);
                }
            } finally {
                dbRecord.unlock();
            }
            if(process) {
                //  wait for completion
                try {
                    processLock.lock();
                    try {
                        if(!processDone) processCondition.await();
                    }finally {
                        processLock.unlock();
                    }
                } catch(InterruptedException e) {}
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessComplete(org.epics.ioc.process.RequestResult)
         */
        public void recordProcessComplete() {
            processLock.lock();
            try {
                processCondition.signal();
            } finally {
                processLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            processDone = true;
        }
        
        private String checkCompatibility() {
            Type linkType = dataField.getField().getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(dataField.getField(),valueField)) return null;
            } else if(linkType==Type.pvArray && valueType==Type.pvArray) {
                Array linkArray = (Array)dataField;
                Array recordArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return null;
            } else if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure linkStructure = (Structure)dataField;
                Structure recordStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return null;
            }
            String errorMessage = 
                "is not compatible with pvname " + pvnameAccess.get();
            channel = null;
            return errorMessage;
        }
        
        private void channelStart() {
            ChannelSetFieldResult result = channel.setField(fieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
                pvLink.message(
                    "fieldName " + fieldName
                    + " is not in record " + recordName,
                    MessageType.error);
                return;
            }
            dataField = channel.getChannelField();
            String errorMessage = checkCompatibility();
            if(errorMessage!=null) {
                pvLink.message(errorMessage,MessageType.error);
                return;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(dataField);
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
            if(inheritSeverityAccess.get()) {
                result = channel.setField("severity");
                if(result==ChannelSetFieldResult.thisChannel) {
                    severityField = channel.getChannelField();
                    channelFieldGroup.addChannelField(severityField);
                    channelMonitor.lookForChange(severityField, true);
                } else {
                    severityField = null;
                }
            }
            String threadName = dbRecord.getRecordName() + pvLink.getFullFieldName();
            channelMonitor.start((ChannelMonitorRequestor)this, queueSize, threadName, ScanPriority.low);
        }
    }
}
