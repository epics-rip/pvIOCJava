/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.digital;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.UInt32Digital;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
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
 * Implement UInt32DigitalOutput.
 * @author mrk
 *
 */
public class BaseUInt32DigitalOutput extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseUInt32DigitalOutput(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }
    private ScalarType valueScalarType = null;
    private PVBoolean valuePVBoolean = null;
    private PVInt pvIndex = null;
    private UInt32Digital uint32Digital = null;
    private int value;
    private PVInt pvMask = null;
    private int mask;
    private int shift;
    private Enumerated enumerated = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
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
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#uninitialize()
     */
    public void uninitialize() {
        super.uninitialize();
        valuePVBoolean = null;
        pvIndex = null;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#start()
     */
    public void start() {
        super.start();
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
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#stop()
     */
    public void stop() {
        super.stop();
        uint32Digital = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    public void endProcess() {
        if(valuePVBoolean!=null) {
            value = valuePVBoolean.get() ? 1 : 0;
        } else if(pvIndex!=null)  {
            value = pvIndex.get();
        } else {
            pvStructure.message(" logic error", MessageType.fatalError);
        }
        if(valueScalarType!=null) {
            value <<= shift;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    public void queueCallback() {
        deviceTrace.print(Trace.SUPPORT, "%s value = %d", fullName,value);
        Status status = uint32Digital.write(user, value,mask);
        if(status!=Status.success) {
            deviceTrace.print(Trace.ERROR,
                    "%s:%s uint32Digital.write failed", fullName,supportName);
            return;
        }
    }
}