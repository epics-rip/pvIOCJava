/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv;


import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.property.PVProperty;
import org.epics.pvdata.property.PVPropertyFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.pdrv.Device;
import org.epics.pvioc.pdrv.Port;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.User;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.alarm.AlarmSupport;
import org.epics.pvioc.support.basic.GenericBase;

/**
 * Abstract link support base class for PDRV links.
 * It handles all the generic things for a PDRV link.
 * It connects and locates the interfaces for all the PDRV fields in the link structure.
 * For each of the lifetime methods it provides methods (initBase, startBase, stopBase, uninitBase)
 * that must be called by derived classes.
 * The process and message implementation should be sufficient for most derived support.
 * @author mrk
 *
 */
public abstract class AbstractPortDriverSupport extends GenericBase
implements PortDriverSupport
{
    /**
     * Constructor for derived support.
     * @param supportName The support name.
     * @param pvRecordStructure The link interface.
     */
    protected AbstractPortDriverSupport(String supportName,PVRecordStructure pvRecordStructure) {
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
    protected static final String emptyString = "";
    protected final String supportName;
    protected final PVRecordStructure pvRecordStructure;
    protected final PVStructure pvStructure;
    protected final PVRecord pvRecord;
    protected final String recordName;
    protected final String fullName;
    
    protected PVField valuePVField = null;
    protected AlarmSupport alarmSupport = null;
   
    protected PortDriverLink portDriverLink = null;
    protected User user = null;
    protected Port port = null;
    protected Device device = null;
    protected Trace portTrace = null;
    protected Trace deviceTrace = null;
    
    protected SupportProcessRequester supportProcessRequester = null;
    private PortDriverSupport[] portDriverSupports = null;
    
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.basic.GenericBase#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        PVStructure pvParent = pvStructure.getParent();
        while(pvParent!=null) {
            valuePVField = pvParent.getSubField("value");
            if(valuePVField!=null) break;
            pvParent = pvParent.getParent();
        }
        if(valuePVField==null) {
            super.uninitialize();
            return;
        }
        Support[] supports = super.getSupports();
        int number = 0;
        for(Support support : supports) {
            if(support instanceof PortDriverSupport) number++;
        }
        portDriverSupports = new PortDriverSupport[number];
        int index = 0;
        for(Support support : supports) {
            if(support instanceof PortDriverSupport) {
                portDriverSupports[index++] = (PortDriverSupport)support;
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.basic.GenericBase#uninitialize()
     */
    @Override
    public void uninitialize() {
        super.uninitialize();
        alarmSupport = null;
        valuePVField = null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.basic.GenericBase#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        port = user.getPort();
        if(port==null) {
            pvStructure.message(user.getMessage(),MessageType.error);
            return;
        }
        portTrace = port.getTrace();
        device = user.getDevice();
        if(device==null) {
            pvStructure.message(user.getMessage(),MessageType.error);
            return;
        }
        deviceTrace = device.getTrace();
        for(PortDriverSupport portDriverSupport : portDriverSupports) {
            portDriverSupport.setPortDriverLink(portDriverLink);
        }
        super.start(afterStart);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.PortDriverSupport#setPortDriverLink(org.epics.pvioc.support.pdrv.PortDriverLink)
     */
    @Override
    public void setPortDriverLink(PortDriverLink portDriverLink) {
        this.portDriverLink = portDriverLink;
        user = portDriverLink.getUser();
        alarmSupport = portDriverLink.getAlarmSupport();
        for(PortDriverSupport portDriverSupport : portDriverSupports) {
            portDriverSupport.setPortDriverLink(portDriverLink);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.basic.GenericBase#stop()
     */
    @Override
    public void stop() {
        super.stop();
        device = null;
        deviceTrace = null;
        port = null;
        portTrace = null;
        user = null;
    }     
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.basic.GenericBase#process(org.epics.pvioc.support.SupportProcessRequester)
     */
    @Override
    public void process(SupportProcessRequester supportProcessRequester) {
        throw new IllegalStateException("process should never be called");
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.PortDriverSupport#processCallback()
     */
    @Override
    public void beginProcess() {
        for(PortDriverSupport portDriverSupport : portDriverSupports) {
            portDriverSupport.beginProcess();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.PortDriverSupport#endProcess()
     */
    @Override
    public void endProcess() {
        for(PortDriverSupport portDriverSupport : portDriverSupports) {
            portDriverSupport.endProcess();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.PortDriverSupport#queueCallback()
     */
    @Override
    public void queueCallback() {
        for(PortDriverSupport portDriverSupport : portDriverSupports) {
            portDriverSupport.queueCallback();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#message(java.lang.String, org.epics.pvioc.util.MessageType)
     */
    @Override
    public void message(String message,MessageType messageType) {
        pvRecord.lock();
        try {
            pvRecordStructure.message(message, messageType);
        } finally {
            pvRecord.unlock();
        }
    }
}
