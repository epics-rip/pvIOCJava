/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.scalar;

import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.interfaces.Float64;
import org.epics.pvioc.pdrv.interfaces.Float64InterruptListener;
import org.epics.pvioc.pdrv.interfaces.Interface;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvioc.util.RequestResult;

/**
 * Implement Float64Average.
 * @author mrk
 *
 */
public class BaseFloat64Average extends AbstractPortDriverInterruptLink implements Float64InterruptListener
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The support name.
     */
    public BaseFloat64Average(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }

    private Float64 float64 = null;
    private int numValues = 0;
    private double sum = 0.0;
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(super.valuePVField.getField().getType()==Type.scalar) return;
        super.uninitialize();
        super.pvStructure.message("value field is not a scalar type", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    @Override
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
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    @Override
    public void stop() {
        super.stop();
        float64.removeInterruptUser(user, this);
        float64 = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#process(org.epics.pvioc.support.SupportProcessRequester)
     */
    @Override
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!super.checkSupportState(SupportState.ready,supportName)) {
            super.alarmSupport.setAlarm(
                    fullName + " not ready",
                    AlarmSeverity.MAJOR,AlarmStatus.DRIVER);
            supportProcessRequester.supportProcessDone(RequestResult.failure);
            return;
        }
        if(numValues==0) {
            super.alarmSupport.setAlarm(
                    fullName + " no new values",
                    AlarmSeverity.MAJOR,AlarmStatus.DRIVER);
        } else {
            double average = sum/numValues;
            convert.fromDouble((PVScalar)valuePVField, average);
            numValues = 0;
            sum = 0.0;
        }
        supportProcessRequester.supportProcessDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Float64InterruptListener#interrupt(double)
     */
    @Override
    public void interrupt(double value) {
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW, "pv %s interrupt", fullName);
        }
        super.pvRecord.lock();
        try {
            sum += (double)value;
            ++numValues;
        } finally {
            super.pvRecord.unlock();
        }
    }
	@Override
	public void becomeProcessor() {}
	@Override
	public void canNotProcess(String reason) {}
	@Override
	public void lostRightToProcess() {}
}

