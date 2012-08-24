/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.digital;

import org.epics.pvdata.property.PVEnumerated;
import org.epics.pvdata.property.PVEnumeratedFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.pdrv.Status;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.interfaces.Interface;
import org.epics.pvioc.pdrv.interfaces.UInt32Digital;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.pdrv.AbstractPortDriverSupport;

/**
 * Implement UInt32DigitalOutput.
 * @author mrk
 *
 */
public class BaseUInt32DigitalOutput extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseUInt32DigitalOutput(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    private ScalarType valueScalarType = null;
    private PVBoolean valuePVBoolean = null;
    private PVInt pvIndex = null;
    private UInt32Digital uint32Digital = null;
    private int value;
    private PVInt pvMask = null;
    private int mask;
    private int shift;
    private PVEnumerated enumerated = PVEnumeratedFactory.create();
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#initialize()
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
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#uninitialize()
     */
    @Override
    public void uninitialize() {
        super.uninitialize();
        valuePVBoolean = null;
        pvIndex = null;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#start()
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
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#stop()
     */
    @Override
    public void stop() {
        super.stop();
        uint32Digital = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    @Override
    public void endProcess() {
        if(valuePVBoolean!=null) {
            value = valuePVBoolean.get() ? 1 : 0;
        } else if(pvIndex!=null)  {
            value = pvIndex.get();
        } else if(enumerated.isAttached()) {
            int oldValue = enumerated.getIndex();
            if(oldValue!=value) enumerated.setIndex(value);
        } else {
            pvStructure.message(" logic error", MessageType.fatalError);
        }
        if(valueScalarType!=null) {
            value <<= shift;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    @Override
    public void queueCallback() {
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.print(Trace.SUPPORT, "pv %s queueCallback value = %d", fullName,value);
        }
        Status status = uint32Digital.write(user, value,mask);
        if(status!=Status.success) {
            if((deviceTrace.getMask()&Trace.ERROR)!=0) {
                deviceTrace.print(Trace.ERROR,
                    "pv %s support %s uint32Digital.write failed", fullName,supportName);
            }
            return;
        }
    }
}
