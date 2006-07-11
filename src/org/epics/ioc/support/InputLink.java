/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.dbProcess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.channelAccess.*;

/**
 * @author mrk
 *
 */
public class InputLink implements LinkSupport, CALinkListener, ChannelDataGet {
    private static String supportName = "InputLink";
    private static Convert convert = ConvertFactory.getConvert();
    
    private DBLink dbLink = null;
    private DBStructure configStructure = null;
    private PVString pvnameAccess = null;
    private PVBoolean processAccess = null;
    private PVBoolean waitAccess = null;
    private PVDouble timeoutAccess = null;
    private PVBoolean inheritSeverityAccess = null;
    private PVBoolean forceLocalAccess = null;
    
    private ChannelOption[] channelOption = null;
    
    private Field severityField = null;
    private PVData recordData = null;
    
    private CALink caLink = null;
    private ChannelIOC channel = null;
    
    private Field linkField = null;
    private ChannelFieldGroup fieldGroup = null;
    
    private RecordProcess recordProcess = null;
    private RecordSupport recordSupport = null;
    
    public InputLink(DBLink dbLink) {
        this.dbLink = dbLink;
    }

    public String getName() {
        return supportName;
    }
    
    // Support
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
        recordData = null;
        forceLocalAccess = null;
        inheritSeverityAccess = null;
        timeoutAccess = null;
        waitAccess = null;
        processAccess = null;
        pvnameAccess = null;
        configStructure = null;
        dbLink = null;
    }

    public void start() {
        boolean process = processAccess.get();
        boolean wait = waitAccess.get();
        caLink = CALinkFactory.create(this,pvnameAccess.get());
        int nOptions = 0;
        if(process) nOptions++;
        if(process && wait) nOptions++;
        channelOption = new ChannelOption[nOptions];
        if(process) channelOption[0] = ChannelOption.process;
        if(wait) channelOption[1] = ChannelOption.wait;
        
    }

    public void stop() {
        channelOption = null;
        channel.destroy();
        channel = null;
        caLink.destroy();
        caLink = null;
        linkField = null;
        fieldGroup.destroy();
        fieldGroup = null;
    }

    // LinkSupport
    public boolean setField(PVData data) {
        recordData = data;
        return true;
    }
    
    public LinkReturn process(RecordProcess recordProcess,RecordSupport recordSupport) {
        if(channel==null) return LinkReturn.failure;
        if(channel.isLocal()) {
            ChannelLocal channelLocal = (ChannelLocal)channel;
            channelLocal.setLinkSupport(this);
            channelLocal.setRecordProcess(recordProcess);
            channelLocal.setRecordSupport(recordSupport);
        }
        this.recordProcess = recordProcess;
        this.recordSupport = recordSupport;
        if(!channel.get(fieldGroup,this,channelOption)) return LinkReturn.failure;
        return LinkReturn.active;
    }
        
    
    // CALinkListener
    public String connect() {
        if(recordData==null) {
            throw new IllegalStateException(
                "Logic Error: InputLink.connect called before setField");
        }
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
        linkField = channel.getField();
        errorMessage = checkCompatibility();
        if(errorMessage!=null) return errorMessage;
        fieldGroup = channel.createFieldGroup();
        fieldGroup.addField(linkField);
        if(inheritSeverityAccess.get()) {
            severityField = channel.getPropertyField("severity");
            if(severityField!=null) fieldGroup.addField(severityField);
        }
        channel.setTimeout(timeoutAccess.get());
        this.channel = channel;
        return null;
    }

    public void disconnect() {
        channel = null;
        fieldGroup.destroy();
        fieldGroup = null;
        linkField = null;
    }

    // ChannelDataGet
    public void gotData(PVData data) {
        if(data.getField()==severityField) {
            PVEnum pvEnum = (PVEnum)data;
            AlarmSeverity severity = AlarmSeverity.getSeverity(pvEnum.getIndex());
            recordProcess.setStatusSeverity("inherit severity",severity);
            return;
        }
        if(data.getField()!=linkField) {
            throw new IllegalArgumentException(
                "Logic error: InputLink.gotData received invalif data");
        }
        Type linkType = linkField.getType();
        Field recordField = recordData.getField();
        Type recordType = recordField.getType();
        if(recordType.isScalar() && linkType.isScalar()) {
            convert.copyScalar(data,recordData);
            recordSupport.linkSupportDone(LinkReturn.done);
            return;
        }
        if(linkType==Type.pvArray && recordType==Type.pvArray) {
            PVArray linkArrayData = (PVArray)data;
            PVArray recordArrayData = (PVArray)recordData;
            convert.copyArray(linkArrayData,0,linkArrayData.getLength(),recordArrayData,0);
            recordSupport.linkSupportDone(LinkReturn.done);
            return;
        }
        if(linkType==Type.pvStructure && recordType==Type.pvStructure) {
            PVStructure linkStructureData = (PVStructure)data;
            PVStructure recordStructureData = (PVStructure)recordData;
            convert.copyStructure(linkStructureData,recordStructureData);
            recordSupport.linkSupportDone(LinkReturn.done);
            return;
        }
        recordSupport.linkSupportDone(LinkReturn.failure);
    }
    
    public void failure(String reason) {
        recordSupport.linkSupportDone(LinkReturn.failure);
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
        Type linkType = linkField.getType();
        Field recordField = recordData.getField();
        Type recordType = recordField.getType();
        if(recordType.isScalar() && linkType.isScalar()) {
            if(convert.isCopyScalarCompatible(linkField,recordField)) return null;
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
