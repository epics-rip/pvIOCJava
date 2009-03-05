/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
public class MonitorLinkBase extends AbstractIOLink
implements CDMonitorRequester,RecordProcessRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvField The field being supported.
     */
    public MonitorLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
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
    private static Executor executor = ExecutorFactory.create("caLinkMonitor", ThreadPriority.low);
    private PVInt monitorTypeAccess = null;
    private PVDouble deadbandAccess = null;
    private PVInt queueSizeAccess = null;
    private PVBoolean reportOverrunAccess = null;
    private PVBoolean processAccess = null;  
    
    private String channelFieldName = null;
    private MonitorType monitorType = null;
    private double deadband = 0.0;
    private int queueSize = 0;
    private boolean reportOverrun = false;
    private boolean isRecordProcessRequester = false;
    private boolean process = false;
   
    private StringArrayData stringArrayData = null;
          
    private CDMonitor cdMonitor = null;
    private ChannelField valueChannelField = null;
    private ChannelField valueIndexChannelField = null;
    private ChannelField valueChoicesChannelField = null;
    private ChannelField alarmChannelField = null;
    private ChannelFieldGroup channelFieldGroup = null;
    private ChannelField[] propertyChannelFields = null;
    
    private int numberOverrun = 0;
    private ReentrantLock lock = new ReentrantLock();
    private Condition waitForCopied = lock.newCondition();
    private volatile boolean cdCopied = false;
    private CD cd = null;
   
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
            alarmSupport.setAlarm("Support not connected",AlarmSeverity.invalid);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        getCD();
        supportProcessRequester.supportProcessDone(RequestResult.success);
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        super.connectionChange(isConnected);
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
        this.cd = cd;
        if(process) {
            cdCopied = false;
            if(recordProcess.process(this, false, null)) {
                if(isRecordProcessRequester) {
                    lock.lock();
                    try {
                        if(!cdCopied) waitForCopied.await(10, TimeUnit.SECONDS);
                    } catch(InterruptedException e) {
                        System.err.println(
                                e.getMessage()
                                + " thread did not call ready");
                    } finally {
                        lock.unlock();
                    }
                    return;
                } else if(recordProcess.processSelfRequest(this)) {
                    recordProcess.processSelfProcess(this, false);
                    lock.lock();
                    try {
                        if(!cdCopied) waitForCopied.await(10, TimeUnit.SECONDS);
                    } catch(InterruptedException e) {
                        System.err.println(
                                e.getMessage()
                                + " thread did not call ready");
                    } finally {
                        lock.unlock();
                    }
                    return;
                }
            }
        }
        pvRecord.lock();
        try {
            getCD();
            return;
        } finally {
            pvRecord.unlock();
        }
    }
    

    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
     */
    public void recordProcessComplete() {
        lock.lock();
        try {
            cdCopied = true;
            waitForCopied.signal();
        } finally {
            lock.unlock();
        }
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
            propertyChannelFields = new ChannelField[length];
            for(int i=0; i<length; i++) {
                ChannelField channelField = channel.createChannelField(
                    propertyPVFields[i].getField().getFieldName());
                propertyChannelFields[i] = channelField;
                channelFieldGroup.addChannelField(channelField);
                cdMonitor.lookForPut(channelField, true);
            }
        }
        cdMonitor.start(queueSize,executor);
    }
    
    private void getCD() {
        ChannelFieldGroup channelFieldGroup = cd.getChannelFieldGroup();
        List<ChannelField> channelFieldList = channelFieldGroup.getList();
        CDStructure cdStructure = cd.getCDRecord().getCDStructure();
        CDField[] cdFields = cdStructure.getCDFields();
        for(int i=0;i<cdFields.length; i++) {
            CDField cdField = cdFields[i];
            ChannelField channelField = channelFieldList.get(i);
            PVField targetPVField = cdField.getPVField();
            if(channelField==alarmChannelField) {
                if(alarmIsProperty) {
                PVStructure pvStructure = (PVStructure)targetPVField;
                PVInt targetPVInt = (PVInt)pvStructure.getSubField("severity.index");
                AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(targetPVInt.get());
                PVString pvString = (PVString)pvStructure.getSubField("message");
                String alarmMessage = pvString.get();
                alarmSupport.setAlarm(alarmMessage, alarmSeverity);
                }
                continue;
            }
            if(cdField.getMaxNumPuts()==0) continue;
            if(channelField==valueChannelField) {
                copy(targetPVField,super.valuePVField);
            } else if(channelField==valueIndexChannelField){
                PVInt pvInt = (PVInt)targetPVField;
                int newValueIndex = pvInt.get();
                valueIndexPV.put(newValueIndex);
                valueIndexPV.postPut();
            } else if(channelField==valueChoicesChannelField){
                PVStringArray pvStringArray = (PVStringArray)targetPVField;
                if(stringArrayData==null) stringArrayData = new StringArrayData();
                int len = pvStringArray.getLength();
                pvStringArray.get(0, len, stringArrayData);
                String[] newValueChoices = stringArrayData.data;
                valueChoicesPV.put(0, newValueChoices.length, newValueChoices, 0);
                valueChoicesPV.postPut();
            } else if(gotAdditionalPropertys){
                boolean foundIt = false;
                for(int j=0; j<propertyChannelFields.length; j++) {
                    ChannelField propertyChannelField = propertyChannelFields[j];
                    if(channelField==propertyChannelField) {
                        copy(targetPVField,propertyPVFields[j]);
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
        if(numberOverrun>0) {
            alarmSupport.setAlarm(
                    "missed " + Integer.toString(numberOverrun) + " notifications",
                    AlarmSeverity.none);
            numberOverrun = 0;
        }
    }
    
    private boolean checkCompatibility(Field targetField) {
        Type targetType = targetField.getType();
        Field valueField = super.valuePVField.getField();
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
