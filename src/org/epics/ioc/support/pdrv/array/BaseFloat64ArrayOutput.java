/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.array;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Float64Array;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDoubleArray;
import org.epics.pvData.pv.PVScalarArray;
import org.epics.pvData.pv.Type;

/**
 * Implement Float64ArrayOutput
 * @author mrk
 *
 */
public class BaseFloat64ArrayOutput extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseFloat64ArrayOutput(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }

    private PVScalarArray valuePVArray = null;
    private Float64Array float64Array = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        if(valuePVField.getField().getType()==Type.scalarArray) {
            valuePVArray = (PVScalarArray)valuePVField;
            if(valuePVArray.getScalarArray().getElementType().isNumeric()) return;   
        }
        super.uninitialize();
        pvStructure.message("value field is not an array with numeric elements", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#uninitialize()
     */
    @Override
    public void uninitialize() {
        super.uninitialize();
        valuePVArray = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#start()
     */
    @Override
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
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#stop()
     */
    @Override
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        super.stop();
        float64Array = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    @Override
    public void queueCallback() {
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.print(Trace.SUPPORT, "pv %s queueCallback", fullName);
        }
        Status status = float64Array.startWrite(user);
        if(status==Status.success) {
            PVDoubleArray pvDoubleArray = float64Array.getPVDoubleArray();
            convert.copyScalarArray(valuePVArray, 0, pvDoubleArray, 0, valuePVArray.getLength());
            float64Array.endWrite(user);
        }
    }
}
