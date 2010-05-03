/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ca.client.ChannelPut;
import org.epics.ca.client.ChannelPutRequester;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;


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
     * @param pvField The field being supported.
     */
    public OutputLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult = null;   
    private boolean isReady = false;
    private ChannelPut channelPut = null;
    protected BitSet bitSet = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
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
     * @see org.epics.ca.client.ChannelPutRequester#channelPutConnect(Status,org.epics.ca.client.ChannelPut, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
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
     * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!isReady) {
            alarmSupport.setAlarm(
                    pvStructure.getFullFieldName() + " not connected",
                    AlarmSeverity.major);
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
     * @see org.epics.ioc.support.ProcessCallbackRequester#processCallback()
     */
    @Override
    public void processCallback() {
        channelPut.put(false);  
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.ChannelPutRequester#getDone(Status)
     */
    @Override
    public void getDone(Status success) {/*nothing to do*/}
    /* (non-Javadoc)
     * @see org.epics.ca.client.ChannelPutRequester#putDone(Status)
     */
    @Override
    public void putDone(Status success) {
        requestResult = (success.isOK() ? RequestResult.success : RequestResult.failure);
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        if(requestResult!=RequestResult.success) {
            alarmSupport.setAlarm(
                    pvStructure.getFullFieldName() + ": put request failed",
                    AlarmSeverity.major);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }        
}
