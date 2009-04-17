/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.array;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Float64Array;
import org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVDoubleArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Implement Float64ArrayInterrupt.
 * @author mrk
 *
 */
public class BaseFloat64ArrayInterrupt extends AbstractPortDriverInterruptLink
implements Float64ArrayInterruptListener
{
    /**
     * The constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseFloat64ArrayInterrupt(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }

    private PVArray valuePVArray = null;
    private Float64Array float64Array = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(valuePVField.getField().getType()==Type.scalarArray) {
            valuePVArray = (PVArray)valuePVField;
            if(valuePVArray.getArray().getElementType().isNumeric()) return;   
        }
        super.uninitialize();
        pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#uninitialize()
     */
    public void uninitialize() {
        super.uninitialize();
        valuePVArray = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = device.findInterface(user, "float64Array");
        if(iface==null) {
            pvStructure.message("interface float64Array not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        float64Array = (Float64Array)iface;
        float64Array.addInterruptUser(user, this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    public void stop() {
        super.stop();
        float64Array.removeInterruptUser(user, this);
        float64Array = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64ArrayInterruptListener#interrupt(org.epics.ioc.pdrv.interfaces.Float64Array)
     */
    public void interrupt(Float64Array float64Array) {
        PVDoubleArray pvDoubleArray = float64Array.getPVDoubleArray();
        if(super.isProcess()) {
            recordProcess.setActive(this);
            Status status = float64Array.startRead(user);
            if(status==Status.success) {
                convert.copyArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
                float64Array.endRead(user);
            }
            recordProcess.process(this, false, null);
        } else {
            pvRecord.lock();
            try {

                Status status = float64Array.startRead(user);
                if(status==Status.success) {
                    convert.copyArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
                    float64Array.endRead(user);
                } else {
                    alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.invalid);
                } 
            } finally {
                pvRecord.unlock();
            }
            deviceTrace.print(Trace.SUPPORT,
                    "%s:%s interrupt and record not processed",
                    fullName,supportName);
        }
    }
}