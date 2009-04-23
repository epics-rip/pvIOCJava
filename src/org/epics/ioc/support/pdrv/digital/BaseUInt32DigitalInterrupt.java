/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.digital;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.UInt32Digital;
import org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvData.misc.Enumerated;
import org.epics.pvData.misc.EnumeratedFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
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
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseUInt32DigitalInterrupt(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }
    
    private ScalarType valueScalarType = null;
    private PVBoolean valuePVBoolean = null;
    private PVInt pvIndex = null;
    private UInt32Digital uint32Digital = null;
    private PVInt pvMask = null;
    private int mask;
    private int shift = 0;
    private Enumerated enumerated = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
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
        enumerated = EnumeratedFactory.getEnumerated(valuePVField);
        if(enumerated!=null) {
            pvIndex = enumerated.getIndex();
            return;
        }
        pvStructure.message("value field is not a valid type", MessageType.fatalError);
        super.uninitialize();
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#uninitialize()
     */
    public void uninitialize() {
        super.uninitialize();
        valuePVBoolean = null;
        pvIndex = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
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
        if(enumerated!=null) {
            String[] choices = uint32Digital.getChoices(user);
            if(choices!=null) {
                PVStringArray pvStringArray = enumerated.getChoices();
                pvStringArray.put(0, choices.length, choices, 0);
                pvStringArray.postPut();
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
    public void stop() {
        super.stop();
        uint32Digital.removeInterruptUser(user, this);
        uint32Digital = null;
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.UInt32DigitalInterruptListener#interrupt(int)
     */
    public void interrupt(int value) {
        if(super.isProcess()) {
            recordProcess.setActive(this);
            putData(value);
            recordProcess.process(this, false, null);
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
    
    private void putData(int value) {
        if(valueScalarType!=null) {
            value = value&mask;
            value >>>= shift;
        }
        if(valuePVBoolean!=null) {
            valuePVBoolean.put((value==0) ? false : true);
            valuePVBoolean.postPut();
        } else if(pvIndex!=null)  {
            pvIndex.put(value);
            pvIndex.postPut();
        } else {
            pvStructure.message(" logic error", MessageType.fatalError);
        }
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,
                "pv %s support %s putData and  calling postPut",fullName,supportName);
        }
    }
}
