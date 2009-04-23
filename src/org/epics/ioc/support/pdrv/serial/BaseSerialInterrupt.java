/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.serial;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.pdrv.interfaces.Serial;
import org.epics.ioc.pdrv.interfaces.SerialInterruptListener;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvData.pv.Array;
import org.epics.pvData.pv.Field;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVInt;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Implement OctetInterrupt.
 * @author mrk
 *
 */
public class BaseSerialInterrupt extends AbstractPortDriverInterruptLink
implements SerialInterruptListener
{
    /**
     * The constructor.
     * @param pvStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseSerialInterrupt(PVStructure pvStructure,String supportName) {
        super(supportName,pvStructure);
    }
    
    private boolean valueIsArray = false;
    private PVInt pvSize = null;
    private int size = 0;
    
    private Serial octet = null;
    private char[] charArray = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#initialize()
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        pvSize = pvStructure.getIntField("size");
        if(pvSize==null) {
            super.uninitialize();
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
     * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        size = pvSize.get();
        if(valueIsArray) charArray = new char[size];
        Interface iface = device.findInterface(user, "octet");
        if(iface==null) {
            pvStructure.message("interface octet not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        octet = (Serial)iface;
        octet.addInterruptUser(user, this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.support.AbstractPDRVLinkSupport#stop()
     */
    public void stop() {
        super.stop();
        octet.removeInterruptUser(user, this);
        charArray = null;
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.SerialInterruptListener#interrupt(byte[], int)
     */
    public void interrupt(byte[] data, int nbytes) {
        if(super.isProcess()) {
            recordProcess.setActive(this);
            putData(data,nbytes);
            recordProcess.process(this, false, null);
        } else {
            pvRecord.lock();
            try {
                putData(data,nbytes);
                if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
                    deviceTrace.print(Trace.SUPPORT,
                        "pv %s interrupt and record not processed",fullName);
                }
            } finally {
                pvRecord.unlock();
            }
        }
    }
    
    private void putData(byte[] data, int nbytes) {
        if(valueIsArray) {
            convert.fromByteArray((PVArray)valuePVField, 0, nbytes, data, 0);
        } else {
            for(int i=0; i<nbytes; i++) charArray[i] = (char)data[i];
            String string = String.copyValueOf(charArray, 0, nbytes);
            convert.fromString((PVScalar)valuePVField, string);
        }
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.print(Trace.SUPPORT,"pv %s putData ",fullName);
        }
    }
}
