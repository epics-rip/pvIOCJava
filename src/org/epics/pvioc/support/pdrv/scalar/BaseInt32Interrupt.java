/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.scalar;


import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.interfaces.Int32;
import org.epics.pvioc.pdrv.interfaces.Int32InterruptListener;
import org.epics.pvioc.pdrv.interfaces.Interface;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink;

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
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseInt32Interrupt(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    
    private Int32 int32 = null;
    private int value = 0;
    private PVScalar pvValue = null;
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
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
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    @Override
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
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    @Override
    public void stop() {
        super.stop();
        int32.removeInterruptUser(user, this);
        int32 = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.Int32InterruptListener#interrupt(int)
     */
    @Override
    public void interrupt(int value) {
    	this.value = value;
    	if(isProcessor) {
    		recordProcess.queueProcessRequest(processToken);
        } else {
            pvRecord.lock();
            try {
                putData(value);
                if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
                    deviceTrace.print(Trace.SUPPORT,
                        "pv %s interrupt and record not processed value %d",fullName,value);
                }
            } finally {
                pvRecord.unlock();
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
    public void becomeProcessor() {
    	putData(value);
    	recordProcess.process(processToken,false);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
     */
    @Override
    public void canNotProcess(String reason) {
    	pvRecord.lock();
    	try {
    		putData(value);
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

    
    private void putData(int value) {
        convert.fromInt(pvValue, value);
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.print(Trace.SUPPORT,"pv %s putData value %d",fullName,value);
        }
    }
}
