 /**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.ioc.ca.CD;
import org.epics.ioc.ca.CDFactory;
import org.epics.ioc.ca.CDField;
import org.epics.ioc.ca.CDGet;
import org.epics.ioc.ca.CDGetRequester;
import org.epics.ioc.ca.CDMonitor;
import org.epics.ioc.ca.CDMonitorFactory;
import org.epics.ioc.ca.CDMonitorRequester;
import org.epics.ioc.ca.CDPut;
import org.epics.ioc.ca.CDPutRequester;
import org.epics.ioc.ca.CDStructure;
import org.epics.ioc.ca.Channel;
import org.epics.ioc.ca.ChannelFactory;
import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelFieldGroupListener;
import org.epics.ioc.ca.ChannelListener;
import org.epics.ioc.ca.ChannelMonitorNotify;
import org.epics.ioc.ca.ChannelMonitorNotifyFactory;
import org.epics.ioc.ca.ChannelMonitorNotifyRequester;
import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.process.ProcessCallbackRequester;
import org.epics.ioc.process.ProcessContinueRequester;
import org.epics.ioc.process.RecordProcess;
import org.epics.ioc.process.RecordProcessRequester;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Convert;
import org.epics.ioc.pv.ConvertFactory;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVArray;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScanPriority;

/**
 * Factory to create support for Channel Access links.
 * @author mrk
 *
 */
public class CASupportFactory {
    /**
     * Create link support for Channel Access links.
     * @param dbStructure The field for which to create support.
     * @return A Support interface or null if the support is not found.
     */
    public static Support create(DBStructure dbStructure) {
        String supportName = dbStructure.getSupportName();
        if(supportName.equals(processSupportName)) {
            return new ProcessSupport(dbStructure);
        } else if(supportName.equals(inputSupportName)) {
            return new InputSupport(dbStructure);
        } else if(supportName.equals(outputSupportName)) {
            return new OutputSupport(dbStructure);
        } else if(supportName.equals(monitorSupportName)) {
            return new MonitorSupport(dbStructure);
        } else if(supportName.equals(monitorNotifySupportName)) {
            return new MonitorNotifySupport(dbStructure);
        }
        dbStructure.getPVStructure().message("no support for " + supportName, MessageType.fatalError);
        return null;
    }
    private static final String processSupportName = "processSupport";
    private static final String inputSupportName = "inputSupport";
    private static final String outputSupportName = "outputSupport";
    private static final String monitorSupportName = "monitorSupport";
    private static final String monitorNotifySupportName = "monitorNotifySupport";

    private static final Convert convert = ConvertFactory.getConvert();
    
    private static abstract class CASupport extends AbstractSupport
    implements ChannelListener,ChannelFieldGroupListener {
        protected DBStructure dbStructure;
        protected PVStructure pvStructure;
        protected String channelRequesterName;
        protected DBRecord dbRecord;
        protected RecordProcess recordProcess = null;
        protected AlarmSupport alarmSupport = null;
        protected PVString providerNameAccess = null;
        protected PVString pvnameAccess = null;
        
        protected Channel channel = null;
        protected boolean isConnected = false;
        
        protected CASupport(String supportName,DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
            channelRequesterName = pvStructure.getFullName();
            dbRecord = dbStructure.getDBRecord();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#initialize()
         */
        @Override
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,processSupportName)) return;
            recordProcess = dbRecord.getRecordProcess();
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            providerNameAccess = pvStructure.getStringField("providerName");
            if(providerNameAccess==null) return;
            pvnameAccess = pvStructure.getStringField("pvname");
            if(pvnameAccess==null) return;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,processSupportName)) return;
            isConnected = false;
            String providerName = providerNameAccess.get();
            String pvname = pvnameAccess.get();
            channel = ChannelFactory.createChannel(pvname, providerName, this);
            if(channel==null) {
                message("providerName " + providerName + " pvname " + pvname + " not found",MessageType.error);
                return;
            }
            setSupportState(SupportState.ready);
        }
        /**
         * Called after derived class is started
         */
        protected void connect() {
            if(super.getSupportState()!=SupportState.ready) return;
            channel.connect();
            isConnected = channel.isConnected();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#stop()
         */
        public void stop() {
            channel.disconnect();
            channel = null;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()==SupportState.readyForInitialize) return;
            setSupportState(SupportState.readyForInitialize);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message,MessageType messageType) {
            dbRecord.lock();
            try {
                pvStructure.message(message, messageType);
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#disconnect(org.epics.ioc.ca.Channel)
         */
        public void destroy(Channel c) {
            dbRecord.lock();
            try {
                if(super.getSupportState()!=SupportState.ready) return;
            } finally {
                dbRecord.unlock();
            }
            recordProcess.stop();
            recordProcess.start();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.Channel, org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel, ChannelField channelField) {
            // nothing to do         
        }
        
    }
    
    private static class ProcessSupport extends CASupport implements
    ChannelListener,
    ProcessCallbackRequester,ProcessContinueRequester,
    ChannelProcessRequester
    {
        
        private ChannelProcess channelProcess = null;
        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult requestResult;

        private ProcessSupport(DBStructure dbStructure) {
            super(processSupportName,dbStructure);
           
        }               
        /* (non-Javadoc)
         * @see org.epics.ioc.support.CASupportFactory.CASupport#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            if(isConnected) {
                channelProcess = channel.createChannelProcess(this);
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
            } else {
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
                channelProcess.destroy();
                channelProcess = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            super.start();
            super.connect();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            channelProcess.destroy();
            channelProcess = null;
            super.stop();
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!isConnected) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not connected",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
            recordProcess.requestProcessCallback(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {            
            channelProcess.process();
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            supportProcessRequester.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelProcessRequester#processDone(org.epics.ioc.util.RequestResult)
         */
        public void processDone(RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
            
        }
    }
    
    private static class InputSupport extends CASupport implements
    ChannelListener,
    ProcessCallbackRequester,ProcessContinueRequester,
    CDGetRequester
    {   
        private DBField valueDBField;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;
        
        private boolean process = false;
        private boolean inheritSeverity = false;
        
        private SupportProcessRequester supportProcessRequester;
        private RequestResult requestResult;   
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;

        private String channelFieldName;
        private CD cd = null;
        private CDGet cdGet;      
        private ChannelField valueChannelField;
        private ChannelField severityField;
        private ChannelFieldGroup channelFieldGroup;
        
        private InputSupport(DBStructure dbStructure) {
            super(inputSupportName,dbStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.CASupportFactory.CASupport#channelStateChange(org.epics.ioc.ca.Channel, boolean)
         */
        public void channelStateChange(Channel c, boolean isConnected) {
            if(isConnected) {
                channelFieldName = channel.getFieldName();
                if(channelFieldName==null) channelFieldName = "value";
                if(!prepareForInput()) return;
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
            } else {
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
                if(cdGet!=null) cd.destroy(cdGet);
                cdGet = null;
                valueChannelField = null;
                severityField = null;
                channelFieldGroup = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {           
            super.initialize();
            if(super.getSupportState()!=SupportState.readyForStart) return;
            processAccess = pvStructure.getBooleanField("process");
            if(processAccess==null) {
                uninitialize();
                return;
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
                uninitialize();
                return;
            }
            valueDBField = dbStructure.getDBRecord().findDBField(pvField);
            inheritSeverityAccess = pvStructure.getBooleanField("inheritSeverity");
            if(inheritSeverityAccess==null) {
                pvStructure.message("inheritSeverityAccess field not found", MessageType.error);
                uninitialize();
            }
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            super.start();
            if(super.getSupportState()!=SupportState.ready) return;
            inheritSeverity = inheritSeverityAccess.get();
            process = processAccess.get();
            super.connect();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(cdGet!=null) cd.destroy(cdGet);
            cdGet = null;
            super.stop();
            
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!isConnected) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not connected",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
            recordProcess.requestProcessCallback(this);
            return;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            requestResult = RequestResult.success;
            alarmSeverity = AlarmSeverity.none;
            cdGet.get(cd);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
            if(inheritSeverity && alarmSeverity!=AlarmSeverity.none) {
                if(alarmSupport!=null) alarmSupport.setAlarm("linkAlarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
             }
             supportProcessRequester.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDGetRequester#getDone(org.epics.ioc.util.RequestResult)
         */
        public void getDone(RequestResult requestResult) {
            CDField[] cdFields = cd.getCDRecord().getCDStructure().getCDFields();
            if(cdFields.length>1) {
                CDField severityIndex = cdFields[1];
                PVInt pvInt = (PVInt)severityIndex.getPVField();
                int index = pvInt.get();
                alarmSeverity = AlarmSeverity.getSeverity(index);
            }
            PVField data = cdFields[0].getPVField();
            Type targetType = data.getField().getType();
            PVField pvField = valueDBField.getPVField();
            Field valueField = pvField.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && targetType.isScalar()) {
                convert.copyScalar(data,pvField);
                valueDBField.postPut();
            } else if(targetType==Type.pvArray && valueType==Type.pvArray) {
                PVArray targetPVArray = (PVArray)data;
                PVArray valuePVArray = (PVArray)valueDBField.getPVField();
                int arrayLength = targetPVArray.getLength();
                int num = convert.copyArray(targetPVArray,0,valuePVArray,0,arrayLength);
                valueDBField.postPut();
                if(num!=arrayLength) message(
                    "length " + arrayLength + " but only copied " + num,
                    MessageType.warning);;
            } else if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                PVStructure targetPVStructure = (PVStructure)data;
                PVStructure valuePVStructure = (PVStructure)valueDBField.getPVField();
                convert.copyStructure(targetPVStructure,valuePVStructure);
                valueDBField.postPut();
            } else {
                message(
                    "Logic error in OutputSupport: unsupported type",
                    MessageType.fatalError);
            }
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }      
        
        private boolean prepareForInput() {
            valueChannelField = channel.createChannelField(channelFieldName);
            if(valueChannelField==null) {
                message(channelFieldName + " does not exist ",MessageType.error);
                return false;
            }
            if(!checkCompatibility(valueChannelField.getField())) {
                valueChannelField = null;
                return false;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(valueChannelField);
            if(inheritSeverity) {
                severityField = valueChannelField.findProperty("alarm.severity.index");
                if(severityField==null) {
                    channelFieldGroup = null;
                    valueChannelField = null;
                    message("severity.index does not exist ",MessageType.error);
                    return false;
                }
                Type type = severityField.getField().getType();
                if(type!=Type.pvInt) {
                    channelFieldGroup = null;
                    valueChannelField = null;
                    message("severity.index is not an int ",MessageType.error);
                    return false;
                }
                channelFieldGroup.addChannelField(severityField);
            }
            cd = CDFactory.createCD(channel, channelFieldGroup);
            cdGet = cd.createCDGet(this, process);
            return true;
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
            message("is not compatible with pvname " + pvnameAccess.get(),MessageType.error);
            return false;
        }
    }
        
    private static class OutputSupport extends CASupport implements
    ChannelListener,
    ProcessCallbackRequester,ProcessContinueRequester,
    CDPutRequester
    {
        private PVBoolean processAccess = null;
        private DBField valueDBField = null;
        
        private boolean process = false;
        
        private SupportProcessRequester supportProcessRequester = null;
        private RequestResult requestResult = null;   
                     
        private String channelFieldName = null;
        private CD cd = null;
        private CDPut cdPut = null;
        private ChannelField valueChannelField = null;
        private ChannelFieldGroup putFieldGroup = null;
        
        private OutputSupport(DBStructure dbStructure) {
            super(inputSupportName,dbStructure);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            if(isConnected) {
                channelFieldName = channel.getFieldName();
                if(channelFieldName==null) channelFieldName = "value";
                if(!prepareForOutput()) return;;
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
            } else {
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
                if(cdPut!=null) cd.destroy(cdPut);
                cdPut = null;
                valueChannelField = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            super.initialize();
            if(super.getSupportState()!=SupportState.readyForStart) return;
            processAccess = pvStructure.getBooleanField("process");
            if(processAccess==null) {
                uninitialize();
                return;
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
                uninitialize();
                return;
            }
            valueDBField = dbStructure.getDBRecord().findDBField(pvField);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            super.start();
            if(super.getSupportState()!=SupportState.ready) return;
            process = processAccess.get();
            super.connect();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(cdPut!=null) cd.destroy(cdPut);
            cdPut = null;
            super.stop();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!isConnected) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName() + " not connected",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            CDField[] cdFields = cd.getCDRecord().getCDStructure().getCDFields();
            CDField cdField = cdFields[0];
            PVField data = cdField.getPVField();
            Type targetType = data.getField().getType();
            PVField valuePVField = valueDBField.getPVField();
            Field valueField = valuePVField.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && targetType.isScalar()) {
                convert.copyScalar(valuePVField,data);
                cdField.incrementNumPuts();
            } else if(targetType==Type.pvArray && valueType==Type.pvArray) {
                PVArray targetPVArray = (PVArray)data;
                PVArray valuePVArray = (PVArray)valuePVField;
                    int arrayLength = targetPVArray.getLength();
                int num = convert.copyArray(valuePVArray,0,targetPVArray,0,arrayLength);
                if(num!=arrayLength) message(
                        "length " + arrayLength + " but only copied " + num,
                        MessageType.warning);
                cdField.incrementNumPuts();
            } else if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                PVStructure targetPVStructure = (PVStructure)data;
                PVStructure valuePVStructure = (PVStructure)valuePVField;
                convert.copyStructure(valuePVStructure,targetPVStructure);
                cdField.incrementNumPuts();
            } else {
                message(
                    "Logic error in OutputSupport: unsupported type",
                    MessageType.fatalError);
                if(alarmSupport!=null) alarmSupport.setAlarm(
                        pvStructure.getFullFieldName()
                        + "Logic error in OutputSupport: unsupported type",
                        AlarmSeverity.major);
                supportProcessRequester.supportProcessDone(RequestResult.success);
                return;
            }
            this.supportProcessRequester = supportProcessRequester;
            recordProcess.requestProcessCallback(this);
            return;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
         */
        public void processCallback() {
            cdPut.put(cd);            
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
         */
        public void processContinue() {
             supportProcessRequester.supportProcessDone(requestResult);
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDPutRequester#getDone(org.epics.ioc.util.RequestResult)
         */
        public void getDone(RequestResult requestResult) {
            // nothing to do
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.CDPutRequester#putDone(org.epics.ioc.util.RequestResult)
         */
        public void putDone(RequestResult requestResult) {
            this.requestResult = requestResult;
            recordProcess.processContinue(this);
        }      
                
        private boolean prepareForOutput() {
            valueChannelField = channel.createChannelField(channelFieldName);
            if(valueChannelField==null) {
                message(channelFieldName + " does not exist ",MessageType.error);
                return false;
            }
            if(!checkCompatibility(valueChannelField.getField())) {
                valueChannelField = null;
                return false;
            }
            putFieldGroup = channel.createFieldGroup(this);
            
            putFieldGroup.addChannelField(valueChannelField); 
            cd = CDFactory.createCD(channel, putFieldGroup);
            cdPut = cd.createCDPut(this, process);
            return true;
        }
              
        private boolean checkCompatibility(Field targetField) {
            Type targetType = targetField.getType();
            PVField valuePVField = valueDBField.getPVField();
            Field valueField = valuePVField.getField();
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
            message("is not compatible with pvname " + pvnameAccess.get(),MessageType.error);
            return false;
        }
    }
    
    private static class MonitorNotifySupport extends CASupport implements
    RecordProcessRequester,
    ChannelListener,
    ChannelMonitorNotifyRequester
    {
        private ChannelMonitorNotify channelMonitorNotify = null;
        private boolean isActive;
        private MonitorNotifySupport(DBStructure dbStructure) {
            super(monitorNotifySupportName,dbStructure);
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.support.CASupportFactory.CASupport#initialize()
         */
        public void initialize() {
            super.initialize();
            if(!super.checkSupportState(SupportState.readyForStart,inputSupportName)) return;
            if(!recordProcess.setRecordProcessRequester(this)) {
                message("notifySupport but record already has recordProcessor",MessageType.error);
                uninitialize();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            super.start();
            if(!super.checkSupportState(SupportState.ready,inputSupportName)) return;
            super.connect();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            isActive = false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            if(isConnected) {
                String fieldName = channel.getFieldName();
                if(fieldName==null) fieldName = "value";
                ChannelField channelField = channel.createChannelField(fieldName);
                ChannelFieldGroup channelFieldGroup = channel.createFieldGroup(this);
                channelFieldGroup.addChannelField(channelField);
                channelMonitorNotify = ChannelMonitorNotifyFactory.create(channel, this);
                channelMonitorNotify.setFieldGroup(channelFieldGroup);
                channelMonitorNotify.start();
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
            } else {
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
                channelMonitorNotify.destroy();
                channelMonitorNotify = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelMonitorNotifyRequester#monitorEvent()
         */
        public void monitorEvent() {
            if(isActive) {
                dbRecord.lock();
                try {
                    alarmSupport.setAlarm(
                        "channelMonitorNotify event but record already active", AlarmSeverity.minor);
                } finally {
                    dbRecord.unlock();
                }
                return;
            }
            recordProcess.process(this, false, null);
        } 
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
    
    private static class MonitorSupport extends CASupport implements
    ChannelListener,
    CDMonitorRequester,
    RecordProcessRequester
    {   
        private PVString pvnameAccess = null;
        private PVInt monitorTypeAccess = null;
        private PVDouble deadbandAccess = null;
        private PVInt queueSizeAccess = null;
        private PVBoolean reportOverrunAccess = null;
        private PVBoolean processAccess = null;
        private PVBoolean inheritSeverityAccess = null;   
        
        private DBField valueDBField = null;
        
        private String recordName = null;
        private String fieldName = null;
        private MonitorType monitorType = null;
        private double deadband = 0.0;
        private int queueSize = 0;
        private boolean reportOverrun = false;
        private boolean isRecordProcessRequester = false;
        private boolean process = false;
        private boolean inheritSeverity = false;
        private boolean valueChanged = false;
              
        private CDMonitor cDMonitor = null;
        private ChannelField targetChannelField = null;
        private ChannelField severityChannelField = null;
        private ChannelFieldGroup channelFieldGroup = null;
        
        private AlarmSeverity alarmSeverity = AlarmSeverity.none;
        private int numberOverrun = 0;
        private ReentrantLock processLock = null;
        private Condition processCondition = null;
        private boolean processDone;
        
        private MonitorSupport(DBStructure dbStructure) {
            super(monitorSupportName,dbStructure);
        }       
        
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
            inheritSeverityAccess = pvStructure.getBooleanField("inheritSeverity");
            if(inheritSeverityAccess==null)  {
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
                processLock = new ReentrantLock();
                processCondition = processLock.newCondition();
            }
            inheritSeverity = inheritSeverityAccess.get();
            if(!process && inheritSeverity) {
                pvStructure.message("inheritSeverity ignored", MessageType.warning);
                inheritSeverity = false;
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
            if(channel!=null) channel.disconnect();
            channel = null;
            setSupportState(SupportState.readyForStart);
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
            if(valueChanged) valueDBField.postPut();
            if(alarmSeverity!=AlarmSeverity.none) {
                if(alarmSupport!=null) alarmSupport.setAlarm("link Alarm", alarmSeverity);
                alarmSeverity = AlarmSeverity.none;
            } else if(numberOverrun>0) {
                if(alarmSupport!=null) alarmSupport.setAlarm(
                    "missed " + Integer.toString(numberOverrun) + " notifications",
                    AlarmSeverity.none);
                numberOverrun = 0;
            }
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelListener#channelStateChange(org.epics.ioc.ca.Channel)
         */
        public void channelStateChange(Channel c,boolean isConnected) {
            assert(c==channel);
            if(isConnected) {
                fieldName = channel.getFieldName();
                if(fieldName==null) fieldName = "value";
                cDMonitor = CDMonitorFactory.create(channel, this);
                channelStart();
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
            } else {
                dbRecord.lock();
                try {
                    super.isConnected = isConnected;
                } finally {
                    dbRecord.unlock();
                }
                cDMonitor.stop();
                cDMonitor = null;
                severityChannelField = null;
                targetChannelField = null;
                channelFieldGroup = null;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.ca.ChannelFieldGroupListener#accessRightsChange(org.epics.ioc.ca.ChannelField)
         */
        public void accessRightsChange(Channel channel,ChannelField channelField) {
            // nothing to do
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
        public void monitorCD(CD cD) {
            dbRecord.lock();
            try {
                boolean valueChanged = false;
                ChannelFieldGroup channelFieldGroup = cD.getChannelFieldGroup();
                List<ChannelField> channelFieldList = channelFieldGroup.getList();
                CDStructure cdStructure = cD.getCDRecord().getCDStructure();
                CDField[] cdbFields = cdStructure.getCDFields();
                for(int i=0;i<cdbFields.length; i++) {
                    CDField cdField = cdbFields[i];
                    PVField targetPVField = cdField.getPVField();
                    ChannelField channelField = channelFieldList.get(i);
                    if(channelField==severityChannelField) {
                        PVInt targetPVInt = (PVInt)targetPVField;
                        alarmSeverity = AlarmSeverity.getSeverity(targetPVInt.get());
                        continue;
                    }
                    if(channelField!=targetChannelField) {
                        pvStructure.message(
                                "Logic error",
                                MessageType.fatalError);
                        continue;
                    }
                    Type targetType = channelField.getField().getType();
                    PVField valuePVField = valueDBField.getPVField();
                    Field valueField = valuePVField.getField();
                    Type valueType = valueField.getType();
                    if(valueType.isScalar() && targetType.isScalar()) {
                        convert.copyScalar(targetPVField,valuePVField);
                        valueChanged = true;
                        continue;
                    }
                    if(targetType==Type.pvArray && valueType==Type.pvArray) {
                        PVArray targetPVArray = (PVArray)targetPVField;
                        PVArray valuePVArray = (PVArray)valuePVField;
                        convert.copyArray(targetPVArray,0,
                            valuePVArray,0,targetPVArray.getLength());
                        valueChanged = true;
                        continue;
                    }
                    if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                        PVStructure targetPVStructure = (PVStructure)targetPVField;
                        PVStructure valuePVStructure = (PVStructure)valuePVField;
                        convert.copyStructure(targetPVStructure,valuePVStructure);
                        valueChanged = true;
                        continue;
                    }
                    pvStructure.message(
                            "Logic error in MonitorSupport: unsupported type",
                            MessageType.fatalError);
                }
                if(process) {
                    if(valueChanged) this.valueChanged = valueChanged;
                } else if(valueChanged){
                    valueDBField.postPut();
                }
            } finally {
                dbRecord.unlock();
            }
            if(process) {
                if(isRecordProcessRequester) {
                    processDone = false;
                    recordProcess.process(this, false, null);
                }
                // wait for completion
                try {
                    processLock.lock();
                    try {
                        while(!processDone) processCondition.await();
                    }finally {
                        processLock.unlock();
                    }
                } catch(InterruptedException e) {}
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete(org.epics.ioc.process.RequestResult)
         */
        public void recordProcessComplete() {
            processLock.lock();
            try {
                processDone = true;
                processCondition.signal();
            } finally {
                processLock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do
        }
        
        private String checkCompatibility() {
            Type targetType = targetChannelField.getField().getType();
            PVField valuePVField = valueDBField.getPVField();
            Field valueField = valuePVField.getField();
            Type valueType = valueField.getType();
            if(valueType.isScalar() && targetType.isScalar()) {
                if(convert.isCopyScalarCompatible(targetChannelField.getField(),valueField)) return null;
            } else if(targetType==Type.pvArray && valueType==Type.pvArray) {
                Array targetArray = (Array)targetChannelField.getField();
                Array valueArray = (Array)valueField;
                if(convert.isCopyArrayCompatible(targetArray,valueArray)) return null;
            } else if(targetType==Type.pvStructure && valueType==Type.pvStructure) {
                Structure targetStructure = (Structure)targetChannelField;
                Structure valueStructure = (Structure)valueField;
                if(convert.isCopyStructureCompatible(targetStructure,valueStructure)) return null;
            }
            String errorMessage = 
                "is not compatible with pvname " + pvnameAccess.get();
            channel = null;
            return errorMessage;
        }
        
        private void channelStart() {
            targetChannelField = channel.createChannelField(fieldName);
            if(targetChannelField==null) {
                pvStructure.message(
                    "fieldName " + fieldName
                    + " is not in record " + recordName,
                    MessageType.error);
                return;
            }
            String errorMessage = checkCompatibility();
            if(errorMessage!=null) {
                pvStructure.message(errorMessage,MessageType.error);
                return;
            }
            channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(targetChannelField);           
            switch(monitorType) {
            case put:
                cDMonitor.lookForPut(targetChannelField, true); break;
            case change:
                cDMonitor.lookForChange(targetChannelField, true); break;
            case absoluteChange:
                cDMonitor.lookForAbsoluteChange(targetChannelField, deadband); break;
            case percentageChange:
                cDMonitor.lookForPercentageChange(targetChannelField, deadband); break;
            }
            if(inheritSeverityAccess.get()) {
                severityChannelField = channel.createChannelField("alarm.severity.index");
                if(severityChannelField!=null) {
                    channelFieldGroup.addChannelField(severityChannelField);
                    cDMonitor.lookForPut(severityChannelField, true);
                } else {
                    severityChannelField = null;
                }
            }
            String threadName = pvStructure.getFullName();
            cDMonitor.start(queueSize, threadName, ScanPriority.getJavaPriority(ScanPriority.low));
        }
    }
}
