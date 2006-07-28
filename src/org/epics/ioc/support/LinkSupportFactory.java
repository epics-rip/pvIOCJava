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

/**
 * @author mrk
 *
 */
public class LinkSupportFactory {
    public static Support create(DBLink dbLink) {
        Support support = null;
        String supportName = dbLink.getLinkSupportName();
        if(supportName.equals("inputLink")) {
            support = new InputLink(dbLink);
        } else if(supportName.equals("processLink")) {
            support = new ProcessLink(dbLink);
        }
        return support;
    }
    
    private static PVString getString(DBStructure configStructure,String fieldName) {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            throw new IllegalStateException(
                "InputLink.initialize: configStructure does not have field"
                + fieldName);
        }
        if(dbData[index].getField().getType()!=Type.pvString) {
            throw new IllegalStateException(
            "InputLink.initialize: configStructure field "
            + fieldName + " is not a string ");
        }
        return (PVString)dbData[index];
    }
    
    private static PVBoolean getBoolean(DBStructure configStructure,String fieldName) {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            throw new IllegalStateException(
                "InputLink.initialize: configStructure does not have field"
                + fieldName);
        }
        if(dbData[index].getField().getType()!=Type.pvBoolean) {
            throw new IllegalStateException(
            "InputLink.initialize: configStructure field "
            + fieldName + " is not a boolean ");
        }
        return (PVBoolean)dbData[index];
    }
    
    private static PVDouble getDouble(DBStructure configStructure,String fieldName) {
        DBData[] dbData = configStructure.getFieldDBDatas();
        int index = configStructure.getFieldDBDataIndex(fieldName);
        if(index<0) {
            throw new IllegalStateException(
                "InputLink.initialize: configStructure does not have field"
                + fieldName);
        }
        if(dbData[index].getField().getType()!=Type.pvDouble) {
            throw new IllegalStateException(
            "InputLink.initialize: configStructure field "
            + fieldName + " is not a double ");
        }
        return (PVDouble)dbData[index];
    }
    private static class InputLink implements LinkSupport,
    CALinkListener, ChannelDataGetListener, ChannelFieldGroupListener,ChannelStateListener {
        private static String supportName = "InputLink";
        private static Convert convert = ConvertFactory.getConvert();
        
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean waitAccess = null;
        private PVDouble timeoutAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        private PVBoolean forceLocalAccess = null;
        
        private RecordProcess recordProcess = null;
        private PVData recordData = null;
        
        private CALink caLink = null;
        private boolean process = false;
        private boolean wait = false;
        
        private ChannelIOC channel = null;
        private DBRecord channelRecord = null;
        private ChannelDataGet dataGet = null;
        private ChannelField linkField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup fieldGroup = null;
        private boolean isSynchronous = false;
        private boolean isAsynchronous = false;
        private LinkReturn linkReturn = LinkReturn.noop;
        
        private LinkListener linkListener = null;
        
        public InputLink(DBLink dbLink) {
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
        }
        
        // Support
        public String getName() {
            return supportName;
        }
        public void initialize() {
            configStructure = dbLink.getConfigurationStructure();
            Structure structure = (Structure)configStructure.getField();
            String configStructureName = structure.getStructureName();
            if(!configStructureName.equals("inputLink")) {
                throw new IllegalStateException(
                    "InputLink.initialize: configStructure name is "
                    + configStructureName
                    + " but expecting inputLink"
                    );
            }
            pvnameAccess = getString(configStructure,"pvname");
            processAccess = getBoolean(configStructure,"process");
            waitAccess = getBoolean(configStructure,"wait");
            timeoutAccess = getDouble(configStructure,"timeout");
            inheritSeverityAccess = getBoolean(configStructure,"inheritSeverity");
            forceLocalAccess = getBoolean(configStructure,"forceLocal");
        }
        public void destroy() {
            stop();
        }
        public void start() {
            if(recordData==null) {
                throw new IllegalStateException(
                    "Logic Error: InputLink.start called before setField");
            }
            recordProcess = dbLink.getRecord().getRecordProcess();
            process = processAccess.get();
            wait = waitAccess.get();
            caLink = CALinkFactory.create(this,pvnameAccess.get());
        }
        public void stop() {
            disconnect();
            wait = false;
            process = false;
            if(caLink!=null) caLink.destroy();
            caLink = null;
        }
//      LinkSupport
        public boolean setField(PVData data) {
            recordData = data;
            return true;
        }
        
        public LinkReturn process(LinkListener listener) {
            if(channel==null) return LinkReturn.failure;
            linkListener = listener;
            isSynchronous = true;
            linkReturn = LinkReturn.active;
            dataGet.get(fieldGroup,this,process,wait);
            isSynchronous = false;
            return linkReturn;
        }
//      CALinkListener
        public String connect() {
            dbRecord.lock();
            try {
                ChannelIOC channel = caLink.getChannel();
                String errorMessage = null;
                if(forceLocalAccess.get() && !channel.isLocal()) {
                    errorMessage = String.format(
                        "%s.%s pvname %s is not local",
                        dbLink.getRecord().getRecordName(),
                        dbLink.getDBDField().getName(),
                        pvnameAccess.get());
                    return errorMessage;
                }
                ChannelSetResult result = channel.setField(pvnameAccess.get());
                if(result!=ChannelSetResult.thisChannel) {
                    throw new IllegalStateException(
                    "Logic Error: InputLink.connect bad return from setField");
                }
                linkField = channel.getChannelField();
                errorMessage = checkCompatibility();
                if(errorMessage!=null) return errorMessage;
                fieldGroup = channel.createFieldGroup(this);
                fieldGroup.addChannelField(linkField);
                if(inheritSeverityAccess.get()) {
                    result = channel.setField("severity");
                    if(result==ChannelSetResult.thisChannel) {
                        severityField = channel.getChannelField();
                    } else {
                        severityField = null;
                    }
                }
                channel.setTimeout(timeoutAccess.get());
                channelRecord = channel.getLocalRecord();
                this.channel = channel;
            } finally {
                dbRecord.unlock();
            }
            return null;
        }
        public void disconnect() {
            dbRecord.lock();
            try {
                severityField = null;
                linkField = null;
                if(fieldGroup!=null) fieldGroup.destroy();
                fieldGroup = null;
                if(channel!=null) channel.destroy();
                channel = null;
            } finally {
                dbRecord.unlock();
            }
        }
        // ChannelDataGetListener
        public void beginSynchronous() {
            dbRecord.lock();
            isAsynchronous = !isSynchronous;
            if(isSynchronous) dbRecord.unlock();
            if(channelRecord!=dbRecord) dbRecord.lockOtherRecord(channelRecord);
        }

        public void endSynchronous() {
            linkReturn = LinkReturn.done;
            if(channelRecord!=dbRecord) channelRecord.unlock();
            if(isAsynchronous) {
                linkListener.processComplete(LinkReturn.done);
                dbRecord.unlock();
            }
        }

        public void newData(ChannelField field,PVData data) {
            if(field==severityField) {
                PVEnum pvEnum = (PVEnum)data;
                AlarmSeverity severity = AlarmSeverity.getSeverity(pvEnum.getIndex());
                if(severity!=AlarmSeverity.none) {
                    recordProcess.setStatusSeverity("inherit severity",severity);
                }
                return;
            }
            if(field!=linkField) {
                throw new IllegalArgumentException(
                    "Logic error: InputLink.gotData received invalif data");
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
                convert.copyArray(linkArrayData,0,recordArrayData,0,linkArrayData.getLength());
                linkListener.processComplete(LinkReturn.done);
                return;
            }
            if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
                PVStructure linkStructureData = (PVStructure)data;
                PVStructure recordStructureData = (PVStructure)recordData;
                convert.copyStructure(linkStructureData,recordStructureData);
                return;
            }
            linkListener.processComplete(LinkReturn.failure);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c) {
            // nothing to do
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
         */
        public void disconnect(Channel c) {
            stop();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.channelAccess.ChannelField)
         */
        public void accessRightsChange(ChannelField channelField) {
            // nothing to do
        }
        public void failure(String reason) {
            linkReturn = LinkReturn.failure;
            if(isAsynchronous) {
                linkListener.processComplete(LinkReturn.failure);
            }
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
    
    private static class ProcessLink implements LinkSupport,
    CALinkListener,ChannelDataProcessListener,ChannelFieldGroupListener,ChannelStateListener{
        private static String supportName = "ProcessLink";
        private DBLink dbLink = null;
        private DBRecord dbRecord = null;
        private DBStructure configStructure = null;
        private PVString pvnameAccess = null;
        private PVBoolean waitAccess = null;
        private PVDouble timeoutAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        private PVBoolean forceLocalAccess = null;
        
        private CALink caLink = null;
        private boolean wait = false;
        private RecordProcess recordProcess = null;
        
        private ChannelIOC channel = null;
        private DBRecord channelRecord = null;
        private ChannelDataProcess dataProcess = null;
        private ChannelField linkField = null;
        private ChannelField severityField = null;
        private ChannelFieldGroup fieldGroup = null;
        private boolean isSynchronous = false;
        private boolean isAsynchronous = false;
        private LinkReturn linkReturn = LinkReturn.noop;
        
        private LinkListener linkListener = null;
        
        ProcessLink(DBLink dbLink) {
            this.dbLink = dbLink;
            dbRecord = dbLink.getRecord();
        }
        //Support
        public String getName() {
            return supportName;
        }
        public void initialize() {
            configStructure = dbLink.getConfigurationStructure();
            Structure structure = (Structure)configStructure.getField();
            String configStructureName = structure.getStructureName();
            if(!configStructureName.equals("inputLink")) {
                throw new IllegalStateException(
                    "InputLink.initialize: configStructure name is "
                    + configStructureName
                    + " but expecting inputLink"
                    );
            }
            pvnameAccess = getString(configStructure,"pvname");
            waitAccess = getBoolean(configStructure,"wait");
            timeoutAccess = getDouble(configStructure,"timeout");
            inheritSeverityAccess = getBoolean(configStructure,"inheritSeverity");
            forceLocalAccess = getBoolean(configStructure,"forceLocal");
        }
        public void destroy() {
            stop();
        }
        public void start() {
            recordProcess = dbLink.getRecord().getRecordProcess();
            wait = waitAccess.get();
            caLink = CALinkFactory.create(this,pvnameAccess.get());
        }
        public void stop() {
            disconnect();
            wait = false;
            if(caLink!=null) caLink.destroy();
            caLink = null;
        }
        // LinkSupport
        public boolean setField(PVData field) {
            // nothing to do
            return false;
        }
        public LinkReturn process(LinkListener listener) {
            if(channel==null) return LinkReturn.failure;
            linkListener = listener;
            isSynchronous = true;
            linkReturn = LinkReturn.active;
            dataProcess.process(fieldGroup,this,wait);
            isSynchronous = false;
            return linkReturn;
        }
        // CALinkListener
        /* (non-Javadoc)
         * @see org.epics.ioc.support.CALinkListener#connect()
         */
        public String connect() {
            dbRecord.lock();
            try {
                ChannelIOC channel = caLink.getChannel();
                String errorMessage = null;
                if(forceLocalAccess.get() && !channel.isLocal()) {
                    errorMessage = String.format(
                        "%s.%s pvname %s is not local",
                        dbLink.getRecord().getRecordName(),
                        dbLink.getDBDField().getName(),
                        pvnameAccess.get());
                    return errorMessage;
                }
                ChannelSetResult result = channel.setField(pvnameAccess.get());
                if(result!=ChannelSetResult.thisChannel) {
                    throw new IllegalStateException(
                    "Logic Error: InputLink.connect bad return from setField");
                }
                linkField = channel.getChannelField();
                fieldGroup = channel.createFieldGroup(this);
                fieldGroup.addChannelField(linkField);
                if(inheritSeverityAccess.get()) {
                    result = channel.setField("severity");
                    if(result==ChannelSetResult.thisChannel) {
                        severityField = channel.getChannelField();
                    } else {
                        severityField = null;
                    }
                }
                channel.setTimeout(timeoutAccess.get());
                channelRecord = channel.getLocalRecord();
                this.channel = channel;
            } finally {
                dbRecord.unlock();
            }
            return null;
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.support.CALinkListener#disconnect()
         */
        public void disconnect() {
            dbRecord.lock();
            try {
                severityField = null;
                linkField = null;
                if(fieldGroup!=null) fieldGroup.destroy();
                fieldGroup = null;
                if(channel!=null) channel.destroy();
                channel = null;
            } finally {
                dbRecord.unlock();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelDataProcessListener#processDone(org.epics.ioc.dbProcess.ProcessReturn)
         */
        public void processDone(ProcessReturn result,AlarmSeverity alarmSeverity,String status) {
            dbRecord.lock();
            if(alarmSeverity!=AlarmSeverity.none) {
                recordProcess.setStatusSeverity("inherit" + status,alarmSeverity);
            }
            linkListener.processComplete(LinkReturn.done);
            dbRecord.unlock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelDataProcessListener#failure(java.lang.String)
         */
        public void failure(String reason) {
            dbRecord.lock();
            recordProcess.setStatusSeverity("linked process failed",AlarmSeverity.major);
            linkListener.processComplete(LinkReturn.done);
            dbRecord.unlock();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#channelStateChange(org.epics.ioc.channelAccess.Channel)
         */
        public void channelStateChange(Channel c) {
            // nothing to do
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.channelAccess.ChannelStateListener#disconnect(org.epics.ioc.channelAccess.Channel)
         */
        public void disconnect(Channel c) {
            stop();
        }
        /**
         * @param channelField
         */
        public void accessRightsChange(ChannelField channelField) {
            // nothing to do
        }
    }
}
