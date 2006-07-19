/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.channelAccess.*;

/**
 * @author mrk
 *
 */
public class ProcessLink implements LinkSupport,
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
    
    private ChannelIOC channel = null;
    private DBRecord channelRecord = null;
    private ChannelDataProcess dataProcess = null;
    private ChannelField linkField = null;
    private ChannelField severityField = null;
    private ChannelFieldGroup fieldGroup = null;
    private boolean isSynchronous = false;
    private boolean isAsynchronous = false;
    private LinkReturn linkReturn = LinkReturn.noop;
    
    private RecordProcess recordProcess = null;
    private RecordSupport recordSupport = null;
    
    ProcessLink(DBLink dbLink) {
        this.dbLink = dbLink;
        dbRecord = dbLink.getRecord();
    }
    //Support
    public String getName() {
        return supportName;
    }
    public void initialize() {
        configStructure = dbLink.getConfigStructure();
        Structure structure = (Structure)configStructure.getField();
        String configStructureName = structure.getStructureName();
        if(!configStructureName.equals("inputLink")) {
            throw new IllegalStateException(
                "InputLink.initialize: configStructure name is "
                + configStructureName
                + " but expecting inputLink"
                );
        }
        pvnameAccess = getString("pvname");
        waitAccess = getBoolean("wait");
        timeoutAccess = getDouble("timeout");
        inheritSeverityAccess = getBoolean("inheritSeverity");
        forceLocalAccess = getBoolean("forceLocal");
    }
    public void destroy() {
        stop();
    }
    public void start() {
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
    public LinkReturn process(RecordProcess recordProcess, RecordSupport recordSupport) {
        if(channel==null) return LinkReturn.failure;
        this.recordProcess = recordProcess;
        this.recordSupport = recordSupport;
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
        recordSupport.linkSupportDone(LinkReturn.done);
        dbRecord.unlock();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.channelAccess.ChannelDataProcessListener#failure(java.lang.String)
     */
    public void failure(String reason) {
        dbRecord.lock();
        recordProcess.setStatusSeverity("linked process failed",AlarmSeverity.major);
        recordSupport.linkSupportDone(LinkReturn.done);
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
    private PVString getString(String fieldName) {
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
    
    private PVBoolean getBoolean(String fieldName) {
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
    
    private PVDouble getDouble(String fieldName) {
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
}
