/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.array;

import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDoubleArray;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.interfaces.Float64Array;
import org.epics.pvioc.pdrv.interfaces.Float64ArrayInterruptListener;
import org.epics.pvioc.pdrv.interfaces.Interface;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink;

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
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseFloat64ArrayInterrupt(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    private PVScalarArray valuePVArray = null;
    private Float64Array float64Array = null;
    private PVDoubleArray pvDoubleArray = null;
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#initialize()
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
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#uninitialize()
     */
    @Override
    public void uninitialize() {
        super.uninitialize();
        valuePVArray = null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#start()
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
        float64Array.addInterruptUser(user, this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    @Override
    public void stop() {
        super.stop();
        float64Array.removeInterruptUser(user, this);
        float64Array = null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Float64ArrayInterruptListener#interrupt(org.epics.pvioc.pdrv.interfaces.Float64Array)
     */
    @Override
    public void interrupt(Float64Array float64Array) {
        pvDoubleArray = float64Array.getPVDoubleArray();
        if(isProcessor) {
        	recordProcess.queueProcessRequest(processToken);
        } else {
            pvRecord.lock();
            try {
                Status status = float64Array.startRead(user);
                if(status==Status.success) {
                    convert.copyScalarArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
                    float64Array.endRead(user);
                } else {
                    alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.INVALID,AlarmStatus.DRIVER);
                } 
            } finally {
                pvRecord.unlock();
            }
            if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
                deviceTrace.print(Trace.SUPPORT,
                    "pv %s support %s interrupt and record not processed",
                    fullName,supportName);
            }
        }
    }
	/* (non-Javadoc)
	 * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
	 */
	@Override
	public void becomeProcessor() {
		Status status = float64Array.startRead(user);
        if(status==Status.success) {
            convert.copyScalarArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
            float64Array.endRead(user);
        }
        recordProcess.process(processToken,false);
	}
	/* (non-Javadoc)
	 * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
	 */
	@Override
	public void canNotProcess(String reason) {
		pvRecord.lock();
        try {
            Status status = float64Array.startRead(user);
            if(status==Status.success) {
                convert.copyScalarArray(pvDoubleArray, 0, valuePVArray, 0, pvDoubleArray.getLength());
                float64Array.endRead(user);
            } else {
                alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.INVALID,AlarmStatus.DRIVER);
            } 
        } finally {
            pvRecord.unlock();
        }
	}
	/* (non-Javadoc)
	 * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
	 */
	@Override
	public void lostRightToProcess() {
		isProcessor = false;
		processToken = null;
	}
}
