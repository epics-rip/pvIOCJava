/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.Serial;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.*;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
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
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseSerialInput(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
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
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        super.initialize(recordSupport);
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
            Array array = (Array)field;
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
    public void start() {
        super.start();
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
    public void stop() {
        super.stop();
        byteArray = null;
        charArray = null;
    }            
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    public void endProcess() {
        super.endProcess();
        deviceTrace.print(Trace.FLOW,
            "%s:%s processContinue ",fullName,supportName);
        if(status==Status.success) {
            String stringValue = null;
            if(pvResponse!=null || !valueIsArray) {
                for(int i=0; i<nbytes; i++) charArray[i] = (char)byteArray[i];
                stringValue = String.copyValueOf(charArray, 0, nbytes);
            }
            if(pvResponse!=null) {
                pvResponse.put(stringValue);
                pvResponse.postPut();
            } else if(valueIsArray) {
                convert.fromByteArray((PVArray)valuePVField, 0, nbytes, byteArray, 0);
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
    public void queueCallback() {
        super.queueCallback();
        deviceTrace.print(Trace.FLOW,
            "%s:%s queueCallback calling read ",fullName,supportName);
        status = serial.read(user, byteArray, size);
        if(status!=Status.success) {
            deviceTrace.print(Trace.ERROR,
                    "%s:%s serial.read failed", fullName,supportName);
            return;
        }
        nbytes = user.getInt();
        deviceTrace.printIO(Trace.SUPPORT, byteArray, user.getInt(), "%s", fullName);
    }
}
