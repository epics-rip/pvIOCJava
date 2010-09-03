/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.Serial;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVInt;

/**
 * Implement SerialDiscard.
 * Issue a read and discard.
 * This is for use for instruments that respond to writes.
 * @author mrk
 *
 */
public class BaseSerialDiscard extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseSerialDiscard(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    
    private PVInt pvSize = null;
    private int size = 0;
    
    private Serial serial = null;
    private byte[] byteArray = null;
    private Status status = Status.success;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        pvSize = pvStructure.getIntField("size");
        if(pvSize==null) {
            super.uninitialize();
            return;
        }
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        size = pvSize.get();
        byteArray = new byte[size];
        Interface iface = device.findInterface(user, "serial");
        if(iface==null) {
            pvStructure.message("interface serial not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        serial = (Serial)iface;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#stop()
     */
    @Override
    public void stop() {
        super.stop();
        byteArray = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    @Override
    public void endProcess() {
        super.endProcess();
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,"pv %s endProcess ",fullName);
        }
        if(status!=Status.success) {
            alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
        }
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    @Override
    public void queueCallback() {
        super.queueCallback();
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,"pv %s queueCallback calling read ",fullName);
        }
        status = serial.read(user, byteArray, size);
        if(status!=Status.success) {
            deviceTrace.print(Trace.ERROR,
                    "%s:%s serial.read failed", fullName,supportName);
            return;
        }
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.printIO(Trace.SUPPORT, byteArray, user.getInt(), "%s", fullName);
        }
    }
}
