/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.scalar;

import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.interfaces.Float64;
import org.epics.pvioc.pdrv.interfaces.Interface;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.pdrv.AbstractPortDriverSupport;

/**
 * Implement Float64Input
 * @author mrk
 *
 */
public class BaseFloat64Input extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseFloat64Input(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }

    private Float64 float64 = null;
    private double value;
    private Status status = Status.success;
    private PVDouble pvLowLimit = null;
    private PVDouble pvHighLimit = null;
    private PVString pvUnits = null;
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        PVField pvField = pvProperty.findProperty(valuePVField,"display");
        if(pvField!=null) {
            PVStructure pvDisplay = (PVStructure)pvField; 
            pvUnits = pvDisplay.getStringField("units");
            pvLowLimit = pvDisplay.getDoubleField("limitLow");
            pvHighLimit = pvDisplay.getDoubleField("limitHigh");
        }
        if(valuePVField.getField().getType()==Type.scalar) return;
        super.uninitialize();
        pvStructure.message("value field is not a scalar type", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = device.findInterface(user, "float64");
        if(iface==null) {
            pvStructure.message("interface float64 not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        float64 = (Float64)iface;
        if(pvUnits!=null && (pvUnits.get()==null || pvUnits.get().length()==0)) {
            String units = float64.getUnits(user);
            pvUnits.put(units);
        }
        if(pvLowLimit!=null && pvHighLimit!=null) {
            if(pvLowLimit.get()==pvHighLimit.get()) {
                double[] limits = float64.getDisplayLimits(user);
                if(limits!=null) {
                    pvLowLimit.put(limits[0]);
                    pvHighLimit.put(limits[1]);
                }
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#stop()
     */
    @Override
    public void stop() {
        super.stop();
        float64 = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    @Override
    public void queueCallback() {
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,
                "pv %s queueCallback calling read ",fullName);
        }
        Status status = float64.read(user);
        if(status!=Status.success) {
            if((deviceTrace.getMask()&Trace.ERROR)!=0) {
                deviceTrace.print(Trace.ERROR,
                    "pv %s support %s float64.read failed", fullName,supportName);
            }
            alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.INVALID,AlarmStatus.DRIVER);
            return;
        }
        value = user.getDouble();
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.print(Trace.SUPPORT, "pv %s value = %e", fullName,value);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    @Override
    public void endProcess() {
        if(status==Status.success) {
            convert.fromDouble((PVScalar)valuePVField, value);
        } else {
            alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.INVALID,AlarmStatus.DRIVER);
        }
    }        
}
