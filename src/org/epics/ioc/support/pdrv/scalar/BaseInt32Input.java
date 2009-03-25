/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.scalar;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Int32;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Type;
/**
 * Implement Int32Input
 * @author mrk
 *
 */
public class BaseInt32Input extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseInt32Input(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }

    private Int32 int32 = null;
    private int value;
    private Status status = Status.success;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(valuePVField.getField().getType()==Type.scalar) return;
        super.uninitialize();
        pvStructure.message("value field is not a scalar type", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#start()
     */
    public void start() {
        super.start();
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = device.findInterface(user, "int32");
        if(iface==null) {
            pvStructure.message("interface int32 not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        int32 = (Int32)iface;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#stop()
     */
    public void stop() {
        super.stop();
        int32 = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    public void queueCallback() {
        deviceTrace.print(Trace.FLOW,
                "%s:%s queueCallback calling read ",fullName,supportName);
        status = int32.read(user);
        if(status!=Status.success) {
            deviceTrace.print(Trace.ERROR,
                    "%s:%s int32.read failed", fullName,supportName);
            return;
        }
        value = user.getInt();
        deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    @Override
    public void endProcess() {
        if(status==Status.success) {
            convert.fromInt((PVScalar)valuePVField, value);
        } else {
            alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
        }
    }        
}
