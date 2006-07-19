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
public class InputLink implements LinkSupport,
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
    
    private RecordProcess recordProcess = null;
    private RecordSupport recordSupport = null;
    
    public InputLink(DBLink dbLink) {
        this.dbLink = dbLink;
        dbRecord = dbLink.getRecord();
    }
    
    // Support
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
        processAccess = getBoolean("process");
        waitAccess = getBoolean("wait");
        timeoutAccess = getDouble("timeout");
        inheritSeverityAccess = getBoolean("inheritSeverity");
        forceLocalAccess = getBoolean("forceLocal");
    }
    public void destroy() {
        stop();
    }
    public void start() {
        if(recordData==null) {
            throw new IllegalStateException(
                "Logic Error: InputLink.start called before setField");
        }
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
//  LinkSupport
    public boolean setField(PVData data) {
        recordData = data;
        return true;
    }
    
    public LinkReturn process(RecordProcess recordProcess,RecordSupport recordSupport) {
        if(channel==null) return LinkReturn.failure;
        this.recordProcess = recordProcess;
        this.recordSupport = recordSupport;
        isSynchronous = true;
        linkReturn = LinkReturn.active;
        dataGet.get(fieldGroup,this,process,wait);
        isSynchronous = false;
        return linkReturn;
    }
//  CALinkListener
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
            recordSupport.linkSupportDone(LinkReturn.done);
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
            recordSupport.linkSupportDone(LinkReturn.done);
            return;
        }
        if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
            PVStructure linkStructureData = (PVStructure)data;
            PVStructure recordStructureData = (PVStructure)recordData;
            convert.copyStructure(linkStructureData,recordStructureData);
            return;
        }
        recordSupport.linkSupportDone(LinkReturn.failure);
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

    // private methods
    public void failure(String reason) {
        linkReturn = LinkReturn.failure;
        if(isAsynchronous) {
            recordSupport.linkSupportDone(LinkReturn.failure);
        }
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
