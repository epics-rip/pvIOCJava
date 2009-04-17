/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.scalar;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.pdrv.interfaces.Float64;
import org.epics.ioc.pdrv.interfaces.Float64InterruptListener;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;

/**
 * Implement Float64Average.
 * @author mrk
 *
 */
public class BaseFloat64Average extends AbstractPortDriverInterruptLink implements Float64InterruptListener
{
    /**
     * Constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The support name.
     */
    public BaseFloat64Average(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }

    private Float64 float64 = null;
    private int numValues = 0;
    private double sum = 0.0;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(super.valuePVField.getField().getType()==Type.scalar) return;
        super.uninitialize();
        super.pvStructure.message("value field is not a scalar type", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = super.device.findInterface(user, "float64");
        if(iface==null) {
            super.pvStructure.message("interface float64 not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        float64 = (Float64)iface;
        float64.addInterruptUser(user, this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    public void stop() {
        super.stop();
        float64.removeInterruptUser(user, this);
        float64 = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.support.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!super.checkSupportState(SupportState.ready,supportName)) {
            super.alarmSupport.setAlarm(
                    fullName + " not ready",
                    AlarmSeverity.major);
            supportProcessRequester.supportProcessDone(RequestResult.failure);
            return;
        }
        if(numValues==0) {
            super.alarmSupport.setAlarm(
                    fullName + " no new values",
                    AlarmSeverity.major);
        } else {
            double average = sum/numValues;
            convert.fromDouble((PVScalar)valuePVField, average);
            numValues = 0;
            sum = 0.0;
        }
        supportProcessRequester.supportProcessDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64InterruptListener#interrupt(double)
     */
    public void interrupt(double value) {
        super.pvRecord.lock();
        try {
            sum += (double)value;
            ++numValues;
        } finally {
            super.pvRecord.unlock();
        }
    }
}

