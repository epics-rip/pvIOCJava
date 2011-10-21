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
import org.epics.ioc.pdrv.interfaces.Int32Array;
import org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.AlarmStatus;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVIntArray;
import org.epics.pvData.pv.PVScalarArray;
import org.epics.pvData.pv.Type;

/**
 * Implement Int32ArrayInterrupt.
 * @author mrk
 *
 */
public class BaseInt32ArrayInterrupt extends AbstractPortDriverInterruptLink
implements Int32ArrayInterruptListener
{
    /**
     * The constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseInt32ArrayInterrupt(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }

    private PVScalarArray valuePVArray = null;
    private Int32Array int32Array = null;
    private PVIntArray pvIntArray = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#initialize()
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
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#uninitialize()
     */
    @Override
    public void uninitialize() {
        super.uninitialize();
        valuePVArray = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = device.findInterface(user, "int32Array");
        if(iface==null) {
            pvStructure.message("interface int32Array not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        int32Array = (Int32Array)iface;
        int32Array.addInterruptUser(user, this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    @Override
    public void stop() {
        if(super.getSupportState()!=SupportState.ready) return;
        super.stop();
        int32Array.removeInterruptUser(user, this);
        int32Array = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Int32ArrayInterruptListener#interrupt(org.epics.ioc.pdrv.interfaces.Int32Array)
     */
    @Override
    public void interrupt(Int32Array int32Array) {
    	pvIntArray = int32Array.getPVIntArray();
    	if(isProcessor) {
    		recordProcess.queueProcessRequest(processToken);
    	} else {
    		pvRecord.lock();
    		try {
    			Status status = int32Array.startRead(user);
    			if(status==Status.success) {
    				convert.copyScalarArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
    				int32Array.endRead(user);
    			} else {
    				alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.INVALID,AlarmStatus.DRIVER);
    			} 
    		}finally {
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
     * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
    public void becomeProcessor() {
    	Status status = int32Array.startRead(user);
    	if(status==Status.success) {
    		convert.copyScalarArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
    		int32Array.endRead(user);
    	}
    	recordProcess.process(processToken,false);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
     */
    @Override
    public void canNotProcess(String reason) {
    	pvRecord.lock();
    	try {
    		Status status = int32Array.startRead(user);
    		if(status==Status.success) {
    			convert.copyScalarArray(pvIntArray, 0, valuePVArray, 0, pvIntArray.getLength());
    			int32Array.endRead(user);
    		} else {
    			alarmSupport.setAlarm(user.getMessage(),AlarmSeverity.INVALID,AlarmStatus.DRIVER);
    		} 
    	} finally {
    		pvRecord.unlock();
    	}
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
     */
    @Override
    public void lostRightToProcess() {
    	isProcessor = false;
    	processToken = null;
    }
}
