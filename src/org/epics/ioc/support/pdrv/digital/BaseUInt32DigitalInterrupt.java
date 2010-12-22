/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.digital;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.UInt32Digital;
import org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvData.property.PVEnumerated;
import org.epics.pvData.property.PVEnumeratedFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Implement UInt32DigitalInterrupt.
 * @author mrk
 *
 */
public class BaseUInt32DigitalInterrupt extends AbstractPortDriverInterruptLink implements UInt32DigitalInterruptListener
{
    /**
     * The constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseUInt32DigitalInterrupt(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    
    private ScalarType valueScalarType = null;
    private PVBoolean valuePVBoolean = null;
    private PVInt pvIndex = null;
    private UInt32Digital uint32Digital = null;
    private int value = 0;
    private PVInt pvMask = null;
    private int mask;
    private int shift = 0;
    private PVEnumerated enumerated = PVEnumeratedFactory.create();;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        pvMask = pvStructure.getIntField("mask");
        if(pvMask==null) {
            super.uninitialize();
            return;
        }
        if(valuePVField.getField().getType()==Type.scalar) {
            PVScalar pvScalar = (PVScalar)valuePVField;
            valueScalarType = pvScalar.getScalar().getScalarType();
            if(valueScalarType==ScalarType.pvBoolean) {
                valuePVBoolean = (PVBoolean)valuePVField;
                return;
            } else if(valueScalarType==ScalarType.pvInt) {
                pvIndex = (PVInt)valuePVField;
                return;
            }
        }
        if(enumerated.attach(valuePVField)) return;
        pvStructure.message("value field is not a valid type", MessageType.fatalError);
        super.uninitialize();
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#uninitialize()
     */
    @Override
    public void uninitialize() {
        super.uninitialize();
        valuePVBoolean = null;
        pvIndex = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = device.findInterface(user, "uint32Digital");
        if(iface==null) {
            pvStructure.message("interface uint32Digital not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        uint32Digital = (UInt32Digital)iface;
        if(enumerated.isAttached()) {
            String[] choices = uint32Digital.getChoices(user);
            if(choices!=null) {
                enumerated.setChoices(choices);
            }
        }
        if(valueScalarType!=null) {
            mask = pvMask.get();
            if(mask==0) {
                pvStructure.message("mask is 0", MessageType.fatalError);
                super.stop();
                return;
            }
            int i = 1;
            shift = 0;
            while(true) {
                if((mask&i)!=0) break;
                ++shift; i <<= 1;
            }
        }
        uint32Digital.addInterruptUser(user, this, mask);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    @Override
    public void stop() {
        super.stop();
        uint32Digital.removeInterruptUser(user, this);
        uint32Digital = null;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener#interrupt(int)
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
    						"pv %s support %s interrupt and record not processed",
    						fullName,supportName);
    			}
    		} finally {
    			pvRecord.unlock();
    		}
    	}
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
    public void becomeProcessor() {
    	putData(value);
    	recordProcess.process(processToken,false);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
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
     * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
     */
    @Override
    public void lostRightToProcess() {
    	isProcessor = false;
    	processToken = null;
    }	

    private void putData(int value) {
    	if(valueScalarType!=null) {
    		value = value&mask;
    		value >>>= shift;
    	}
    	if(valuePVBoolean!=null) {
    		valuePVBoolean.put((value==0) ? false : true);
    	} else if(pvIndex!=null)  {
    		pvIndex.put(value);
    	}else if(enumerated.isAttached()) {
            int oldValue = enumerated.getIndex();
            if(oldValue!=value) enumerated.setIndex(value);
    	} else {
    		pvStructure.message(" logic error", MessageType.fatalError);
    	}
    	if((deviceTrace.getMask()&Trace.FLOW)!=0) {
    		deviceTrace.print(Trace.FLOW,
    				"pv %s support %s putData",fullName,supportName);
    	}
    }
}
