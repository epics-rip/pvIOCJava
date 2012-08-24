/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv.serial;

import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.ScalarArray;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.pdrv.Trace;
import org.epics.pvioc.pdrv.interfaces.Interface;
import org.epics.pvioc.pdrv.interfaces.Serial;
import org.epics.pvioc.pdrv.interfaces.SerialInterruptListener;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink;

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
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseSerialInterrupt(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }
    
    private boolean valueIsArray = false;
    private PVInt pvSize = null;
    private int size = 0;
    
    private Serial octet = null;
    private char[] charArray = null;
    private byte[] data = null;
    private int nbytes = 0;
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.pdrv.AbstractPortDriverInterruptLink#initialize()
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
     * @see org.epics.pvioc.pdrv.support.AbstractPDRVLinkSupport#start()
     */
    @Override
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
     * @see org.epics.pvioc.pdrv.support.AbstractPDRVLinkSupport#stop()
     */
    @Override
    public void stop() {
        super.stop();
        octet.removeInterruptUser(user, this);
        charArray = null;
    }  
    /* (non-Javadoc)
     * @see org.epics.pvioc.pdrv.interfaces.SerialInterruptListener#interrupt(byte[], int)
     */
    @Override
    public void interrupt(byte[] data, int nbytes) {
    	this.data = data;
    	this.nbytes = nbytes;
    	if(isProcessor) {
    		recordProcess.queueProcessRequest(processToken);
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
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
    public void becomeProcessor() {
    	putData(data,nbytes);
    	recordProcess.process(processToken,false);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
     */
    @Override
    public void canNotProcess(String reason) {
    	pvRecord.lock();
    	try {
    		putData(data,nbytes);
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
    
    private void putData(byte[] data, int nbytes) {
        if(valueIsArray) {
            convert.fromByteArray((PVScalarArray)valuePVField, 0, nbytes, data, 0);
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
