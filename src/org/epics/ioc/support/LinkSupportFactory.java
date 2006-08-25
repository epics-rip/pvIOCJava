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
        LinkSupport support = null;
        String supportName = dbLink.getSupportName();
        if(supportName.equals("processLink")) {
            support = new ProcessLink(dbLink);
        } else if(supportName.equals("inputLink")) {
            support = new InputLink(dbLink);
        } else if(supportName.equals("outputLink")) {
            support = new OutputLink(dbLink);
        }else if(supportName.equals("monitorLink")) {
            support = new MonitorLink(dbLink);
        }
        return support;
    }

    private static Convert convert = ConvertFactory.getConvert();
    static private Pattern periodPattern = Pattern.compile("[.]");
    
    private static PVString getString(AbstractSupport support,
    DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            support.errorMessage(
                "InputLink.initialize: configStructure does not have field"
                + fieldName);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvString) {
            support.errorMessage(
                "InputLink.initialize: configStructure field "
                + fieldName + " is not a string ");
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
            support.errorMessage(
                "InputLink.initialize: configStructure does not have field"
                + fieldName);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvBoolean) {
            support.errorMessage(
                "InputLink.initialize: configStructure field "
                + fieldName + " is not a boolean ");
            return null;
        }
        return (PVBoolean)dbData[index];
    }
    
    private static PVDouble getDouble(AbstractSupport support,
    DBStructure configStructure,String fieldName)
    {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            support.errorMessage(
                "InputLink.initialize: configStructure does not have field"
                + fieldName);
            return null;
        }
        if(dbData[index].getField().getType()!=Type.pvDouble) {
            support.errorMessage(
                "InputLink.initialize: configStructure field "
                + fieldName + " is not a double ");
            return null;
        }
        return (PVDouble)dbData[index];
    }
    
    private static class ProcessLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener, ChannelProcessListener,ChannelFieldGroupListener
    {
        private static String supportName = "ProcessLink";
        private SupportState supportState = SupportState.readyForInitialize;
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private String recordName = null;
        private PVBoolean waitAccess = null;
        private PVDouble timeoutAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        private PVBoolean forceLocalAccess = null;
        
        private boolean wait = false;
        
        private Channel channel = null;
        private DBRecord channelRecord = null;
        
        private ChannelProcess channelProcess = null;
        private ProcessCompleteListener processListener = null;
        private AlarmSeverity alarmSeverity = AlarmSeverity.invalid;
        private String status = null;
        private ProcessResult processResult = ProcessResult.failure;

        private ProcessLink(DBLink dbLink) {
            super(supportName,dbLink);
            this.dbLink = dbLink;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(supportState!=SupportState.readyForInitialize) return;
            dbRecord = dbLink.getRecord();
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = dbLink.getConfigurationStructure();
            Structure structure = (Structure)configStructure.getField();
            String configStructureName = structure.getStructureName();
            if(!configStructureName.equals("processLink")) {
                throw new IllegalStateException(
                    "InputLink.initialize: configStructure name is "
                    + configStructureName
                    + " but expecting inputLink"
                    );
            }
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            waitAccess = getBoolean(this,configStructure,"wait");
            if(waitAccess==null) return;
            timeoutAccess = getDouble(this,configStructure,"timeout");
            if(timeoutAccess==null) return;
            inheritSeverityAccess = getBoolean(this,configStructure,"inheritSeverity");
            if(inheritSeverityAccess==null) return;
            forceLocalAccess = getBoolean(this,configStructure,"forceLocal");
            if(forceLocalAccess==null) return;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            stop();
            supportState = SupportState.readyForInitialize;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(supportState!=SupportState.readyForStart) return;
            wait = waitAccess.get();
            // split pvname into record name and rest of name
            String[]pvname = periodPattern.split(pvnameAccess.get(),2);
            recordName = pvname[0];
            channel = ChannelFactory.createChannel(recordName,this);
            if(channel==null) return;
            if(channel.isLocal()) {
                ChannelLink channelLocal = (ChannelLink)channel;
                channelLocal.setLinkRecord(dbLink.getRecord());
            }
            channelProcess = channel.createChannelProcess();
            supportState = SupportState.ready;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(supportState!=SupportState.ready) return;
            channelRecord = null;
            wait = false;
            if(channel!=null) channel.destroy();
            channel = null;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#process(org.epics.ioc.dbProcess.ProcessCompleteListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                errorMessage(
                        "process called but supportState is "
                        + supportState.toString());
                return ProcessReturn.failure;
            }
            if(channel==null) return ProcessReturn.failure;
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity(
                    dbLink.getFullFieldName() + " not connected",
                        AlarmSeverity.major);
                return ProcessReturn.failure;
            }
            processListener = listener;
            channelProcess.process(this,wait);
            return ProcessReturn.active;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            if(inheritSeverityAccess.get() && alarmSeverity!=AlarmSeverity.none) {
                recordProcessSupport.setStatusSeverity("inherit" + status,alarmSeverity);
            }
            processListener.processComplete(this,processResult);
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
        public void channelStateChange(Channel c) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!channel.isConnected()) {
                    channelRecord = null;
                    return;
                }
                String errorMessage = null;
                if(forceLocalAccess.get() && !channel.isLocal()) {
                    errorMessage = String.format(
                        "%s.%s pvname %s is not local",
                        dbLink.getRecord().getRecordName(),
                        dbLink.getDBDField().getName(),
                        pvnameAccess.get());
                    errorMessage(errorMessage);
                    setSupportState(SupportState.readyForInitialize);
                }
                channel.setTimeout(timeoutAccess.get());
                channelRecord = null;
                if(channel.isLocal()) {
                    IOCDB iocdb = dbRecord.getRecordProcess().getProcessDB().getIOCDB();
                    channelRecord = iocdb.findRecord(recordName);
                    if(channelRecord==null) {
                        throw new IllegalStateException(
                        "Logic Error: channel is local but cant find record");
                    }
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
                uninitialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#processDone(org.epics.ioc.dbProcess.ProcessResult, org.epics.ioc.util.AlarmSeverity, java.lang.String)
         */
        public void processDone(Channel channel,ProcessResult result,AlarmSeverity alarmSeverity,String status) {
            this.alarmSeverity = alarmSeverity;
            this.status = status;
            processResult = result;
            recordProcessSupport.processContinue(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelProcessListener#failure(java.lang.String)
         */
        public void failure(Channel channel,String reason) {
            alarmSeverity = AlarmSeverity.major;
            status = "linked process failed";
            processResult = ProcessResult.failure;
            recordProcessSupport.processContinue(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
    }
    
    private static class InputLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener, ChannelGetListener, ChannelFieldGroupListener
    {
        private static String supportName = "InputLink";
        
        private SupportState supportState = SupportState.readyForInitialize;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private String recordName = null;
        private String fieldName = null;
        private PVBoolean processAccess = null;
        private PVBoolean waitAccess = null;
        private PVDouble timeoutAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        private PVBoolean forceLocalAccess = null;
        
        private PVData recordData = null;
        
        private boolean process = false;
        private boolean wait = false;
        
        private Channel channel = null;
        private DBRecord channelRecord = null;
        private ChannelGet channelGet = null;
        private ChannelField linkField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup fieldGroup = null;
        
        private boolean isSynchronous = false;
        private ProcessResult processResult = ProcessResult.failure;
        private ProcessReturn processReturn = ProcessReturn.failure;
        private ProcessCompleteListener processListener = null;
        
        /**
         * Constructor for InputLink.
         * @param dbLink The field for which to create support.
         */
        public InputLink(DBLink dbLink) {
            super(supportName,dbLink);
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(supportState!=SupportState.readyForInitialize) return;
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = dbLink.getConfigurationStructure();
            Structure structure = (Structure)configStructure.getField();
            String configStructureName = structure.getStructureName();
            if(!configStructureName.equals("inputLink")) {
                errorMessage(
                    "InputLink.initialize: configStructure name is "
                    + configStructureName
                    + " but expecting inputLink"
                    );
                return;
            }
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = getBoolean(this,configStructure,"process");
            if(processAccess==null) return;
            waitAccess = getBoolean(this,configStructure,"wait");
            if(waitAccess==null) return;
            timeoutAccess = getDouble(this,configStructure,"timeout");
            if(timeoutAccess==null) return;
            inheritSeverityAccess = getBoolean(this,configStructure,"inheritSeverity");
            if(inheritSeverityAccess==null) return;
            forceLocalAccess = getBoolean(this,configStructure,"forceLocal");
            if(forceLocalAccess==null) return;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            stop();
            supportState = SupportState.readyForInitialize;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(supportState!=SupportState.readyForStart) return;
            if(recordData==null) {
                errorMessage(
                    "Logic Error: InputLink.start called before setField");
                setSupportState(SupportState.zombie);
                return;
            }
            process = processAccess.get();
            wait = waitAccess.get();
            // split pvname into record name and rest of name
            String[]pvname = periodPattern.split(pvnameAccess.get(),2);
            recordName = pvname[0];
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }
            channel = ChannelFactory.createChannel(recordName,this);
            if(channel==null) {
                errorMessage("Failed to create channel for " + recordName);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            if(channel.isLocal()) {
                ChannelLink channelLocal = (ChannelLink)channel;
                channelLocal.setLinkRecord(dbLink.getRecord());
            }
            
            channelGet = channel.createChannelGet();
            if(channel.isConnected()) {
                channelStateChange(channel);
            }
            supportState = SupportState.ready;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(supportState!=SupportState.ready) return;
            channelRecord = null;
            if(channel!=null) channel.destroy();
            channel = null;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            recordData = data;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#process(org.epics.ioc.dbProcess.LinkListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                errorMessage(
                    "process called but supportState is "
                    + supportState.toString());
                return ProcessReturn.failure;
            }
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                return ProcessReturn.failure;
            }
            processListener = listener;
            isSynchronous = true;
            processResult = ProcessResult.success;
            processReturn = ProcessReturn.active;
            channelGet.get(fieldGroup,this,process,wait);
            isSynchronous = false;
            if(processReturn==ProcessReturn.active) return processReturn;
            if(processResult==ProcessResult.success) return ProcessReturn.success;
            return ProcessReturn.failure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            processListener.processComplete(this,processResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c) {
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
                String errorMessage = null;
                if(forceLocalAccess.get() && !channel.isLocal()) {
                    errorMessage = String.format(
                        "%s.%s pvname %s is not local",
                        dbLink.getRecord().getRecordName(),
                        dbLink.getDBDField().getName(),
                        pvnameAccess.get());
                    errorMessage(errorMessage);
                    setSupportState(SupportState.readyForInitialize);
                }
                ChannelSetFieldResult result = channel.setField(fieldName);
                if(result!=ChannelSetFieldResult.thisChannel) {
                    throw new IllegalStateException(
                    "Logic Error: InputLink.connect bad return from setField");
                }
                linkField = channel.getChannelField();
                errorMessage = checkCompatibility();
                if(errorMessage!=null) {
                    errorMessage(errorMessage);
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
                channel.setTimeout(timeoutAccess.get());
                channelRecord = null;
                if(channel.isLocal()) {
                    IOCDB iocdb = dbRecord.getRecordProcess().getProcessDB().getIOCDB();
                    channelRecord = iocdb.findRecord(recordName);
                    if(channelRecord==null) {
                        throw new IllegalStateException(
                        "Logic Error: channel is local but cant find record");
                    }
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
                uninitialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetListener#beginSynchronous()
         */
        public void beginSynchronous(Channel channel) {
            processReturn = ProcessReturn.success;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetListener#endSynchronous()
         */
        public void endSynchronous(Channel channel) {
            processResult = ProcessResult.success;
            if(!isSynchronous) {
                recordProcessSupport.processContinue(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetListener#newData(org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
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
                errorMessage("Logic error in InputLink field!=linkField");
                processResult = ProcessResult.failure;
            }
            Type linkType = data.getField().getType();
            Field recordField = recordData.getField();
            Type recordType = recordField.getType();
            if(recordType.isScalar() && linkType.isScalar()) {
                convert.copyScalar(data,recordData);
                return;
            }
            if(linkType==Type.pvArray && recordType==Type.pvArray) {
                PVArray linkArrayData = (PVArray)data;
                PVArray recordArrayData = (PVArray)recordData;
                convert.copyArray(linkArrayData,0,
                    recordArrayData,0,linkArrayData.getLength());
                return;
            }
            if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
                PVStructure linkStructureData = (PVStructure)data;
                PVStructure recordStructureData = (PVStructure)recordData;
                convert.copyStructure(linkStructureData,recordStructureData);
                return;
            }
            errorMessage("Logic error in InputLink: unsupported type");
            processResult = ProcessResult.failure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelGetListener#failure(java.lang.String)
         */
        public void failure(Channel channel,String reason) {
            processResult = ProcessResult.failure;
            if(!isSynchronous) {
                recordProcessSupport.processContinue(this);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
        
        
        private String checkCompatibility() {
            Type linkType = linkField.getField().getType();
            Field recordField = recordData.getField();
            Type recordType = recordField.getType();
            if(recordType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(linkField.getField(),recordField)) return null;
            } else if(linkType==Type.pvArray && recordType==Type.pvArray) {
                Array linkArray = (Array)linkField;
                Array recordArray = (Array)recordField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return null;
            } else if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
                Structure linkStructure = (Structure)linkField;
                Structure recordStructure = (Structure)recordField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return null;
            }
            String errorMessage = String.format(
                "%s.%s is not compatible with pvname %s",
                dbLink.getRecord().getRecordName(),
                dbLink.getDBDField().getName(),
                pvnameAccess.get());
            channel = null;
            return errorMessage;
        }
    }
    
    private static class OutputLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener, ChannelPutListener, ChannelFieldGroupListener
    {
        private static String supportName = "OutputLink";
        
        private SupportState supportState = SupportState.readyForInitialize;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private String recordName = null;
        private String fieldName = null;
        private PVBoolean processAccess = null;
        private PVBoolean waitAccess = null;
        private PVDouble timeoutAccess = null;
        private PVBoolean forceLocalAccess = null;
        
        private PVData recordData = null;
        
        private boolean process = false;
        private boolean wait = false;
        
        private Channel channel = null;
        private DBRecord channelRecord = null;
        private ChannelPut dataPut = null;
        private ChannelField linkField = null;
        private ChannelFieldGroup fieldGroup = null;
        
        private boolean isSynchronous = false;
        private ProcessResult processResult = ProcessResult.failure;
        private ProcessReturn processReturn = ProcessReturn.failure;
        private ProcessCompleteListener processListener = null;
        
        /**
         * Constructor for an OutputLink
         * @param dbLink
         */
        public OutputLink(DBLink dbLink) {
            super(supportName,dbLink);
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(supportState!=SupportState.readyForInitialize) return;
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = dbLink.getConfigurationStructure();
            Structure structure = (Structure)configStructure.getField();
            String configStructureName = structure.getStructureName();
            if(!configStructureName.equals("outputLink")) {
                errorMessage(
                    "OutputLink.initialize: configStructure name is "
                    + configStructureName
                    + " but expecting outputLink"
                    );
                return;
            }
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = getBoolean(this,configStructure,"process");
            if(processAccess==null) return;
            waitAccess = getBoolean(this,configStructure,"wait");
            if(waitAccess==null) return;
            timeoutAccess = getDouble(this,configStructure,"timeout");
            if(timeoutAccess==null) return;
            forceLocalAccess = getBoolean(this,configStructure,"forceLocal");
            if(forceLocalAccess==null) return;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            stop();
            supportState = SupportState.readyForInitialize;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(supportState!=SupportState.readyForStart) return;
            if(recordData==null) {
                errorMessage(
                    "Logic Error: OutputLink.start called before setField");
                setSupportState(SupportState.zombie);
                return;
            }
            process = processAccess.get();
            wait = waitAccess.get();
            // split pvname into record name and rest of name
            String[]pvname = periodPattern.split(pvnameAccess.get(),2);
            recordName = pvname[0];
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }
            channel = ChannelFactory.createChannel(recordName,this);
            if(channel==null) {
                errorMessage("Failed to create channel for " + recordName);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            if(channel.isLocal()) {
                ChannelLink channelLocal = (ChannelLink)channel;
                channelLocal.setLinkRecord(dbLink.getRecord());
            }
            
            dataPut = channel.createChannelPut();
            if(channel.isConnected()) {
                channelStateChange(channel);
            }
            supportState = SupportState.ready;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(supportState!=SupportState.ready) return;
            channelRecord = null;
            if(channel!=null) channel.destroy();
            channel = null;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            recordData = data;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#process(org.epics.ioc.dbProcess.LinkListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                errorMessage(
                    "process called but supportState is "
                    + supportState.toString());
                return ProcessReturn.failure;
            }
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                return ProcessReturn.failure;
            }
            processListener = listener;
            isSynchronous = true;
            processResult = ProcessResult.success;
            processReturn = ProcessReturn.active;
            dataPut.put(fieldGroup,this,process,wait);
            isSynchronous = false;
            if(processReturn==ProcessReturn.active) return processReturn;
            if(processResult==ProcessResult.success) return ProcessReturn.success;
            return ProcessReturn.failure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            processListener.processComplete(this,processResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c) {
            assert(c==channel);
            dbRecord.lock();
            try {
                if(!channel.isConnected()) {
                    channelRecord = null;
                    linkField = null;
                    if(fieldGroup!=null) fieldGroup.destroy();
                    fieldGroup = null;
                    return;
                }
                String errorMessage = null;
                if(forceLocalAccess.get() && !channel.isLocal()) {
                    errorMessage = String.format(
                        "%s.%s pvname %s is not local",
                        dbLink.getRecord().getRecordName(),
                        dbLink.getDBDField().getName(),
                        pvnameAccess.get());
                    errorMessage(errorMessage);
                    setSupportState(SupportState.readyForInitialize);
                }
                ChannelSetFieldResult result = channel.setField(fieldName);
                if(result!=ChannelSetFieldResult.thisChannel) {
                    throw new IllegalStateException(
                    "Logic Error: OutputLink.connect bad return from setField");
                }
                linkField = channel.getChannelField();
                errorMessage = checkCompatibility();
                if(errorMessage!=null) {
                    errorMessage(errorMessage);
                    return;
                }
                fieldGroup = channel.createFieldGroup(this);
                fieldGroup.addChannelField(linkField);
                channel.setTimeout(timeoutAccess.get());
                channelRecord = null;
                if(channel.isLocal()) {
                    IOCDB iocdb = dbRecord.getRecordProcess().getProcessDB().getIOCDB();
                    channelRecord = iocdb.findRecord(recordName);
                    if(channelRecord==null) {
                        throw new IllegalStateException(
                        "Logic Error: channel is local but cant find record");
                    }
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
                uninitialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#nextData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField, org.epics.ioc.pvAccess.PVData)
         */
        public void nextData(Channel channel, ChannelField field, PVData data) {
            if(field!=linkField) {
                errorMessage("Logic error in OutputLink field!=linkField");
                processResult = ProcessResult.failure;
            }
            Type linkType = data.getField().getType();
            Field recordField = recordData.getField();
            Type recordType = recordField.getType();
            if(recordType.isScalar() && linkType.isScalar()) {
                convert.copyScalar(recordData,data);
                return;
            }
            if(linkType==Type.pvArray && recordType==Type.pvArray) {
                PVArray linkArrayData = (PVArray)data;
                PVArray recordArrayData = (PVArray)recordData;
                convert.copyArray(recordArrayData,0,linkArrayData,0,recordArrayData.getLength());
                return;
            }
            if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
                PVStructure linkStructureData = (PVStructure)data;
                PVStructure recordStructureData = (PVStructure)recordData;
                convert.copyStructure(recordStructureData,linkStructureData);
                return;
            }
            errorMessage("Logic error in OutputLink: unsupported type");
            processResult = ProcessResult.failure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#processDone(org.epics.ioc.channelAccess.Channel, org.epics.ioc.dbProcess.ProcessResult, org.epics.ioc.util.AlarmSeverity, java.lang.String)
         */
        public void processDone(Channel channel, ProcessResult result, AlarmSeverity alarmSeverity, String status) {
            processResult = ProcessResult.success;
            if(!isSynchronous) {
                recordProcessSupport.processContinue(this);
            }
            
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelPutListener#failure(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void failure(Channel channel,String reason) {
            processResult = ProcessResult.failure;
            if(!isSynchronous) {
                recordProcessSupport.processContinue(this);
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
        }
              
        private String checkCompatibility() {
            Type linkType = linkField.getField().getType();
            Field recordField = recordData.getField();
            Type recordType = recordField.getType();
            if(recordType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(linkField.getField(),recordField)) return null;
            } else if(linkType==Type.pvArray && recordType==Type.pvArray) {
                Array linkArray = (Array)linkField;
                Array recordArray = (Array)recordField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return null;
            } else if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
                Structure linkStructure = (Structure)linkField;
                Structure recordStructure = (Structure)recordField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return null;
            }
            String errorMessage = String.format(
                "%s.%s is not compatible with pvname %s",
                dbLink.getRecord().getRecordName(),
                dbLink.getDBDField().getName(),
                pvnameAccess.get());
            channel = null;
            return errorMessage;
        }
    }
    
    private static class MonitorLink extends AbstractSupport
    implements LinkSupport,ChannelStateListener,ChannelFieldGroupListener,
    ChannelNotifyGetListener, ChannelNotifyListener
    {
        private static String supportName = "MonitorLink";
        
        private SupportState supportState = SupportState.readyForInitialize;
        private RecordProcess recordProcess = null;
        private RecordProcessSupport recordProcessSupport = null;
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private String recordName = null;
        private String fieldName = null;
        private PVBoolean processAccess = null;
        private PVBoolean monitorOnlyAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        private PVBoolean forceLocalAccess = null;
        
        private PVData recordData = null;
        
        private boolean process = false;
        private boolean monitorOnly = false;
        
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
            super(supportName,dbLink);
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#initialize()
         */
        public void initialize() {
            if(supportState!=SupportState.readyForInitialize) return;
            recordProcess = dbRecord.getRecordProcess();
            recordProcessSupport = recordProcess.getRecordProcessSupport();
            configStructure = dbLink.getConfigurationStructure();
            Structure structure = (Structure)configStructure.getField();
            String configStructureName = structure.getStructureName();
            if(!configStructureName.equals("monitorLink")) {
                errorMessage(
                    "MonitorLink.initialize: configStructure name is "
                    + configStructureName
                    + " but expecting monitorLink"
                    );
                return;
            }
            pvnameAccess = getString(this,configStructure,"pvname");
            if(pvnameAccess==null) return;
            processAccess = getBoolean(this,configStructure,"process");
            if(processAccess==null) return;
            monitorOnlyAccess = getBoolean(this,configStructure,"monitorOnly");
            if(monitorOnlyAccess==null) return;
            inheritSeverityAccess = getBoolean(this,configStructure,"inheritSeverity");
            if(inheritSeverityAccess==null) return;
            forceLocalAccess = getBoolean(this,configStructure,"forceLocal");
            if(forceLocalAccess==null) return;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#uninitialize()
         */
        public void uninitialize() {
            stop();
            supportState = SupportState.readyForInitialize;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#start()
         */
        public void start() {
            if(supportState!=SupportState.readyForStart) return;
            if(recordData==null) {
                errorMessage(
                    "Logic Error: MonitorLink.start called before setField");
                setSupportState(SupportState.zombie);
                return;
            }
            process = processAccess.get();
            monitorOnly = monitorOnlyAccess.get();
            // split pvname into record name and rest of name
            String[]pvname = periodPattern.split(pvnameAccess.get(),2);
            recordName = pvname[0];
            if(pvname.length==2) {
                fieldName = pvname[1];
            } else {
                fieldName = "value";
            }
            channel = ChannelFactory.createChannel(recordName,this);
            if(channel==null) {
                errorMessage("Failed to create channel for " + recordName);
                setSupportState(SupportState.readyForInitialize);
                return;
            }
            if(channel.isLocal()) {
                ChannelLink channelLocal = (ChannelLink)channel;
                channelLocal.setLinkRecord(dbLink.getRecord());
            }
            
            channelSubscribe = channel.createSubscribe();
            if(channel.isConnected()) {
                channelStateChange(channel);
            }
            supportState = SupportState.ready;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#stop()
         */
        public void stop() {
            if(supportState!=SupportState.ready) return;
            channelRecord = null;
            if(channel!=null) channel.destroy();
            channel = null;
            supportState = SupportState.readyForStart;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#setField(org.epics.ioc.pvAccess.PVData)
         */
        public void setField(PVData data) {
            recordData = data;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.LinkSupport#process(org.epics.ioc.dbProcess.LinkListener)
         */
        public ProcessReturn process(ProcessCompleteListener listener) {
            if(supportState!=SupportState.ready) {
                errorMessage(
                    "process called but supportState is "
                    + supportState.toString());
                return ProcessReturn.failure;
            }
            if(!channel.isConnected()) {
                recordProcessSupport.setStatusSeverity("Link not connected",
                    AlarmSeverity.invalid);
                return ProcessReturn.failure;
            }
            return ProcessReturn.success;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.Support#processContinue()
         */
        public void processContinue() {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c) {
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
                String errorMessage = null;
                if(forceLocalAccess.get() && !channel.isLocal()) {
                    errorMessage = String.format(
                        "%s.%s pvname %s is not local",
                        dbLink.getRecord().getRecordName(),
                        dbLink.getDBDField().getName(),
                        pvnameAccess.get());
                    errorMessage(errorMessage);
                    setSupportState(SupportState.readyForInitialize);
                }
                ChannelSetFieldResult result = channel.setField(fieldName);
                if(result!=ChannelSetFieldResult.thisChannel) {
                    throw new IllegalStateException(
                    "Logic Error: MonitorLink.connect bad return from setField");
                }
                linkField = channel.getChannelField();
                errorMessage = checkCompatibility();
                if(errorMessage!=null) {
                    errorMessage(errorMessage);
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
                    IOCDB iocdb = dbRecord.getRecordProcess().getProcessDB().getIOCDB();
                    channelRecord = iocdb.findRecord(recordName);
                    if(channelRecord==null) {
                        throw new IllegalStateException(
                        "Logic Error: channel is local but cant find record");
                    }
                }
                if(monitorOnly) {
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
                uninitialize();
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
                errorMessage("Logic error in MonitorLink field!=linkField");
                return;
            }
            Type linkType = data.getField().getType();
            Field recordField = recordData.getField();
            Type recordType = recordField.getType();
            if(recordType.isScalar() && linkType.isScalar()) {
                convert.copyScalar(data,recordData);
                return;
            }
            if(linkType==Type.pvArray && recordType==Type.pvArray) {
                PVArray linkArrayData = (PVArray)data;
                PVArray recordArrayData = (PVArray)recordData;
                convert.copyArray(linkArrayData,0,
                    recordArrayData,0,linkArrayData.getLength());
                return;
            }
            if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
                PVStructure linkStructureData = (PVStructure)data;
                PVStructure recordStructureData = (PVStructure)recordData;
                convert.copyStructure(linkStructureData,recordStructureData);
                return;
            }
            errorMessage("Logic error in MonitorLink: unsupported type");
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelNotifyListener#newData(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.ChannelField)
         */
        public void newData(Channel channel, ChannelField field) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelNotifyListener#reason(org.epics.ioc.channelAccess.Channel, org.epics.ioc.channelAccess.Event)
         */
        public void reason(Channel channel, Event reason) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelNotifyListener#failure(org.epics.ioc.channelAccess.Channel, java.lang.String)
         */
        public void failure(Channel channel,String reason) {
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
            Field recordField = recordData.getField();
            Type recordType = recordField.getType();
            if(recordType.isScalar() && linkType.isScalar()) {
                if(convert.isCopyScalarCompatible(linkField.getField(),recordField)) return null;
            } else if(linkType==Type.pvArray && recordType==Type.pvArray) {
                Array linkArray = (Array)linkField;
                Array recordArray = (Array)recordField;
                if(convert.isCopyArrayCompatible(linkArray,recordArray)) return null;
            } else if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
                Structure linkStructure = (Structure)linkField;
                Structure recordStructure = (Structure)recordField;
                if(convert.isCopyStructureCompatible(linkStructure,recordStructure)) return null;
            }
            String errorMessage = String.format(
                "%s.%s is not compatible with pvname %s",
                dbLink.getRecord().getRecordName(),
                dbLink.getDBDField().getName(),
                pvnameAccess.get());
            channel = null;
            return errorMessage;
        }
    }
}
