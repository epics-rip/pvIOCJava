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
ProcessContinueRequester,QueueRequestCallback
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
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize()
     */
    abstract public void initialize();
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#uninitialize()
     */
    abstract public void uninitialize();
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    abstract public void start();
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
     */
    abstract public void stop();
    /**
     * A method that must be implemented by derived support.
     * It is called from a queueRequest callback.
     */
    abstract public void queueCallback();
    
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
    protected PVString pvDrvParams = null;
    
    protected PdrvLink pdrvLink = new PdrvLink();
    
    protected User user = null;
    protected Port port = null;
    protected Device device = null;
    protected Trace portTrace = null;
    protected Trace deviceTrace = null;
    
    
    protected DriverUser driverUser = null;
    
    protected SupportProcessRequester supportProcessRequester = null;
    
    private String alarmMessage = null;
    
    protected boolean initBase() {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return false;
        if(valueDBField!=null) {
            valuePVField = valueDBField.getPVField();
        }
        alarmSupport = AlarmFactory.findAlarmSupport(dbLink);
        configStructure = super.getConfigStructure("pdrvLink", true);
        if(configStructure==null) return false;
        pvPortName = configStructure.getStringField("portName");
        if(pvPortName==null) return false;
        pvAddr = configStructure.getIntField("addr");
        if(pvAddr==null) return false;
        pvMask = configStructure.getIntField("mask");
        if(pvMask==null) return false;
        pvSize = configStructure.getIntField("size");
        if(pvSize==null) return false;
        pvTimeout = configStructure.getDoubleField("timeout");
        if(pvTimeout==null) return false;
        pvDrvParams = configStructure.getStringField("drvParams");
        if(pvDrvParams==null) return false;
        setSupportState(SupportState.readyForStart);
        return true;
    }
    
    protected boolean uninitBase() {
        if(super.getSupportState()==SupportState.ready) {
            stop();
        }
        if(super.getSupportState()!=SupportState.readyForStart) return false;
        pvDrvParams = null;
        pvTimeout = null;
        pvAddr = null;
        pvPortName = null;
        configStructure = null;
        alarmSupport = null;
        setSupportState(SupportState.readyForInitialize);
        return true;
    }
    
    protected boolean startBase() {
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return false;
        user = Factory.createUser(this);
        pdrvLink.portName = pvPortName.get();
        pdrvLink.addr = pvAddr.get();
        pdrvLink.mask = pvMask.get();
        pdrvLink.timeout = pvTimeout.get();
        user.setTimeout(pdrvLink.timeout);
        pdrvLink.drvParams = pvDrvParams.get();
        port = user.connectPort(pdrvLink.portName);
        if(port==null) {
            pvLink.message(user.getMessage(),MessageType.error);
            return false;
        }
        portTrace = port.getTrace();
        device = user.connectDevice(pdrvLink.addr);
        if(device==null) {
            pvLink.message(user.getMessage(),MessageType.error);
            return false;
        }
        deviceTrace = device.getTrace();
        Interface iface = device.findInterface(user, "driverUser", true);
        if(iface!=null) {
            driverUser = (DriverUser)iface;
            driverUser.create(user, pdrvLink.drvParams);
        }
        setSupportState(SupportState.ready);
        return true;
    }
    
    protected boolean stopBase() {
        if(super.getSupportState()!=SupportState.ready) return false ;
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
        return true;
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!super.checkSupportState(SupportState.ready,supportName)) {
            if(alarmSupport!=null) alarmSupport.setAlarm(
                    pvLink.getFullFieldName() + " not ready",
                    AlarmSeverity.major);
            supportProcessRequester.supportProcessDone(RequestResult.failure);
            return;
        }
        if(supportProcessRequester==null) {
            throw new IllegalStateException("supportProcessRequester is null");
        }
        this.supportProcessRequester = supportProcessRequester;
        alarmMessage = null;
        port.queueRequest(user, QueuePriority.medium);
    }   
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#setField(org.epics.ioc.db.DBField)
     */
    public void setField(DBField dbField) {
        valueDBField = dbField;
        valuePVField = valueDBField.getPVField();
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
        supportProcessRequester.supportProcessDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.QueueRequestCallback#callback(org.epics.ioc.pdrv.Status, org.epics.ioc.pdrv.User)
     */
    public void callback(Status status, User user) {
        if(status==Status.success) {
            this.queueCallback();
        } else {
            alarmMessage = pvLink.getFullFieldName() + " " + user.getMessage();
        }
        recordProcess.processContinue(this);
    }
    
}
