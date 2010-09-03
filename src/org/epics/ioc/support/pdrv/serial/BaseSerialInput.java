/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.Serial;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVScalarArray;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarArray;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Implement SerialInput.
 * @author mrk
 *
 */
public class BaseSerialInput extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseSerialInput(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    
    private PVString pvResponse = null;
    private boolean valueIsArray = false;
    private PVInt pvSize = null;
    private int size = 0;
    
    private Serial serial = null;
    private byte[] byteArray = null;
    private char[] charArray = null;
    private int nbytes = 0;
    private Status status = Status.success;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        pvSize = pvStructure.getIntField("size");
        if(pvSize==null) {
            super.uninitialize();
            return;
        }
        PVStructure pvParent = pvStructure.getParent();
        PVField pvField = pvParent.getSubField("response");
        if(pvField!=null) {
            pvResponse = pvParent.getStringField("response");
            if(pvResponse==null) {
                super.uninitialize();
                return;
            }
            return;
        }
        Field field = valuePVField.getField();
        if(field.getType()==Type.scalarArray) {
            ScalarArray array = (ScalarArray)field;
            ScalarType elementType = array.getElementType();
            if(!elementType.isNumeric()) {
                pvStructure.message("value field is not a supported type", MessageType.fatalError);
                super.uninitialize();
                return;
            }
            valueIsArray = true;
        }
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        size = pvSize.get();
        byteArray = new byte[size];
        if(pvResponse!=null || !valueIsArray) charArray = new char[size];
        Interface iface = device.findInterface(user, "serial");
        if(iface==null) {
            pvStructure.message("interface serial not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        serial = (Serial)iface;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#stop()
     */
    @Override
    public void stop() {
        super.stop();
        byteArray = null;
        charArray = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    @Override
    public void endProcess() {
        super.endProcess();
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,"pv %s endProcess",fullName);
        }
        if(status==Status.success) {
            String stringValue = null;
            if(pvResponse!=null || !valueIsArray) {
                for(int i=0; i<nbytes; i++) charArray[i] = (char)byteArray[i];
                stringValue = String.copyValueOf(charArray, 0, nbytes);
            }
            if(pvResponse!=null) {
                pvResponse.put(stringValue);
            } else if(valueIsArray) {
                convert.fromByteArray((PVScalarArray)valuePVField, 0, nbytes, byteArray, 0);
            } else {
                convert.fromString((PVScalar)valuePVField, stringValue);
            }
        } else {
            alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
        }
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    @Override
    public void queueCallback() {
        super.queueCallback();
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,
            "pv %s queueCallback calling read ",fullName);
        }
        status = serial.read(user, byteArray, size);
        if(status!=Status.success) {
            if((deviceTrace.getMask()&Trace.ERROR)!=0) {
                deviceTrace.print(Trace.ERROR,
                    "pv %s support %s serial.read failed", fullName,supportName);
            }
            return;
        }
        nbytes = user.getInt();
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.printIO(Trace.SUPPORT, byteArray, user.getInt(), "%s", fullName);
        }
    }
}
