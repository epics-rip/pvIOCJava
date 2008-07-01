/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import java.util.List;

import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDField;
import org.epics.ioc.ca.CDMonitor;
import org.epics.ioc.ca.CDMonitorFactory;
import org.epics.ioc.ca.CDMonitorRequester;
import org.epics.ioc.ca.CDStructure;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.IOCExecutor;
import org.epics.ioc.util.IOCExecutorFactory;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanPriority;

/**
 * Implementation for a channel access monitor link.
 * @author mrk
 *
 */
public class MonitorSupportBase extends AbstractLinkSupport
implements CDMonitorRequester,RecordProcessRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param dbStructure The dbStructure for the field being supported.
     */
    public MonitorSupportBase(String supportName,DBStructure dbStructure) {
        super(supportName,dbStructure);
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
    
    private static IOCExecutor iocExecutor
        = IOCExecutorFactory.create("caLinkMonitor", ScanPriority.low);
    private PVInt monitorTypeAccess = null;
    private PVDouble deadbandAccess = null;
    private PVInt queueSizeAccess = null;
    private PVBoolean reportOverrunAccess = null;
    private PVBoolean processAccess = null;  
    
    private DBField valueDBField = null;
    
    private String channelFieldName = null;
    private MonitorType monitorType = null;
    private double deadband = 0.0;
    private int queueSize = 0;
    private boolean reportOverrun = false;
    private boolean isRecordProcessRequester = false;
    private boolean process = false;
    private boolean valueChanged = false;
    private boolean valueIndexChanged = false;
    private int newValueIndex = 0;
    private boolean valueChoicesChanged = false;
    private String[] newValueChoices = null;
    private StringArrayData stringArrayData = null;
    private boolean[] propertyValueChanged = null;
          
    private CDMonitor cdMonitor = null;
    private ChannelField valueChannelField = null;
    private ChannelField valueIndexChannelField = null;
    private ChannelField valueChoicesChannelField = null;
    private ChannelField alarmChannelField = null;
    private ChannelFieldGroup channelFieldGroup = null;
    private ChannelField[] propertyChannelFields = null;
    
    private AlarmSeverity alarmSeverity = AlarmSeverity.none;
    private String alarmMessage = null;
    private int numberOverrun = 0;
   
    
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#initialize()
     */
    public void initialize() {
        super.initialize();
        if(super.getSupportState()!=SupportState.readyForStart) return;
        PVStructure pvTypeStructure = pvStructure.getStructureField("type", "monitorType");
        PVEnumerated pvEnumerated = pvTypeStructure.getPVEnumerated();
        if(pvEnumerated!=null) monitorTypeAccess = pvEnumerated.getIndexField();
        if(monitorTypeAccess==null) {
            uninitialize(); return;
        }
        deadbandAccess = pvStructure.getDoubleField("deadband");
        if(deadbandAccess==null)  {
            uninitialize(); return;
        }
        queueSizeAccess = pvStructure.getIntField("queueSize");
        if(queueSizeAccess==null)  {
            uninitialize(); return;
        }
        reportOverrunAccess = pvStructure.getBooleanField("reportOverrun");
        if(reportOverrunAccess==null)  {
            uninitialize(); return;
        }
        processAccess = pvStructure.getBooleanField("process");
        if(processAccess==null)  {
            uninitialize(); return;
        }
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
            uninitialize(); return;
        }
        valueDBField = dbStructure.getDBRecord().findDBField(pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        super.start();
        if(super.getSupportState()!=SupportState.ready) return;
        int index = monitorTypeAccess.get();
        monitorType = MonitorType.getType(index);
        deadband = deadbandAccess.get();
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
        }
        super.connect();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#stop()
     */
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        if(isRecordProcessRequester) recordProcess.releaseRecordProcessRequester(this);
        isRecordProcessRequester = false;
        disconnect();
        super.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.SupportListener)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!channel.isConnected()) {
            if(alarmSupport!=null) alarmSupport.setAlarm("Support not connected",
                AlarmSeverity.invalid);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        if(alarmIsProperty) {
            if(alarmSupport!=null) alarmSupport.setAlarm(alarmMessage, alarmSeverity);
        } else if(numberOverrun>0) {
            if(alarmSupport!=null) alarmSupport.setAlarm(
                    "missed " + Integer.toString(numberOverrun) + " notifications",
                    AlarmSeverity.none);
            numberOverrun = 0;
        }
        if(process) post();
        supportProcessRequester.supportProcessDone(RequestResult.success);
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            channelFieldName = channel.getFieldName();
            if(channelFieldName==null) channelFieldName = "value";
            cdMonitor = CDMonitorFactory.create(channel, this);
            channelStart();
        } else {
            disconnect();
        }
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDMonitorRequester#dataOverrun(int)
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
     * @see org.epics.ioc.ca.CDMonitorRequester#monitorCD(org.epics.ioc.ca.CD)
     */
    public void monitorCD(CD cd) {
        dbRecord.lock();
        try {
            ChannelFieldGroup channelFieldGroup = cd.getChannelFieldGroup();
            List<ChannelField> channelFieldList = channelFieldGroup.getList();
            CDStructure cdStructure = cd.getCDRecord().getCDStructure();
            CDField[] cdFields = cdStructure.getCDFields();
            for(int i=0;i<cdFields.length; i++) {
                CDField cdField = cdFields[i];
                ChannelField channelField = channelFieldList.get(i);
                PVField targetPVField = cdField.getPVField();
                if(channelField==alarmChannelField) {
                    PVStructure pvStructure = (PVStructure)targetPVField;
                    PVInt targetPVInt = (PVInt)pvStructure.findProperty("severity.index");
                    alarmSeverity = AlarmSeverity.getSeverity(targetPVInt.get());
                    PVString pvString = (PVString)pvStructure.findProperty("message");
                    alarmMessage = pvString.get();
                    continue;
                }
                if(cdField.getMaxNumPuts()==0) continue;
                if(channelField==valueChannelField) {
                    if(copy(targetPVField,valueDBField.getPVField())) valueChanged = true;
                } else if(channelField==valueIndexChannelField){
                    PVInt pvInt = (PVInt)targetPVField;
                    newValueIndex = pvInt.get();
                    valueIndexChanged = true;
                } else if(channelField==valueChoicesChannelField){
                    PVStringArray pvStringArray = (PVStringArray)targetPVField;
                    if(stringArrayData==null) stringArrayData = new StringArrayData();
                    int len = pvStringArray.getLength();
                    pvStringArray.get(0, len, stringArrayData);
                    valueChoicesChanged = true;
                    newValueChoices = stringArrayData.data;
                } else if(gotAdditionalPropertys){
                    boolean foundIt = false;
                    for(int j=0; j<propertyChannelFields.length; j++) {
                        ChannelField propertyChannelField = propertyChannelFields[j];
                        if(channelField==propertyChannelField) {
                            if(copy(targetPVField,propertyPVFields[j])) {
                                propertyValueChanged[j] = true;
                            }
                            foundIt = true;
                            break;
                        }
                    }
                    if(!foundIt) {
                        pvStructure.message(
                                "Logic error in MonitorSupport",
                                MessageType.error);
                    }
                } else {
                    pvStructure.message(
                            "Logic error in MonitorSupport",
                            MessageType.fatalError);
                }
            }
            if(!process) {
                post();
            }
        } finally {
            dbRecord.unlock();
        }
        if(process) {
            if(isRecordProcessRequester) {
                if(recordProcess.process(this, false, null)) return;
            } else if(recordProcess.processSelfRequest(this)) {
                recordProcess.processSelfProcess(this, false);
                return;
            }
            dbRecord.lock();
            try {
                post();
            } finally {
                dbRecord.unlock();
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
     */
    public void recordProcessComplete() {
        // nothing to do
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
     */
    public void recordProcessResult(RequestResult requestResult) {
        // nothing to do
    }
    
    private void channelStart() {
        channelFieldGroup = channel.createFieldGroup(this);
        if(valueIsEnumerated) {
            String name = channelFieldName + "." + "index";
            valueIndexChannelField = channel.createChannelField(name);
            if(valueIndexChannelField==null) {
                pvStructure.message(
                        name
                        + " is not in channel " + channel.getChannelName(),
                        MessageType.error);
                return;
            }
            channelFieldGroup.addChannelField(valueIndexChannelField);
            cdMonitor.lookForPut(valueIndexChannelField, true);
            name = channelFieldName + "." + "choices";
            valueChoicesChannelField = channel.createChannelField(name);
            if(valueChoicesChannelField==null) {
                pvStructure.message(
                        name
                        + " is not in channel " + channel.getChannelName(),
                        MessageType.error);
                return;
            }
            channelFieldGroup.addChannelField(valueChoicesChannelField);
            cdMonitor.lookForPut(valueChoicesChannelField, true);
        } else {
            valueChannelField = channel.createChannelField(channelFieldName);
            if(valueChannelField==null) {
                pvStructure.message(
                        "channelFieldName " + channelFieldName
                        + " is not in channel " + channel.getChannelName(),
                        MessageType.error);
                return;
            }
            if(!checkCompatibility(valueChannelField.getField()))return;
            channelFieldGroup.addChannelField(valueChannelField);
            switch(monitorType) {
            case put:
                cdMonitor.lookForPut(valueChannelField, true); break;
            case change:
                cdMonitor.lookForChange(valueChannelField, true); break;
            case absoluteChange:
                cdMonitor.lookForAbsoluteChange(valueChannelField, deadband); break;
            case percentageChange:
                cdMonitor.lookForPercentageChange(valueChannelField, deadband); break;
            }
        }
        if(alarmIsProperty) {
            alarmChannelField = channel.createChannelField("alarm");
            if(alarmChannelField!=null) {
                channelFieldGroup.addChannelField(alarmChannelField);
                cdMonitor.lookForPut(alarmChannelField, true);
            } else {
                alarmChannelField = null;
            }
        }
        if(gotAdditionalPropertys) {
            int length = propertyPVFields.length;
            propertyValueChanged = new boolean[length];
            propertyChannelFields = new ChannelField[length];
            for(int i=0; i<length; i++) {
                propertyValueChanged[i] = false;
                ChannelField channelField = channel.createChannelField(
                    propertyPVFields[i].getField().getFieldName());
                propertyChannelFields[i] = channelField;
                channelFieldGroup.addChannelField(channelField);
                cdMonitor.lookForPut(channelField, true);
            }
        }
        cdMonitor.start(queueSize,iocExecutor);
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
        message("is not compatible with pvname " + pvnamePVString.get(),MessageType.error);
        return false;
    }
    
    private boolean copy(PVField fromPVField,PVField toPVField) {
        Type fromType = fromPVField.getField().getType();
        Type toType = toPVField.getField().getType();
        if(fromType.isScalar() && toType.isScalar()) {
            convert.copyScalar(fromPVField, toPVField);
        } else if(fromType==Type.pvArray && toType==Type.pvArray) {
            PVArray fromPVArray = (PVArray)fromPVField;
            PVArray toPVArray = (PVArray)toPVField;
            int arrayLength = fromPVArray.getLength();
            int num = convert.copyArray(fromPVArray,0,toPVArray,0,arrayLength);
            if(num!=arrayLength) message(
                "length " + arrayLength + " but only copied " + num,
                MessageType.warning);;
        } else if(fromType==Type.pvStructure && toType==Type.pvStructure) {
            PVStructure fromPVStructure = (PVStructure)fromPVField;
            PVStructure toPVStructure = (PVStructure)toPVField;
            convert.copyStructure(fromPVStructure,toPVStructure);
        } else {
            message(
                "Logic error: unsupported type",
                MessageType.error);
            return false;
        }
        return true;
    }
    
    private void post() {
        if(valueChanged) {
            valueDBField.postPut();
            valueChanged = false;
        }
        if(valueIndexChanged) {
            valueIndexPVInt.put(newValueIndex);
            valueIndexDBField.postPut();
            valueIndexChanged = false;
            newValueIndex = 0;
        }
        if(valueChoicesChanged) {
            valueChoicesPVStringArray.put(0, newValueChoices.length, newValueChoices, 0);
            valueChoicesDBField.postPut();
            valueChoicesChanged = false;
            newValueChoices = null;
        }
        if(gotAdditionalPropertys){
            for(int j=0; j<propertyChannelFields.length; j++) {
                if(propertyValueChanged[j]) {
                    propertyDBFields[j].postPut();
                    propertyValueChanged[j] = false;
                }
            }
        }
    }
    
    private void disconnect() {
        if(cdMonitor!=null) cdMonitor.stop();
        cdMonitor = null;
        alarmChannelField = null;
        valueChannelField = null;
        valueIndexChannelField = null;
        valueChoicesChannelField = null;
        channelFieldGroup = null;
    }
}
