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
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Scalar;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Structure;
import org.epics.pvData.pv.Type;

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
     * @param pvStructure The pvStructure for the field being supported.
     */
    public MonitorSupportBase(String supportName,PVStructure pvStructure) {
        super(supportName,pvStructure);
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
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    private static Executor executor = ExecutorFactory.create("caLinkMonitor", ThreadPriority.low);
    private PVInt monitorTypeAccess = null;
    private PVDouble deapvandAccess = null;
    private PVInt queueSizeAccess = null;
    private PVBoolean reportOverrunAccess = null;
    private PVBoolean processAccess = null;  
    
    private PVField valuePVField = null;
    
    private String channelFieldName = null;
    private MonitorType monitorType = null;
    private double deapvand = 0.0;
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
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        super.initialize(recordSupport);
        if(super.getSupportState()!=SupportState.readyForStart) return;
        PVStructure pvTypeStructure = pvStructure.getStructureField("type");
        Enumerated enumerated = EnumeratedFactory.getEnumerated(pvTypeStructure);
        if(enumerated!=null) monitorTypeAccess = enumerated.getIndex();
        if(monitorTypeAccess==null) {
            uninitialize(); return;
        }
        deapvandAccess = pvStructure.getDoubleField("deapvand");
        if(deapvandAccess==null)  {
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
        PVStructure pvParent = pvStructure.getParent();
        PVField valuePVField = null;
        while(pvParent!=null) {
            valuePVField = pvProperty.findProperty(pvParent,"value");
            if(valuePVField!=null) break;
            pvParent = pvParent.getParent();
        }
        if(valuePVField==null) {
            pvStructure.message("value field not found", MessageType.error);
            uninitialize(); return;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        super.start();
        if(super.getSupportState()!=SupportState.ready) return;
        int index = monitorTypeAccess.get();
        monitorType = MonitorType.getType(index);
        deapvand = deapvandAccess.get();
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
        pvRecord.lock();
        try {
            pvStructure.message(
                "missed " + Integer.toString(number) + " notifications",
                MessageType.warning);
        } finally {
            pvRecord.unlock();
        }
    } 
    
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDMonitorRequester#monitorCD(org.epics.ioc.ca.CD)
     */
    public void monitorCD(CD cd) {
        pvRecord.lock();
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
                    PVInt targetPVInt = (PVInt)pvProperty.findProperty(pvStructure,"severity.index");
                    alarmSeverity = AlarmSeverity.getSeverity(targetPVInt.get());
                    PVString pvString = (PVString)pvProperty.findProperty(pvStructure,"message");
                    alarmMessage = pvString.get();
                    continue;
                }
                if(cdField.getMaxNumPuts()==0) continue;
                if(channelField==valueChannelField) {
                    if(copy(targetPVField,valuePVField)) valueChanged = true;
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
            pvRecord.unlock();
        }
        if(process) {
            if(isRecordProcessRequester) {
                if(recordProcess.process(this, false, null)) return;
            } else if(recordProcess.processSelfRequest(this)) {
                recordProcess.processSelfProcess(this, false);
                return;
            }
            pvRecord.lock();
            try {
                post();
            } finally {
                pvRecord.unlock();
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
                cdMonitor.lookForAbsoluteChange(valueChannelField, deapvand); break;
            case percentageChange:
                cdMonitor.lookForPercentageChange(valueChannelField, deapvand); break;
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
        cdMonitor.start(queueSize,executor);
    }
    
    private boolean checkCompatibility(Field targetField) {
        Type targetType = targetField.getType();
        Field valueField = valuePVField.getField();
        Type valueType = valueField.getType();
        if(valueType==Type.scalar && targetType==Type.scalar) {
            if(convert.isCopyScalarCompatible((Scalar)targetField,(Scalar)valueField)) return true;
        } else if(targetType==Type.scalarArray && valueType==Type.scalarArray) {
            Array targetArray = (Array)targetField;
            Array valueArray = (Array)valueField;
            if(convert.isCopyArrayCompatible(targetArray,valueArray)) return true;
        } else if(targetType==Type.structure && valueType==Type.structure) {
            Structure targetStructure = (Structure)targetField;
            Structure valueStructure = (Structure)valueField;
            if(convert.isCopyStructureCompatible(targetStructure,valueStructure)) return true;
        }
        message("is not compatible with pvname " + pvnamePV.get(),MessageType.error);
        return false;
    }
    
    private boolean copy(PVField fromPVField,PVField toPVField) {
        Type fromType = fromPVField.getField().getType();
        Type toType = toPVField.getField().getType();
        if(fromType==Type.scalar && toType==Type.scalar) {
            convert.copyScalar((PVScalar)fromPVField,(PVScalar)toPVField);
        } else if(fromType==Type.scalarArray && toType==Type.scalarArray) {
            PVArray fromPVArray = (PVArray)fromPVField;
            PVArray toPVArray = (PVArray)toPVField;
            int arrayLength = fromPVArray.getLength();
            int num = convert.copyArray(fromPVArray,0,toPVArray,0,arrayLength);
            if(num!=arrayLength) message(
                "length " + arrayLength + " but only copied " + num,
                MessageType.warning);;
        } else if(fromType==Type.structure && toType==Type.structure) {
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
            valueChanged = false;
        }
        if(valueIndexChanged) {
            valueIndexPV.put(newValueIndex);
            valueIndexChanged = false;
            newValueIndex = 0;
        }
        if(valueChoicesChanged) {
            valueChoicesPV.put(0, newValueChoices.length, newValueChoices, 0);
            valueChoicesChanged = false;
            newValueChoices = null;
        }
        if(gotAdditionalPropertys){
            for(int j=0; j<propertyChannelFields.length; j++) {
                if(propertyValueChanged[j]) {
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
