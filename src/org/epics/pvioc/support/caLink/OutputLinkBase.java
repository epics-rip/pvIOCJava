/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.caLink;

import org.epics.pvaccess.client.ChannelPut;
import org.epics.pvaccess.client.ChannelPutRequester;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.support.ProcessCallbackRequester;
import org.epics.pvioc.support.ProcessContinueRequester;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.util.RequestResult;


/**
 * Implementation for a channel access output link.
 * @author mrk
 *
 */
public class OutputLinkBase extends AbstractIOLink
implements ProcessCallbackRequester,ChannelPutRequester,ProcessContinueRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvRecordField The field being supported.
     */
    public OutputLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult = null;   
    private boolean isReady = false;
    private ChannelPut channelPut = null;
    protected BitSet bitSet = null;
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            if(channelPut==null) {
                channel.createChannelPut(this, pvRequest);
            } else {
                pvRecord.lock();
                try {
                    isReady = true;
                } finally {
                    pvRecord.unlock();
                }
            }
        } else {
            pvRecord.lock();
            try {
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvaccess.client.ChannelPutRequester#channelPutConnect(Status,org.epics.pvaccess.client.ChannelPut, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
     */
    @Override
    public void channelPutConnect(Status status, ChannelPut channelPut,PVStructure pvStructure, BitSet bitSet) {
        pvRecord.lock();
        try {
            if(!status.isSuccess()) {
                message("createChannelPut failed " + status.getMessage(),MessageType.error);
                return;
            }
            if(!super.setLinkPVStructure(pvStructure)) {
                channelPut.destroy();
                return;
            }
            this.channelPut = channelPut;
            this.bitSet = bitSet;
            isReady = true;
        } finally {
            pvRecord.unlock();
        }
    }
    
   
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.AbstractSupport#process(org.epics.pvioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!isReady) {
            alarmSupport.setAlarm(
                    pvRecordField.getFullFieldName() + " not connected",
                    AlarmSeverity.MAJOR,AlarmStatus.DB);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        for(int i=0; i< linkPVFields.length; i++) {
            if(i==indexAlarmLinkField) continue;
            PVField pvFrom = pvFields[i];
            PVField pvTo = linkPVFields[i];
            if(pvFrom.equals(pvTo)) continue;
            convert.copy(pvFrom, pvTo);
            bitSet.set(pvTo.getFieldOffset());
        }
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
        return;
    }       
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ProcessCallbackRequester#processCallback()
     */
    @Override
    public void processCallback() {
        channelPut.put(false);  
    }
    /* (non-Javadoc)
     * @see org.epics.pvaccess.client.ChannelPutRequester#getDone(Status)
     */
    @Override
    public void getDone(Status success) {/*nothing to do*/}
    /* (non-Javadoc)
     * @see org.epics.pvaccess.client.ChannelPutRequester#putDone(Status)
     */
    @Override
    public void putDone(Status success) {
        requestResult = (success.isOK() ? RequestResult.success : RequestResult.failure);
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        if(requestResult!=RequestResult.success) {
            alarmSupport.setAlarm(
                    pvRecordField.getFullFieldName() + ": put request failed",
                    AlarmSeverity.MAJOR,AlarmStatus.DB);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }        
}
