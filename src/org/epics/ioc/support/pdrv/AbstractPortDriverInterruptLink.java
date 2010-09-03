/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv;


import org.epics.ioc.database.PVRecord;
import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.pdrv.Device;
import org.epics.ioc.pdrv.Factory;
import org.epics.ioc.pdrv.Port;
import org.epics.ioc.pdrv.QueueRequestCallback;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.User;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.factory.ConvertFactory;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.pv.Convert;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;

/**
 * Abstract  base class for PortDriverInterruptLink.
 * It creates a User, connects to a port and a device, and attempts to become the recordProcessor
 * if process is true. Then derived class must handle the interrupt.
 * @author mrk
 *
 */
public abstract class AbstractPortDriverInterruptLink extends AbstractSupport
implements RecordProcessRequester,QueueRequestCallback
{
    /**
     * Constructor for derived PortDriverInterruptLink Support.
     * @param supportName The support name.
     * @param pvRecordStructure The link interface.
     */
    protected AbstractPortDriverInterruptLink(String supportName,PVRecordStructure pvRecordStructure) {
        super(supportName,pvRecordStructure);
        this.supportName = supportName;
        this.pvRecordStructure = pvRecordStructure;
        pvStructure = pvRecordStructure.getPVStructure();
        fullName = pvRecordStructure.getFullName();
        pvRecord = pvRecordStructure.getPVRecord();
        recordName = pvRecord.getRecordName();
    }  
    
    protected static Convert convert = ConvertFactory.getConvert();
    protected static PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    protected final String supportName;
    protected final PVRecordStructure pvRecordStructure;
    protected final PVStructure pvStructure;
    protected final String fullName;
    protected final PVRecord pvRecord;
    protected final String recordName;
    
    protected RecordProcess recordProcess = null;
    protected ProcessToken processToken = null;
    protected PVField valuePVField = null;
    protected AlarmSupport alarmSupport = null;
    protected PVString pvPortName = null;
    protected PVString pvDeviceName = null;
    protected PVBoolean pvProcess = null;
    
    protected User user = null;
    protected Port port = null;
    protected Trace portTrace = null;
    protected Device device = null;
    protected Trace deviceTrace = null;
    protected boolean isProcessor = false;
    
    protected SupportProcessRequester supportProcessRequester = null;
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize()
     */
    @Override
    public void initialize() {
        if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
        recordProcess = pvRecord.getRecordProcess();
        PVStructure pvParent = pvRecordStructure.getPVStructure().getParent();
        while(pvParent!=null) {
            valuePVField = pvParent.getSubField("value");
            if(valuePVField!=null) break;
            pvParent = pvParent.getParent();
        }
        if(valuePVField==null) {
            pvRecordStructure.message("value field not found", MessageType.error);
            return;
        }
        alarmSupport = AlarmSupportFactory.findAlarmSupport(pvRecordStructure);
        pvPortName = pvStructure.getStringField("portName");
        if(pvPortName==null) return;
        pvDeviceName = pvStructure.getStringField("deviceName");
        if(pvDeviceName==null) return;
        if(pvStructure.getSubField("process")!=null) {
        	pvProcess = pvStructure.getBooleanField("process");
        } else {
        	pvProcess = null;
        }
        setSupportState(SupportState.readyForStart);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#uninitialize()
     */
    @Override
    public void uninitialize() {
        if(super.getSupportState()==SupportState.ready) {
            stop();
        }
        if(super.getSupportState()!=SupportState.readyForStart) return;
        pvProcess = null;
        pvDeviceName = null;
        pvPortName = null;
        alarmSupport = null;
        valuePVField = null;
        recordProcess = null;
        setSupportState(SupportState.readyForInitialize);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        user = Factory.createUser(this);
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
        if(pvProcess!=null && pvProcess.get()) {
        	processToken = recordProcess.requestProcessToken(this);
            isProcessor = (processToken==null) ? false : true;
            if(!isProcessor) {
                pvStructure.message("could not become record processor", MessageType.error);
            }
        }
        setSupportState(SupportState.ready);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
     */
    @Override
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        if(isProcessor) recordProcess.releaseProcessToken(processToken);
        isProcessor = false;
        user.disconnectPort();
        device = null;
        deviceTrace = null;
        port = null;
        portTrace = null;
        user = null;
        setSupportState(SupportState.readyForStart);
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.QueueRequestCallback#callback(org.epics.ioc.pdrv.Status, org.epics.ioc.pdrv.User)
     */
    @Override
    public void callback(Status status, User user) {
        if((deviceTrace.getMask()&Trace.ERROR)!=0) {
            deviceTrace.print(Trace.ERROR,
                "record %s support %s why was callback called???%n",
                fullName,supportName);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
     */
    @Override
    public void recordProcessComplete() {}
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
     */
    @Override
    public void recordProcessResult(RequestResult requestResult) {}
}
