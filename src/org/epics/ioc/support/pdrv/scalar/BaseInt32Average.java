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
 * Implement Int32Average.
 * @author mrk
 *
 */
public class BaseInt32Average extends AbstractPortDriverInterruptLink
implements Int32InterruptListener
{
    /**
     * Constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The support name.
     */
    public BaseInt32Average(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }

    private Int32 int32 = null;
    private int numValues = 0;
    private long sum = 0;
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
        Interface iface = super.device.findInterface(user, "int32");
        if(iface==null) {
            super.pvStructure.message("interface int32 not supported", MessageType.fatalError);
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
            double average = ((double)sum)/numValues;
            convert.fromDouble((PVScalar)valuePVField, average);
            numValues = 0;
            sum = 0;
        }
        supportProcessRequester.supportProcessDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32InterruptListener#interrupt(int)
     */
    public void interrupt(int value) {
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,
                "pv %s support %s interrupt",
                fullName,supportName);
        }
        super.pvRecord.lock();
        try {
            sum += (long)value;
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