/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.scalar;


import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Int32;
import org.epics.ioc.pdrv.interfaces.Int32InterruptListener;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Implement Int32Interrupt.
 * @author mrk
 *
 */
public class BaseInt32Interrupt extends AbstractPortDriverInterruptLink
implements Int32InterruptListener
{
    /**
     * The constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseInt32Interrupt(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }
    
    private Int32 int32 = null;
    private PVScalar pvValue = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(valuePVField.getField().getType()==Type.scalar) {
            pvValue = (PVScalar)valuePVField;
            return;
        }
        super.uninitialize();
        pvStructure.message("value field is not a scalar type", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = device.findInterface(user, "int32");
        if(iface==null) {
            pvStructure.message("interface int32 not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        int32 = (Int32)iface;
        int32.addInterruptUser(user, this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    public void stop() {
        super.stop();
        int32.removeInterruptUser(user, this);
        int32 = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32InterruptListener#interrupt(int)
     */
    public void interrupt(int value) {
        if(super.isProcess()) {
            recordProcess.setActive(this);
            putData(value);
            recordProcess.process(this, false, null);
        } else {
            pvRecord.lock();
            try {
                putData(value);
                deviceTrace.print(Trace.FLOW,
                    "%s:%s interrupt and record not processed",
                    fullName,supportName);
            } finally {
                pvRecord.unlock();
            }
        }
    }
    
    private void putData(int value) {
        convert.fromInt(pvValue, value);
        deviceTrace.print(Trace.FLOW,
            "%s:%s putData value " + value,fullName,supportName);
    }
}
