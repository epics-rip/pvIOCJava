/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.pdrv.Status;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.Serial;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverSupport;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * @author mrk
 *
 */
public class BaseSerialOutput extends AbstractPortDriverSupport
{
    /**
     * Constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseSerialOutput(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }
    
    private PVString pvSend = null;
    private boolean valueIsArray = false;
    private PVArray valuePVArray = null;
    private PVInt pvSize = null;
    private int size = 0;
    
    private Serial serial = null;
    private byte[] byteArray = null;
    private int nbytes = 0;
    private Status status = Status.success;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        pvSize = pvStructure.getIntField("size");
        if(pvSize==null) {
            super.uninitialize();
            return;
        }
        PVStructure pvParent = pvStructure.getParent();
        PVField pvField = pvParent.getSubField("request");
        if(pvField!=null) {
            pvSend = pvParent.getStringField("request");
            if(pvSend==null) {
                super.uninitialize();
                return;
            }
            return;
        }
        pvField = pvParent.getSubField("command");
        if(pvField!=null) {
            pvSend = pvParent.getStringField("command");
            if(pvSend==null) {
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
            valuePVArray = (PVArray)valuePVField;
        }
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        size = pvSize.get();
        byteArray = new byte[size];
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
    } 
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#beginProcess()
     */
    public void beginProcess() {
        super.beginProcess();
        if(valueIsArray) {
            nbytes = valuePVArray.getLength();
            if(size<nbytes) {
                size = nbytes; byteArray = new byte[size];
            }
            nbytes = convert.toByteArray(valuePVArray, 0, nbytes, byteArray, 0);
        } else {
            String string = null;
            if(pvSend!=null) {
                string = pvSend.get();
            } else {
                string = convert.getString(valuePVField);
            }
            if(string==null) string = "";
            nbytes = string.length();
            if(size<nbytes) {
                size = nbytes;
                byteArray = new byte[size];
            }
            for(int i=0; i<nbytes; i++) {
                char nextChar = string.charAt(i);
                byteArray[i] = (byte)nextChar;
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#endProcess()
     */
    public void endProcess() {
        super.endProcess();
        if(status!=Status.success) {
            alarmSupport.setAlarm(user.getMessage(), AlarmSeverity.invalid);
        }
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverSupport#queueCallback()
     */
    public void queueCallback() {
        super.queueCallback();
        if((deviceTrace.getMask()&Trace.FLOW)!=0) {
            deviceTrace.print(Trace.FLOW,"pv %s calling write",fullName);
        }
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.printIO(Trace.SUPPORT, byteArray, nbytes, "%s", fullName);
        }
        status = serial.write(user, byteArray, nbytes);
        if(status!=Status.success) {
            if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                deviceTrace.print(Trace.ERROR,
                    "pv %s support %s serial.write failed", fullName,supportName);
            }
            return;
        }
        if(user.getInt()!=nbytes) {
            if((deviceTrace.getMask()&Trace.FLOW)!=0) {
                deviceTrace.print(Trace.ERROR,
                    "pv %s support %s write requested %d bytes set %d bytes",
                    fullName,supportName,nbytes,user.getInt());
            }
        }
    }
}
