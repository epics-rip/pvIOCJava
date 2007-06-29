/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.support;

import org.epics.ioc.pdrv.*;
import org.epics.ioc.db.*;
import org.epics.ioc.pdrv.interfaces.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.support.*;
import org.epics.ioc.util.*;

/**
 * Abstract link support base class for PDRV links.
 * It handles all the generic things for a PDRV link.
 * It connects locates the interfaces for all the PDRV fields in the link structure.
 * For each of the lifetime methods it provides methods (initBase, startBase, stopBase, uninitBase)
 * than must be called by derived classes.
 * The process, setField, and message implementation should be sufficient for most derived support.
 * @author mrk
 *
 */
public abstract class AbstractPdrvLinkSupport extends AbstractLinkSupport implements
ProcessContinueRequester,QueueRequestCallback,
PdrvLinkSupport,
RecordProcessRequester
{
    /**
     * Constructor for derived support.
     * @param supportName The support name.
     * @param dbLink The link interface.
     */
    protected AbstractPdrvLinkSupport(String supportName,DBLink dbLink) {
        super(supportName,dbLink);
        this.supportName = supportName;
        this.dbLink = dbLink;
        pvLink = dbLink.getPVLink();
        fullName = pvLink.getFullName();
        dbRecord = dbLink.getDBRecord();
        pvRecord = dbRecord.getPVRecord();
        recordName = pvRecord.getRecordName();
        recordProcess = dbRecord.getRecordProcess();
    }      
    
    protected String supportName;
    protected DBLink dbLink;
    protected PVLink pvLink;
    protected DBField valueDBField = null;
    protected PVField valuePVField = null;
    protected DBRecord dbRecord = null;
    protected PVRecord pvRecord = null;
    protected String recordName = null;
    protected String fullName = null;
    protected RecordProcess recordProcess = null;
    
    protected AlarmSupport alarmSupport = null;
    protected PVStructure configStructure = null;;
    protected PVString pvPortName = null;
    protected PVInt pvAddr = null;
    protected PVInt pvMask = null;
    protected PVInt pvSize = null;
    protected PVDouble pvTimeout = null;
    protected PVBoolean pvProcess = null;
    protected PVString pvDrvParams = null;
    
    protected User user = null;
    protected Port port = null;
    protected Device device = null;
    protected Trace portTrace = null;
    protected Trace deviceTrace = null;
    
    protected DriverUser driverUser = null;
    
    protected SupportProcessRequester supportProcessRequester = null;
    
    private String alarmMessage = null;
    private boolean isProcess = false;
    
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
        if(valueDBField==null) {
            pvLink.message("setField was not called", MessageType.error);
            return;
        }
        valuePVField = valueDBField.getPVField();
        alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
        configStructure = super.getConfigStructure("pdrvLink", true);
        if(configStructure==null) return;
        pvPortName = configStructure.getStringField("portName");
        if(pvPortName==null) return;
        pvAddr = configStructure.getIntField("addr");
        if(pvAddr==null) return;
        pvMask = configStructure.getIntField("mask");
        if(pvMask==null) return;
        pvSize = configStructure.getIntField("size");
        if(pvSize==null) return;
        pvTimeout = configStructure.getDoubleField("timeout");
        if(pvTimeout==null) return;
        pvDrvParams = configStructure.getStringField("drvParams");
        if(pvDrvParams==null) return;
        pvProcess = configStructure.getBooleanField("process");
        setSupportState(SupportState.readyForStart);
    }
    
    public void uninitialize() {
        if(super.getSupportState()==SupportState.ready) {
            stop();
        }
        if(super.getSupportState()!=SupportState.readyForStart) return;
        pvDrvParams = null;
        pvTimeout = null;
        pvAddr = null;
        pvPortName = null;
        configStructure = null;
        alarmSupport = null;
        setSupportState(SupportState.readyForInitialize);
    }
    
    public void start() {
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        user = Factory.createUser(this);
        user.setTimeout(pvTimeout.get());
        port = user.connectPort(pvPortName.get());
        if(port==null) {
            pvLink.message(user.getMessage(),MessageType.error);
            return;
        }
        portTrace = port.getTrace();
        device = user.connectDevice(pvAddr.get());
        if(device==null) {
            pvLink.message(user.getMessage(),MessageType.error);
            return;
        }
        deviceTrace = device.getTrace();
        Interface iface = device.findInterface(user, "driverUser", true);
        if(iface!=null) {
            driverUser = (DriverUser)iface;
            driverUser.create(user,pvDrvParams.get() );
        }
        if(pvProcess!=null) {
            boolean interrupt = pvProcess.get();
            if(interrupt) {
                isProcess =recordProcess.setRecordProcessRequester(this);
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
        alarmMessage = null;
        if(isProcess) {
            deviceTrace.print(Trace.FLOW,
                "%s:%s process calling processContinue", fullName,supportName);
            processContinue();
        } else {
            deviceTrace.print(Trace.FLOW,
                    "%s:%s process calling queueRequest", fullName,supportName);
            port.queueRequest(user, QueuePriority.medium);
        }
        deviceTrace.print(Trace.FLOW,"%s:%s process done", fullName,supportName);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
     */
    public void setField(DBField dbField) {
        valueDBField = dbField;
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#message(java.lang.String, org.epics.ioc.util.MessageType)
     */
    public void message(String message,MessageType messageType) {
        dbRecord.lock();
        try {
            pvLink.message(message, messageType);
        } finally {
            dbRecord.unlock();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        if(alarmMessage!=null) {
            if(alarmSupport!=null) alarmSupport.setAlarm(alarmMessage,AlarmSeverity.major);
        }
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
            alarmMessage = fullName + " " + user.getMessage();
        }
        deviceTrace.print(Trace.FLOW,
                "%s:%s callback calling processContinue", fullName,supportName);
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.support.PdrvLinkSupport#isInterrupt()
     */
    public boolean isProcess() {
        return isProcess;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.support.PdrvLinkSupport#queueCallback()
     */
    public void queueCallback() {
        deviceTrace.print(Trace.ERROR,
                "%s:%s queueCallback not implemented", fullName,supportName);
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
