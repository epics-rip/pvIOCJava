/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.QueuePriority;
import org.epics.ioc.pdrv.QueueRequestCallback;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.pdrv.interfaces.DriverUser;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVProperty;
import org.epics.ioc.pv.PVPropertyFactory;
import org.epics.ioc.pv.PVRecord;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmFactory;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Abstract link support base class for PDRV links.
 * It handles all the generic things for a PDRV link.
 * It connects locates the interfaces for all the PDRV fields in the link structure.
 * For each of the lifetime methods it provides methods (initBase, startBase, stopBase, uninitBase)
 * than must be called by derived classes.
 * The process and message implementation should be sufficient for most derived support.
 * @author mrk
 *
 */
public abstract class AbstractPDRVSupport extends AbstractSupport implements
ProcessContinueRequester,QueueRequestCallback,
PDRVSupport,
RecordProcessRequester
{
    /**
     * Constructor for derived support.
     * @param supportName The support name.
     * @param dbStructure The link interface.
     */
    protected AbstractPDRVSupport(String supportName,DBStructure dbStructure) {
        super(supportName,dbStructure);
        this.supportName = supportName;
        this.dbStructure = dbStructure;
        pvStructure = dbStructure.getPVStructure();
        fullName = pvStructure.getFullName();
        dbRecord = dbStructure.getDBRecord();
        pvRecord = dbRecord.getPVRecord();
        recordName = pvRecord.getRecordName();
        recordProcess = dbRecord.getRecordProcess();
    }      
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    protected String supportName;
    protected DBStructure dbStructure;
    protected PVStructure pvStructure;
    protected DBField valueDBField = null;
    protected PVField valuePVField = null;
    protected DBRecord dbRecord = null;
    protected PVRecord pvRecord = null;
    protected String recordName = null;
    protected String fullName = null;
    protected RecordProcess recordProcess = null;
    
    protected AlarmSupport alarmSupport = null;
    protected PVString pvPortName = null;
    protected PVString pvDeviceName = null;
    protected PVInt pvMask = null;
    protected PVInt pvSize = null;
    
    protected PVDouble pvTimeout = null;
    protected PVBoolean pvProcess = null;
    protected PVStructure pvDrvParams = null;
    
    protected User user = null;
    protected Port port = null;
    protected Device device = null;
    protected Trace portTrace = null;
    protected Trace deviceTrace = null;
    
    protected DriverUser driverUser = null;
    
    protected SupportProcessRequester supportProcessRequester = null;
    
    private boolean isProcessor = false;
    
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
        valuePVField = pvProperty.findPropertyViaParent(pvStructure,"value");
        if(valuePVField==null) {
            pvStructure.message("value field not found", MessageType.error);
            return;
        }
        valueDBField = dbStructure.getDBRecord().findDBField(valuePVField);
        alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
        pvPortName = pvStructure.getStringField("portName");
        if(pvPortName==null) return;
        pvDeviceName = pvStructure.getStringField("deviceName");
        if(pvDeviceName==null) return;
        pvMask = pvStructure.getIntField("mask");
        if(pvMask==null) return;
        pvSize = pvStructure.getIntField("size");
        if(pvSize==null) return;
        pvTimeout = pvStructure.getDoubleField("timeout");
        if(pvTimeout==null) return;
        PVField[] pvFields = pvStructure.getPVFields();
        Structure structure = pvStructure.getStructure();
        int index = structure.getFieldIndex("drvParams");
        PVField pvField = null;
        if(index>=0) pvField = pvFields[index];
        if(pvField!=null && pvField.getField().getType()==Type.pvStructure) {
            pvDrvParams = (PVStructure)pvField;
        }
        pvProcess = pvStructure.getBooleanField("process");
        setSupportState(SupportState.readyForStart);
    }
    
    public void uninitialize() {
        if(super.getSupportState()==SupportState.ready) {
            stop();
        }
        if(super.getSupportState()!=SupportState.readyForStart) return;
        pvDrvParams = null;
        pvTimeout = null;
        pvDeviceName = null;
        pvPortName = null;
        pvStructure = null;
        alarmSupport = null;
        setSupportState(SupportState.readyForInitialize);
    }
    
    public void start() {
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        user = Factory.createUser(this);
        user.setTimeout(pvTimeout.get());
        user.setDeviceDriverPvt(pvDrvParams);
        port = user.connectPort(pvPortName.get());
        if(port==null) {
            pvStructure.message(user.getMessage(),MessageType.error);
            return;
        }
        portTrace = port.getTrace();
        device = user.connectDevice(pvDeviceName.get());
        if(device==null) {
            pvStructure.message(user.getMessage(),MessageType.error);
            return;
        }
        deviceTrace = device.getTrace();
        Interface iface = device.findInterface(user, "driverUser", true);
        if(iface!=null) {
            driverUser = (DriverUser)iface;
            driverUser.create(user,pvDrvParams);
        }
        if(pvProcess!=null) {
            boolean interrupt = pvProcess.get();
            if(interrupt) {
                isProcessor =recordProcess.setRecordProcessRequester(this);
            }
        }
        setSupportState(SupportState.ready);
    }
    
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        user.disconnectPort();
        if(driverUser!=null) {
            driverUser.dispose(user);
        }
        driverUser = null;
        device = null;
        deviceTrace = null;
        port = null;
        portTrace = null;
        user = null;
        setSupportState(SupportState.readyForStart);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!super.checkSupportState(SupportState.ready,supportName)) {
            if(alarmSupport!=null) alarmSupport.setAlarm(
                    fullName + " not ready",
                    AlarmSeverity.major);
            supportProcessRequester.supportProcessDone(RequestResult.failure);
            return;
        }
        if(supportProcessRequester==null) {
            throw new IllegalStateException("supportProcessRequester is null");
        }
        this.supportProcessRequester = supportProcessRequester;
        if(isProcessor) {
            deviceTrace.print(Trace.FLOW,
                "%s:%s process calling processContinue", fullName,supportName);
            processContinue();
        } else {
            deviceTrace.print(Trace.FLOW,
                    "%s:%s process calling queueRequest", fullName,supportName);
            user.setMessage(null);
            user.queueRequest(QueuePriority.medium);
        }
        deviceTrace.print(Trace.FLOW,"%s:%s process done", fullName,supportName);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
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
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {       
        deviceTrace.print(Trace.FLOW,
            "%s:%s processContinue calling supportProcessDone",
            fullName,supportName);
        supportProcessRequester.supportProcessDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.QueueRequestCallback#callback(org.epics.ioc.pdrv.Status, org.epics.ioc.pdrv.User)
     */
    public void callback(Status status, User user) {
        deviceTrace.print(Trace.FLOW,
            "%s:%s callback status %s",
            fullName,supportName,status.toString());
        if(status==Status.success) {
            deviceTrace.print(Trace.FLOW,
                    "%s:%s callback calling queueCallback", fullName,supportName);
            this.queueCallback();
        } else {
            deviceTrace.print(Trace.ERROR,
                    "%s:%s callback error %s",
                    fullName,supportName,user.getMessage());
        }
        deviceTrace.print(Trace.FLOW,
                "%s:%s callback calling processContinue", fullName,supportName);
        if(user.getAlarmSeverity()!=AlarmSeverity.none) {
        	alarmSupport.setAlarm(user.getAlarmMessage(),user.getAlarmSeverity());
        }
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.support.PDRVLinkSupport#isInterrupt()
     */
    public boolean isProcess() {
        return isProcessor;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.support.PDRVLinkSupport#queueCallback()
     */
    public void queueCallback() {
        deviceTrace.print(Trace.FLOW,
                "%s:%s queueCallback", fullName,supportName);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
     */
    public void recordProcessComplete() {
        deviceTrace.print(Trace.FLOW,
                "%s:%s recordProcessComplete", fullName,supportName);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
     */
    public void recordProcessResult(RequestResult requestResult) {
        deviceTrace.print(Trace.FLOW,
                "%s:%s recordProcessResult %s",
                fullName,supportName,requestResult.toString());
    }
    
}
