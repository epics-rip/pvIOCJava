 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.channelAccess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

import java.util.regex.*;

/**
 * Factory to create link support.
 * @author mrk
 *
 */
public class LinkSupportFactory {
    /**
     * Create link support.
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
        }
        dbLink.message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    
    private static final String processLinkSupportName = "processLink";
    private static final String inputLinkSupportName = "inputLink";
    private static final String outputLinkSupportName = "outputLink";
    private static final String monitorLinkSupportName = "monitorLink";

    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern periodPattern = Pattern.compile("[.]");
    
    private static class ProcessLink extends AbstractSupport
    implements LinkSupport,
    RecordProcessRequestor,ProcessCallbackRequestor,ProcessContinueRequestor,
    ChannelGetRequestor,ChannelStateListener,ChannelFieldGroupListener
    {
        private DBLink dbLink = null;
        private String channelRequestorName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private DBStructure configStructure = null;
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
        private ChannelFieldGroup fieldGroup = null;

        private ProcessLink(DBLink dbLink) {
            super(processLinkSupportName,dbLink);
            this.dbLink = dbLink;
            channelRequestorName = 
                dbLink.getRecord().getRecordName()
                + dbLink.getFullFieldName();
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,processLinkSupportName)) return;
            dbRecord = dbLink.getRecord();
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
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,processLinkSupportName)) return;
            isConnected = false;
            inheritSeverity = inheritSeverityAccess.get();
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                dbLink.message("pvname is not defined",MessageType.error);
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
                        dbLink.message("severity field not found",MessageType.error);
                        return;
                    }
                }
                linkRecordProcess = linkDBRecord.getRecordProcess();
                boolean result = linkRecordProcess.setRecordProcessRequestor(this);
                if(!result) {
                    dbLink.message("record already has processor", MessageType.error);
                    linkDBRecord = null;
                    return;
                }
                isLocal = true;
                isConnected = true;
            } else {
                isLocal = false;
                channel = ChannelFactory.createChannel(recordName, this);
                if(channel==null) {
                    dbLink.message(
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
         * @see org.epics.ioc.dbProcess.Support#stop()
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
                fieldGroup = null;
                channel.destroy();
                channel = null;
            }
            setSupportState(SupportState.readyForStart);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#process(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,processLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        dbLink.getFullFieldName() + " not ready",
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
                dbLink.getFullFieldName() + " not connected",
                AlarmSeverity.major);
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackRequestor#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                linkRecordProcess.process(this, false,timeStamp);               
            } else {
                if(inheritSeverity) {
                    channelGet.get(fieldGroup);
                } else {
                    channelProcess.process();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
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
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
               recordProcess.setStatusSeverity("linkAlarm", alarmSeverity);
               alarmSeverity = AlarmSeverity.none;
            }
            supportProcessRequestor.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData field) {
            // nothing to do
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#nextGetData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetData(Channel channel, ChannelField field, PVData data) {
            if(field!=severityField) {
                throw new IllegalStateException(channelRequestorName + "Logic error");  
            }
            PVEnum pvEnum = (PVEnum)data;
            int index = pvEnum.getIndex();
            alarmSeverity = AlarmSeverity.getSeverity(index);
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // Nothing to do.
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#requestDone(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.RequestResult)
         */
        public void requestDone(Channel channel, RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(Channel channel,String message,MessageType messageType) {
            dbRecord.lock();
            try {
                dbLink.message(message, messageType);
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
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
                    fieldGroup = null;
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
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
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // nothing to do         
        }       
        private boolean prepareForInput() {
            ChannelSetFieldResult result = channel.setField("severity");
            if(result!=ChannelSetFieldResult.thisChannel) {
                message(channel,"field severity does not exist",MessageType.error);
                return false;
            }
            severityField = channel.getChannelField();
            Type type = severityField.getField().getType();
            if(type!=Type.pvEnum) {
                severityField = null;
                message(channel,"field severity is not an enum",MessageType.error);
                return false;
            }
            fieldGroup = channel.createFieldGroup(this);
            fieldGroup.addChannelField(severityField);
            return true;
        }
    }
    
    private static class InputLink extends AbstractSupport
    implements LinkSupport,
    RecordProcessRequestor,ProcessCallbackRequestor,ProcessContinueRequestor,
    ChannelGetRequestor,ChannelFieldGroupListener,ChannelStateListener
    {
        private DBLink dbLink;
        private String channelRequestorName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private DBStructure configStructure = null;
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
        private ChannelFieldGroup fieldGroup = null;
        private int arrayLength = 0;
        private int arrayOffset = 0;
        
        /**
         * Constructor for InputLink.
         * @param dbLink The field for which to create support.
         */
        public InputLink(DBLink dbLink) {
            super(inputLinkSupportName,dbLink);
            this.dbLink = dbLink;
            channelRequestorName = 
                dbLink.getRecord().getRecordName()
                + dbLink.getFullFieldName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,inputLinkSupportName)) return;
            dbRecord = dbLink.getRecord();
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
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,inputLinkSupportName)) return;
            isConnected = false;
            if(valueData==null) {
                dbLink.message(
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
                dbLink.message("pvname is not defined",MessageType.error);
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
                            dbLink.message("severity field not found",MessageType.error);
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
                        dbLink.message("record already has processor", MessageType.error);
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
                channel = ChannelFactory.createChannel(recordName,this);
                if(channel==null) {
                    dbLink.message(
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
         * @see org.epics.ioc.dbProcess.Support#stop()
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
                fieldGroup = null;
                channel.destroy();
                channel = null;
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            valueData = data;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#process(org.epics.ioc.dbProcess.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,inputLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        dbLink.getFullFieldName() + " not ready",
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
                dbLink.getFullFieldName() + " not connected",
                AlarmSeverity.major);
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackRequestor#processCallback()
         */
        public void processCallback() {
            if(isLocal) {
                linkRecordProcess.process(this, false,timeStamp);
            } else {
                arrayLength = -1;
                channelGet.get(fieldGroup);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#ready()
         */
        public RequestResult ready() {
            throw new IllegalStateException(channelRequestorName + " why was ready called?");      
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
                recordProcess.setStatusSeverity("linkAlarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
             }
             supportProcessRequestor.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
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
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
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
                    fieldGroup = null;
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
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
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#newData(org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetData(Channel channel,ChannelField field,PVData data) {
            if(field==severityField) {
                PVEnum pvEnum = (PVEnum)data;
                int index = pvEnum.getIndex();
                alarmSeverity = AlarmSeverity.getSeverity(index);
                return false;
            }
            if(field!=valueField) {
                dbLink.message(
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
            dbLink.message(
                    "Logic error in InputLink: unsupported type",
                    MessageType.fatalError);
            return false;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // nothing to do
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel, RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel,String message,MessageType messageType) {
            dbRecord.lock();
            try {
                dbLink.message(message, messageType);
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.ChannelField)
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
                dbLink.message(
                        "Logic error in InputLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                linkDBRecord.unlock();
            }
        }
        
        private boolean prepareForInput() {
            ChannelSetFieldResult result = channel.setField(valueFieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
                message(channel,valueFieldName + " does not exist ",MessageType.error);
                return false;
            }
            valueField = channel.getChannelField();
            if(!checkCompatibility(valueField.getField())) {
                valueField = null;
                return false;
            }
            fieldGroup = channel.createFieldGroup(this);
            fieldGroup.addChannelField(valueField);
            if(inheritSeverity) {
                result = channel.setField("severity");
                if(result!=ChannelSetFieldResult.thisChannel) {
                    fieldGroup = null;
                    valueField = null;
                    message(channel," severity does not exist ",MessageType.error);
                    return false;
                }
                severityField = channel.getChannelField();
                Type type = severityField.getField().getType();
                if(type!=Type.pvEnum) {
                    fieldGroup = null;
                    valueField = null;
                    message(channel," severity is not an enum ",MessageType.error);
                    return false;
                }
                fieldGroup.addChannelField(severityField);
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
            message(channel,"is not compatible with pvname " + pvnameAccess.get(),MessageType.error);
            return false;
        }
    }
    
    
    private static class OutputLink extends AbstractSupport
    implements LinkSupport,
    RecordProcessRequestor,ProcessCallbackRequestor,ProcessContinueRequestor,
    ChannelPutGetRequestor,
    ChannelFieldGroupListener,ChannelStateListener
    {
        private DBLink dbLink;
        private String channelRequestorName = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private DBStructure configStructure = null;
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
         * @param dbLink The field for which to create support.
         */
        public OutputLink(DBLink dbLink) {
            super(inputLinkSupportName,dbLink);
            this.dbLink = dbLink;
            channelRequestorName = 
                dbLink.getRecord().getRecordName()
                + dbLink.getFullFieldName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,inputLinkSupportName)) return;
            dbRecord = dbLink.getRecord();
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
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,inputLinkSupportName)) return;
            isConnected = false;
            if(valueData==null) {
                dbLink.message(
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
                dbLink.message("pvname is not defined",MessageType.error);
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
                            dbLink.message("severity field not found",MessageType.error);
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
                        dbLink.message("record already has processor", MessageType.error);
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
                channel = ChannelFactory.createChannel(recordName,this);
                if(channel==null) {
                    dbLink.message(
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
         * @see org.epics.ioc.dbProcess.Support#stop()
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
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            valueData = data;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#process(org.epics.ioc.dbProcess.SupportProcessRequestor)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,inputLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        dbLink.getFullFieldName() + " not ready",
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
                dbLink.getFullFieldName() + " not connected",
                AlarmSeverity.major);
            supportProcessRequestor.supportProcessDone(RequestResult.success);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.ProcessCallbackRequestor#processCallback()
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
         * @see org.epics.ioc.dbProcess.ProcessContinueRequestor#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
                recordProcess.setStatusSeverity("linkAlarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
             }
             supportProcessRequestor.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete()
         */
        public void recordProcessComplete() {
            recordProcess.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.RequestResult)
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
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
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
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
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
         * @see org.epics.ioc.channelAccess.ChannelPutRequestor#newData(org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextPutData(Channel channel,ChannelField field,PVData data) {
            if(field==severityField) {
                PVEnum pvEnum = (PVEnum)data;
                int index = pvEnum.getIndex();
                alarmSeverity = AlarmSeverity.getSeverity(index);
                return false;
            }
            if(field!=valueField) {
                dbLink.message(
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
            dbLink.message(
                    "Logic error in InputLink: unsupported type",
                    MessageType.fatalError);
            return false;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#nextGetData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetData(Channel channel, ChannelField field, PVData data) {
            if(field!=severityField) {
                throw new IllegalStateException(channelRequestorName + "Logic error");  
            }
            PVEnum pvEnum = (PVEnum)data;
            int index = pvEnum.getIndex();
            alarmSeverity = AlarmSeverity.getSeverity(index);
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // nothing to do
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutRequestor#nextDelayedPutData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedPutData(PVData data) {
            // nothing to do
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel, RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel,String message,MessageType messageType) {
            dbRecord.lock();
            try {
                dbLink.message(message, messageType);
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.ChannelField)
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
                dbLink.message(
                        "Logic error in OutputLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                linkDBRecord.unlock();
            }
        }
        
        private boolean prepareForOutput() {
            ChannelSetFieldResult result = channel.setField(valueFieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
                message(channel,valueFieldName + " does not exist ",MessageType.error);
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
                    message(channel," severity does not exist ",MessageType.error);
                    return false;
                }
                severityField = channel.getChannelField();
                Type type = severityField.getField().getType();
                if(type!=Type.pvEnum) {
                    putFieldGroup = null;
                    valueField = null;
                    message(channel," severity is not an enum ",MessageType.error);
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
            message(channel,"is not compatible with pvname " + pvnameAccess.get(),MessageType.error);
            return false;
        }
    }
    
    private static class MonitorLink extends AbstractSupport
    implements LinkSupport,
    ChannelStateListener,
    ChannelFieldGroupListener,
    RecordProcessRequestor,
    ChannelSubscribeGetRequestor,
    ChannelSubscribeRequestor, ChannelGetRequestor
    {
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private String channelRequestorName = null;
        private RecordProcess recordProcess = null;
        private DBStructure configStructure = null;       
        
        private PVString pvnameAccess = null;
        private PVInt queueCapacityAccess = null;
        private PVBoolean reportOverrunAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;   
        
        private PVData valueData = null;
        
        private String fieldName = null;       
        private int queueCapacity = 0;
        private boolean reportOverrun = false;
        private boolean process = false;
        private boolean inheritSeverity = false;
        
        private boolean isSubscribeGet = false;
        
        private Channel channel = null;
        private DBRecord channelRecord = null;
        private ChannelSubscribe channelSubscribe = null;
        private ChannelField dataField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup fieldGroup = null;
        // channelGet is used if queueCapacity is 0
        private ChannelGet channelGet = null;
        
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
        private int numberOverrun = 0;
        
        
        /**
         * Constructor for MonitorLink.
         * @param dbLink The field for which to create support.
         */
        public MonitorLink(DBLink dbLink) {
            super(monitorLinkSupportName,dbLink);
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
            channelRequestorName = dbRecord.getRecordName() + dbLink.getFullFieldName();
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requestor#getRequestorName()
         */
        public String getRequestorName() {
            return channelRequestorName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorLinkSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            configStructure = super.getConfigStructure("monitorLink");
            if(configStructure==null) return;
            pvnameAccess = super.getString(configStructure,"pvname");
            if(pvnameAccess==null) return;
            queueCapacityAccess = super.getInt(configStructure,"queueCapacity");
            if(queueCapacityAccess==null) return;
            reportOverrunAccess = super.getBoolean(configStructure,"reportOverrun");
            if(reportOverrunAccess==null) return;
            processAccess = super.getBoolean(configStructure,"process");
            if(processAccess==null) return;
            inheritSeverityAccess = super.getBoolean(configStructure,"inheritSeverity");
            if(inheritSeverityAccess==null) return;
            setSupportState(SupportState.readyForStart);  
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,monitorLinkSupportName)) return;
            if(valueData==null) {
                dbLink.message(
                        "Logic Error: MonitorLink.start called before setField",
                        MessageType.fatalError);
                setSupportState(SupportState.zombie);
                return;
            }           
            // split pvname into record name and rest of name
            String[]pvname = periodPattern.split(pvnameAccess.get(),2);
            String recordName = pvname[0];
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }
            channel = ChannelFactory.createChannel(recordName,this);
            if(channel==null) {
                dbLink.message(
                        "Failed to create channel for " + recordName,
                        MessageType.error);
                setSupportState(SupportState.readyForInitialize);
                return;
            }                       
            queueCapacity = queueCapacityAccess.get();
            if(queueCapacity<=0) {
                isSubscribeGet = false;
            } else {
                isSubscribeGet = true;
            }
            reportOverrun = reportOverrunAccess.get();
            if(queueCapacity<=0 && reportOverrun) {
                configStructure.message("reportOverrun ignored", MessageType.warning);
                reportOverrun = false;
            }
            process = processAccess.get();
            if(process) {
                boolean isProcessor = recordProcess.setRecordProcessRequestor(this);
                if(!isProcessor) {
                    configStructure.message("process is not possible", MessageType.error);
                    process = false;
                }
            }
            inheritSeverity = inheritSeverityAccess.get();
            if(!process && inheritSeverity) {
                configStructure.message("inheritSeverity ignored", MessageType.warning);
                inheritSeverity = false;
            }
            channelSubscribe = channel.createSubscribe();
            if(!isSubscribeGet) {
                channelGet = channel.createChannelGet(this,false);
            }
            if(channel.isConnected()) {
                channelStart();
            }
            
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            channelRecord = null;
            if(channel!=null) channel.destroy();
            channel = null;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            valueData = data;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#process(org.epics.ioc.dbProcess.LinkListener)
         */
        public void process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,monitorLinkSupportName + ".process")) {
                recordProcess.setStatusSeverity(
                        dbLink.getFullFieldName() + " not ready",
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
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!channel.isConnected()) {
                    channelRecord = null;
                    severityField = null;
                    dataField = null;
                    fieldGroup = null;
                    return;
                }
                channelStart();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
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
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestor#requestDone(org.epics.ioc.channelAccess.Channel, org.epics.ioc.dbProcess.RequestResult)
         */
        public void requestDone(Channel channel, RequestResult requestResult) {
            if(process) {
                recordProcess.process(this,false,null);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribeGetRequestor#message(org.epics.ioc.channelAccess.Channel, java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(Channel channel,String message, MessageType messageType) {
            dbLink.message(message, messageType);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribeGetRequestor#startSubscribeGetData()
         */
        public void startSubscribeGetData() {
            if(process) {
                recordProcess.setActive(this);
            }
            channelSubscribe.readyForData();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribeGetRequestor#dataOverrun(int)
         */
        public void dataOverrun(int numberSets) {
            if(!reportOverrun) return;
            if(process) {
                numberOverrun = numberSets;
                return;
            }
            dbRecord.lock();
            try {
                dbLink.message(
                    "missed " + Integer.toString(numberSets) + " notifications",
                    MessageType.warning);
            } finally {
                dbRecord.unlock();
            }
        }                
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribeGetRequestor#nextSubscribeGetData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void nextSubscribeGetData(Channel channel, ChannelField field, PVData data) {
            dbRecord.lock();
            try {
                if(field==severityField) {
                    PVEnum pvEnum = (PVEnum)data;
                    alarmSeverity = AlarmSeverity.getSeverity(pvEnum.getIndex());
                    return;
                }
                if(field!=dataField) {
                    dbLink.message(
                            "Logic error in MonitorLink field!=channelField",
                            MessageType.fatalError);
                    return;
                }
                Type linkType = data.getField().getType();
                Field valueField = valueData.getField();
                Type valueType = valueField.getType();
                if(valueType.isScalar() && linkType.isScalar()) {
                    convert.copyScalar(data,valueData);
                    return;
                }
                if(linkType==Type.pvArray && valueType==Type.pvArray) {
                    PVArray linkArrayData = (PVArray)data;
                    PVArray recordArrayData = (PVArray)valueData;
                    convert.copyArray(linkArrayData,0,
                        recordArrayData,0,linkArrayData.getLength());
                    return;
                }
                if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                    PVStructure linkStructureData = (PVStructure)data;
                    PVStructure recordStructureData = (PVStructure)valueData;
                    convert.copyStructure(linkStructureData,recordStructureData);
                    return;
                }
                dbLink.message(
                        "Logic error in MonitorLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                dbRecord.unlock();
            }
        }       
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#nextDelayedGetData(org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextDelayedGetData(PVData data) {
            // nothing to do
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelSubscribeRequestor#dataModified(org.epics.ioc.channelAccess.Channel)
         */
        public void dataModified(Channel channel) {
            if(process) {
                recordProcess.setActive(this);
            }
            channelGet.get(fieldGroup);
        }                    
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetRequestor#nextGetData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public boolean nextGetData(Channel channel,ChannelField field,PVData data) {           
            dbRecord.lock();
            try {
                if(field==severityField) {
                    PVEnum pvEnum = (PVEnum)data;
                    alarmSeverity = AlarmSeverity.getSeverity(pvEnum.getIndex());
                    return false;
                }
                if(field!=dataField) {
                    dbLink.message(
                            "Logic error in MonitorLink field!=channelField",
                            MessageType.fatalError);
                    return false;
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
                    convert.copyArray(linkArrayData,0,
                        recordArrayData,0,linkArrayData.getLength());
                    return false;
                }
                if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                    PVStructure linkStructureData = (PVStructure)data;
                    PVStructure recordStructureData = (PVStructure)valueData;
                    convert.copyStructure(linkStructureData,recordStructureData);
                    return false;
                }
                dbLink.message(
                        "Logic error in MonitorLink: unsupported type",
                        MessageType.fatalError);
            } finally {
                dbRecord.unlock();
            }
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessComplete(org.epics.ioc.dbProcess.RequestResult)
         */
        public void recordProcessComplete() {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessRequestor#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
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
                throw new IllegalStateException(
                "Logic Error: MonitorLink.connect bad return from setField");
            }
            dataField = channel.getChannelField();
            String errorMessage = checkCompatibility();
            if(errorMessage!=null) {
                dbLink.message(errorMessage,MessageType.error);
                return;
            }
            fieldGroup = channel.createFieldGroup(this);
            fieldGroup.addChannelField(dataField);
            if(inheritSeverityAccess.get()) {
                result = channel.setField("severity");
                if(result==ChannelSetFieldResult.thisChannel) {
                    severityField = channel.getChannelField();
                    fieldGroup.addChannelField(severityField);
                } else {
                    severityField = null;
                }
            }
            channelRecord = null;
            if(channel.isLocal()) {
                IOCDB iocdb = dbRecord.getIOCDB();
                channelRecord = iocdb.findRecord(dbRecord.getRecordName());
                if(channelRecord==null) {
                    throw new IllegalStateException(
                    "Logic Error: channel is local but cant find record");
                }
            }
            if(queueCapacity==0) {
                channelSubscribe.start(fieldGroup,(ChannelSubscribeRequestor)this,null);
            } else {
                channelSubscribe.start(fieldGroup,queueCapacity,(ChannelSubscribeGetRequestor)this,null);
            }
        }
    }
}
