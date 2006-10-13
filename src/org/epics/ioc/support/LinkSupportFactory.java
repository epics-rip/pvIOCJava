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
        dbLink.message("no support for " + supportName, IOCMessageType.fatalError);
        return null;
    }
    
    private static final String processLinkSupportName = "processLink";
    private static final String inputLinkSupportName = "inputLink";
    private static final String outputLinkSupportName = "outputLink";
    private static final String monitorLinkSupportName = "monitorLink";

    private static Convert convert = ConvertFactory.getConvert();
    private static Pattern periodPattern = Pattern.compile("[.]");
    
    private static class ProcessLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener, ChannelProcessListener
    {
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private Channel channel = null;
        
        private ChannelProcess channelProcess = null;
        private ProcessRequestListener processListener = null;

        private ProcessLink(DBLink dbLink) {
            super(processLinkSupportName,dbLink);
            this.dbLink = dbLink;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,processLinkSupportName)) return;
            dbRecord = dbLink.getRecord();
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = getConfigStructure(dbLink,"processLink");
            if(configStructure==null) return;
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            inheritSeverityAccess = getBoolean(this,configStructure,"inheritSeverity");
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
            // split pvname into record name and rest of name
            String name = pvnameAccess.get();
            if(name==null) {
                dbLink.message("pvname is not defined",IOCMessageType.error);
                return;
            }
            String[]pvname = periodPattern.split(name,2);
            String recordName = pvname[0];
            channel = ChannelFactory.createChannel(recordName,this);
            if(channel==null) {
                dbLink.message(
                    "Failed to create channel for " + recordName,
                    IOCMessageType.error);
                return;
            }
            if(channel.isLocal()) {
                ChannelLink channelLink = (ChannelLink)channel;
                channelLink.setDBLink(dbLink);
            }
            channelProcess = channel.createChannelProcess();
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            channelProcess.destroy();
            channelProcess = null;
            channel.destroy();
            channel = null;
            setSupportState(SupportState.readyForStart);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.AbstractSupport#process(org.epics.ioc.dbProcess.ProcessRequestListener)
         */
        public ProcessReturn process(ProcessRequestListener listener) {
            if(!super.checkSupportState(SupportState.ready,processLinkSupportName + ".process")) {
                return ProcessReturn.failure;
            }
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity(
                    dbLink.getFullFieldName() + " not connected",
                        AlarmSeverity.major);
                return ProcessReturn.failure;
            }
            processListener = listener;
            ChannelRequestReturn result = channelProcess.process(this);
            switch(result) {
            case active: return ProcessReturn.active;
            case success: return ProcessReturn.success;
            default: break;
            }
            recordProcessSupport.setStatusSeverity(
                dbLink.getRecord().getRecordName()
                + dbLink.getFullFieldName() + " process request failed",
                AlarmSeverity.major);
            return ProcessReturn.failure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData field) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            //nothing to do. Just wait until next process
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
         */
        public void disconnect(Channel c) {
            // record is not locked must not call uninitialize directly
            recordProcess.uninitialize();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(Channel channel, AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            if(inheritSeverityAccess.get()) {
                dbRecord.lock();
                try {
                    recordProcessSupport.setStatusSeverity("inherit" + status,alarmSeverity);
                } finally {
                    dbRecord.unlock();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel) {
            processListener.processComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel,String message) {
            dbLink.message(message, IOCMessageType.warning);
        }
    }
    
    private static class InputLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener, ChannelGetListener, ChannelFieldGroupListener
    {
        private DBLink dbLink;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private String fieldName = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private PVData valueData = null;
        
        private boolean process = false;
        
        private Channel channel = null;
        private DBRecord channelRecord = null;
        private ChannelGet channelGet = null;
        private ChannelField channelField = null;
        private ChannelFieldGroup fieldGroup = null;
        
        private ProcessRequestListener processListener = null;
        
        /**
         * Constructor for InputLink.
         * @param dbLink The field for which to create support.
         */
        public InputLink(DBLink dbLink) {
            super(inputLinkSupportName,dbLink);
            this.dbLink = dbLink;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,inputLinkSupportName)) return;
            dbRecord = dbLink.getRecord();
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = getConfigStructure(dbLink,"inputLink");
            if(configStructure==null) return;
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = getBoolean(this,configStructure,"process");
            if(processAccess==null) return;
            inheritSeverityAccess = getBoolean(this,configStructure,"inheritSeverity");
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
            if(valueData==null) {
                dbLink.message(
                    "Logic Error: InputLink.start called before setField",
                    IOCMessageType.error);
                setSupportState(SupportState.zombie);
                return;
            }
            process = processAccess.get();
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
                        IOCMessageType.error);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            if(channel.isLocal()) {
                ChannelLink channelLink = (ChannelLink)channel;
                channelLink.setDBLink(dbLink);
            }
            channelGet = channel.createChannelGet();
            if(channel.isConnected()) prepareForInput();
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
        public ProcessReturn process(ProcessRequestListener listener) {
            if(!super.checkSupportState(SupportState.ready,inputLinkSupportName + ".process")) return ProcessReturn.failure;
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                return ProcessReturn.failure;
            }
            if(channelField==null) {
                recordProcessSupport.setStatusSeverity("Link is not prepared for input",
                        AlarmSeverity.invalid);
                    return ProcessReturn.failure;
            }
            processListener = listener;
            ChannelRequestReturn result = channelGet.get(fieldGroup,this,process);
            switch(result) {
            case active: return ProcessReturn.active;
            case success: return ProcessReturn.success;
            default: break;
            }
            recordProcessSupport.setStatusSeverity(
                dbLink.getRecord().getRecordName()
                + dbLink.getFullFieldName() + " get request failed",
                AlarmSeverity.major);
            return ProcessReturn.failure;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!isConnected) {
                    if(fieldGroup!=null) fieldGroup.destroy();
                    fieldGroup = null;
                    channelRecord = null;
                    channelField = null;
                    return;
                } else {
                    prepareForInput();
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
         */
        public void disconnect(Channel c) {
            //  record is not locked must not call uninitialize directly
            recordProcess.uninitialize();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetListener#beginSynchronous()
         */
        public void beginSynchronous(Channel channel) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetListener#endSynchronous()
         */
        public void endSynchronous(Channel channel) {
            // nothing to do.
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetListener#newData(org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void newData(Channel channel,ChannelField field,PVData data) {
            if(field!=channelField) {
                dbLink.message(
                    "Logic error in InputLink field!=channelField",
                    IOCMessageType.fatalError);
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
                    "Logic error in InputLink: unsupported type",
                    IOCMessageType.fatalError);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel) {
            processListener.processComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(Channel channel, AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            if(inheritSeverityAccess.get()) {
                recordProcessSupport.setStatusSeverity("inherit" + status,alarmSeverity);
            }
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#message(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel,String message) {
            dbLink.message(message, IOCMessageType.warning);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
        
        private void prepareForInput() {
            String errorMessage = null;
            ChannelSetFieldResult result = channel.setField(fieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
                dbLink.message(
                    "Logic Error: InputLink.connect bad return from setField",
                    IOCMessageType.error);
                return;
            }
            channelField = channel.getChannelField();
            errorMessage = checkCompatibility();
            if(errorMessage!=null) {
                channelField = null;
                dbLink.message(errorMessage,IOCMessageType.error);
                return;
            }
            fieldGroup = channel.createFieldGroup(this);
            fieldGroup.addChannelField(channelField);
            channelRecord = null;
            if(channel.isLocal()) {
                IOCDB iocdb = dbRecord.getIOCDB();
                channelRecord = iocdb.findRecord(dbRecord.getRecordName());
                if(channelRecord==null) {
                    throw new IllegalStateException(
                    "Logic Error: channel is local but can't find record");
                }
            }
        }
        
        private String checkCompatibility() {
            Type linkType = channelField.getField().getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(channelField.getField(),valueField)) return null;
            } else if(linkType==Type.pvArray && valueType==Type.pvArray) {
                Array linkArray = (Array)channelField;
                Array recordArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return null;
            } else if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure linkStructure = (Structure)channelField;
                Structure recordStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return null;
            }
            String errorMessage =
                "is not compatible with pvname " + pvnameAccess.get();
            channel = null;
            return errorMessage;
        }
    }
    
    private static class OutputLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener, ChannelPutListener, ChannelFieldGroupListener
    {
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private String fieldName = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private PVData valueData = null;
        
        private boolean process = false;
        
        private Channel channel = null;
        private DBRecord channelRecord = null;
        private ChannelPut dataPut = null;
        private ChannelField channelField = null;
        private ChannelFieldGroup fieldGroup = null;
        
        private ProcessRequestListener processListener = null;
        
        /**
         * Constructor for an OutputLink
         * @param dbLink
         */
        public OutputLink(DBLink dbLink) {
            super(outputLinkSupportName,dbLink);
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,outputLinkSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = getConfigStructure(dbLink,"outputLink");
            if(configStructure==null) return;
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = getBoolean(this,configStructure,"process");
            if(processAccess==null) return;
            inheritSeverityAccess = getBoolean(this,configStructure,"inheritSeverity");
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
            if(!super.checkSupportState(SupportState.readyForStart,outputLinkSupportName)) return;
            if(valueData==null) {
                dbLink.message(
                        "Logic Error: OutputLink.start called before setField",
                        IOCMessageType.fatalError);
                setSupportState(SupportState.zombie);
                return;
            }
            process = processAccess.get();
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
                        IOCMessageType.error);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            if(channel.isLocal()) {
                ChannelLink channelLink = (ChannelLink)channel;
                channelLink.setDBLink(dbLink);
            }
            
            dataPut = channel.createChannelPut();
            if(channel.isConnected()) prepareForOutput();
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
        public ProcessReturn process(ProcessRequestListener listener) {
            if(!super.checkSupportState(SupportState.ready,outputLinkSupportName + ".process")) return ProcessReturn.failure;
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                return ProcessReturn.failure;
            }
            if(channelField==null) {
                recordProcessSupport.setStatusSeverity("Link is not prepared for output",
                    AlarmSeverity.invalid);
                return ProcessReturn.failure;
            }
            processListener = listener;
            ChannelRequestReturn result = dataPut.put(fieldGroup,this,process);
            switch(result) {
            case active: return ProcessReturn.active;
            case success: return ProcessReturn.success;
            default: break;
            }
            recordProcessSupport.setStatusSeverity(
                dbLink.getRecord().getRecordName()
                 + dbLink.getFullFieldName() + " put request failed",
                AlarmSeverity.major);
            return ProcessReturn.failure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!channel.isConnected()) {
                    if(fieldGroup!=null) fieldGroup.destroy();
                    fieldGroup = null;
                    channelRecord = null;
                    channelField = null;
                    return;
                }
                prepareForOutput();
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
                uninitialize();
                switch(supportState) {
                case readyForInitialize: break;
                case readyForStart: initialize(); break;
                case ready: initialize(); start(); break;
                case zombie: break;
                }
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#nextData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void nextData(Channel channel, ChannelField field, PVData data) {
            if(field!=channelField) {
                dbLink.message(
                        "Logic error in OutputLink field!=channelField",
                        IOCMessageType.fatalError);
            }
            Type linkType = data.getField().getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                convert.copyScalar(valueData,data);
                return;
            }
            if(linkType==Type.pvArray && valueType==Type.pvArray) {
                PVArray linkArrayData = (PVArray)data;
                PVArray recordArrayData = (PVArray)valueData;
                convert.copyArray(recordArrayData,0,linkArrayData,0,recordArrayData.getLength());
                return;
            }
            if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                PVStructure linkStructureData = (PVStructure)data;
                PVStructure recordStructureData = (PVStructure)valueData;
                convert.copyStructure(recordStructureData,linkStructureData);
                return;
            }
            dbLink.message(
                    "Logic error in OutputLink: unsupported type",
                    IOCMessageType.fatalError);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel) {
            processListener.processComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(Channel channel, AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            if(inheritSeverityAccess.get()) {
                recordProcessSupport.setStatusSeverity("inherit" + status,alarmSeverity);
            }
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#failure(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void message(Channel channel,String message) {
            dbLink.message(message, IOCMessageType.warning);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
        
        private void prepareForOutput() {
            String errorMessage = null;
            ChannelSetFieldResult result = channel.setField(fieldName);
            if(result!=ChannelSetFieldResult.thisChannel) {
                throw new IllegalStateException(
                "Logic Error: OutputLink.connect bad return from setField");
            }
            channelField = channel.getChannelField();
            errorMessage = checkCompatibility();
            if(errorMessage!=null) {
                dbLink.message(errorMessage,IOCMessageType.error);
                return;
            }
            fieldGroup = channel.createFieldGroup(this);
            fieldGroup.addChannelField(channelField);
            channelRecord = null;
            if(channel.isLocal()) {
                IOCDB iocdb = dbRecord.getIOCDB();
                channelRecord = iocdb.findRecord(dbRecord.getRecordName());
                if(channelRecord==null) {
                    throw new IllegalStateException(
                    "Logic Error: channel is local but cant find record");
                }
            }
        }      
        private String checkCompatibility() {
            Type linkType = channelField.getField().getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(channelField.getField(),valueField)) return null;
            } else if(linkType==Type.pvArray && valueType==Type.pvArray) {
                Array linkArray = (Array)channelField;
                Array recordArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return null;
            } else if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure linkStructure = (Structure)channelField;
                Structure recordStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return null;
            }
            String errorMessage = 
                "is not compatible with pvname " + pvnameAccess.get();
            channel = null;
            return errorMessage;
        }
    }
    
    private static class MonitorLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener,ChannelFieldGroupListener,
    ChannelNotifyGetListener, ChannelNotifyListener
    {
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private String fieldName = null;
        private PVBoolean processAccess = null;
        private PVInt queueCapacityAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private PVData valueData = null;
        
        private boolean process = false;
        private int queueCapacity = 0;
        
        private Channel channel = null;
        private DBRecord channelRecord = null;
        private ChannelSubscribe channelSubscribe = null;
        private ChannelField linkField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup fieldGroup = null;
        
        
        /**
         * Constructor for MonitorLink.
         * @param dbLink The field for which to create support.
         */
        public MonitorLink(DBLink dbLink) {
            super(monitorLinkSupportName,dbLink);
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,monitorLinkSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = getConfigStructure(dbLink,"monitorLink");
            if(configStructure==null) return;
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = getBoolean(this,configStructure,"process");
            if(processAccess==null) return;
            queueCapacityAccess = getInt(this,configStructure,"queueCapacity");
            if(queueCapacityAccess==null) return;
            inheritSeverityAccess = getBoolean(this,configStructure,"inheritSeverity");
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
                        IOCMessageType.fatalError);
                setSupportState(SupportState.zombie);
                return;
            }
            process = processAccess.get();
            queueCapacity = queueCapacityAccess.get();
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
                        IOCMessageType.error);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            if(channel.isLocal()) {
                ChannelLink channelLink = (ChannelLink)channel;
                channelLink.setDBLink(dbLink);
            }
            
            channelSubscribe = channel.createSubscribe(queueCapacity);
            if(channel.isConnected()) {
                channelStateChange(channel,true);
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
        public ProcessReturn process(ProcessRequestListener listener) {
            if(!super.checkSupportState(SupportState.ready,monitorLinkSupportName + ".process")) return ProcessReturn.failure;
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                return ProcessReturn.failure;
            }
            return ProcessReturn.success;
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
                    linkField = null;
                    if(fieldGroup!=null) fieldGroup.destroy();
                    fieldGroup = null;
                    return;
                }
                ChannelSetFieldResult result = channel.setField(fieldName);
                if(result!=ChannelSetFieldResult.thisChannel) {
                    throw new IllegalStateException(
                    "Logic Error: MonitorLink.connect bad return from setField");
                }
                linkField = channel.getChannelField();
                String errorMessage = checkCompatibility();
                if(errorMessage!=null) {
                    dbLink.message(errorMessage,IOCMessageType.error);
                    return;
                }
                fieldGroup = channel.createFieldGroup(this);
                fieldGroup.addChannelField(linkField);
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
                    channelSubscribe.start(fieldGroup,(ChannelNotifyListener)this,null);
                } else {
                    channelSubscribe.start(fieldGroup,(ChannelNotifyGetListener)this,null);
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
                uninitialize();
                switch(supportState) {
                case readyForInitialize: break;
                case readyForStart: initialize(); break;
                case ready: initialize(); start(); break;
                case zombie: break;
                }
            } finally {
                dbRecord.unlock();
            }
        }
        
        public void beginSynchronous(Channel channel) {
            // nothing to do
        }

        
        public void endSynchronous(Channel channel) {
            if(!process) return;
            recordProcess.process(null);
        }
      
        public void newData(Channel channel,ChannelField field,PVData data) {
            if(field==severityField) {
                PVEnum pvEnum = (PVEnum)data;
                AlarmSeverity severity = AlarmSeverity.getSeverity(
                    pvEnum.getIndex());
                if(severity!=AlarmSeverity.none) {
                    recordProcess.getRecordProcessSupport().setStatusSeverity("inherit severity",severity);
                }
                return;
            }
            if(field!=linkField) {
                dbLink.message(
                        "Logic error in MonitorLink field!=channelField",
                        IOCMessageType.fatalError);
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
                    IOCMessageType.fatalError);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelNotifyListener#newData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField)
         */
        public void newData(Channel channel, ChannelField field) {
            // nothing to do
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestDone(org.epics.ioc.channelAccess.Channel)
         */
        public void requestDone(Channel channel) {
            // What to do ???
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelRequestListener#requestResult(org.epics.ioc.channelAccess.Channel, org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void requestResult(Channel channel, AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // What to do???
            
        }
        public void message(Channel channel,String message) {
            // What to do????
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
            
        private String checkCompatibility() {
            Type linkType = linkField.getField().getType();
            Field valueField = valueData.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(linkField.getField(),valueField)) return null;
            } else if(linkType==Type.pvArray && valueType==Type.pvArray) {
                Array linkArray = (Array)linkField;
                Array recordArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return null;
            } else if(linkType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure linkStructure = (Structure)linkField;
                Structure recordStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return null;
            }
            String errorMessage = 
                "is not compatible with pvname " + pvnameAccess.get();
            channel = null;
            return errorMessage;
        }
    }
    
    private static DBStructure getConfigStructure(DBLink dbLink,String structureName) {
        DBStructure configStructure = dbLink.getConfigurationStructure();
        if(configStructure==null) {
            dbLink.message("no configuration structure", IOCMessageType.fatalError);
            return null;
        }
        Structure structure = (Structure)configStructure.getField();
        String configStructureName = structure.getStructureName();
        if(!configStructureName.equals(structureName)) {
            configStructure.message(
                    "configurationStructure name is " + configStructureName
                    + " but expecting " + structureName,
                IOCMessageType.fatalError);
            return null;
        }
        return configStructure;
    }
    
    private static PVString getString(AbstractSupport support,
    DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                IOCMessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvString) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type string ",
                IOCMessageType.error);
            return null;
        }
        return (PVString)dbData[index];
    }
    
    private static PVBoolean getBoolean(AbstractSupport support,
    DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                IOCMessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvBoolean) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type boolean ",
                IOCMessageType.error);
            return null;
        }
        return (PVBoolean)dbData[index];
    }
    
    private static PVInt getInt(AbstractSupport support,
            DBStructure configStructure,String fieldName)
            {
                DBData[] dbData = configStructure.getFieldDBDatas();
                int index = configStructure.getFieldDBDataIndex(fieldName);
                if(index<0) {
                    configStructure.message(
                        "configStructure does not have field" + fieldName,
                        IOCMessageType.error);
                    return null;
                }
                if(dbData[index].getField().getType()!=Type.pvInt) {
                    dbData[index].message(
                        "configStructure field "
                        + fieldName + " does not have type int ",
                        IOCMessageType.error);
                    return null;
                }
                return (PVInt)dbData[index];
            }
    
    private static PVDouble getDouble(AbstractSupport support,
    DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            configStructure.message(
                "configStructure does not have field" + fieldName,
                IOCMessageType.error);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvDouble) {
            dbData[index].message(
                "configStructure field "
                + fieldName + " does not have type double ",
                IOCMessageType.error);
            return null;
        }
        return (PVDouble)dbData[index];
    }
}
