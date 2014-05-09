/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.caLink;

import org.epics.pvaccess.client.ChannelGet;
import org.epics.pvaccess.client.ChannelGetRequester;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.*;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Status;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.support.ProcessCallbackRequester;
import org.epics.pvioc.support.ProcessContinueRequester;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.util.RequestResult;

/**
 * Implementation for a channel access input link.
 * @author mrk
 *
 */
public class InputLinkBase extends AbstractIOLink
implements ProcessCallbackRequester,ChannelGetRequester,ProcessContinueRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvRecordField The field being supported.
     */
    public InputLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
      
    private boolean isReady = false;
    private ChannelGet channelGet = null;
   
    private PVStructure linkPVStructure = null;
    private BitSet bitSet = null;
    
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult;   
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            if(channelGet==null) {
                channel.createChannelGet(this, pvRequest);
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
     * @see org.epics.pvaccess.client.ChannelGetRequester#channelGetConnect(Status, org.epics.pvaccess.client.ChannelGet, org.epics.pvdata.pv.PVStructure, org.epics.pvdata.misc.BitSet)
     */
    @Override
    public void channelGetConnect(Status status, ChannelGet channelGet,Structure structure) {
        pvRecord.lock();
        try {
            if(!status.isSuccess()) {
                message("createChannelGet failed " + status.getMessage(),MessageType.error);
                return;
            }
            if(!super.findPVFields(structure)) {
                channelGet.destroy();
                return;
            }
            this.channelGet = channelGet;
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
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
        return;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ProcessCallbackRequester#processCallback()
     */
    @Override
    public void processCallback() {
        channelGet.get();
    }
    /* (non-Javadoc)
     * @see org.epics.pvaccess.client.ChannelGetRequester#getDone(Status)
     */
    @Override
    public void getDone(Status success, ChannelGet channelGet, PVStructure pvStructure, BitSet bitSet) {
        requestResult = (success.isOK() ? RequestResult.success : RequestResult.failure);
        this.linkPVStructure = pvStructure;
        this.bitSet = bitSet;
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        boolean allSet = bitSet.get(0);
        PVField[] linkPVFields = linkPVStructure.getPVFields();
        for(int i=0; i< linkPVFields.length; i++) {
            if(i==indexAlarmLinkField) {
                alarmSupport.setAlarm(pvAlarmMessage.get(),
                    AlarmSeverity.getSeverity(pvAlarmSeverity.get()),AlarmStatus.DB);
            } else if(allSet){
                convert.copy(linkPVFields[i],pvFields[i]);
            } else {
                copyChanged(linkPVFields[i],pvFields[i]);
            }
        }
        linkPVStructure = null;
        bitSet = null;
        supportProcessRequester.supportProcessDone(requestResult);
    }
    
    private void copyChanged(PVField pvFrom,PVField pvTo) {
        int startFrom = pvFrom.getFieldOffset();
        int startTo = pvTo.getFieldOffset();
        int nextSet = bitSet.nextSetBit(startFrom);
        if(nextSet<0) return;
        if(nextSet==startFrom) {
            convert.copy(pvFrom, pvTo);
            return;
        }
        if(pvFrom.getNumberFields()==1) return;
        while(nextSet<pvFrom.getNextFieldOffset()) {
            PVField from = ((PVStructure)pvFrom).getSubField(nextSet);
            int nextTo = nextSet - startFrom + startTo;
            PVField to = ((PVStructure)pvTo).getSubField(nextTo);
            convert.copy(from, to);
            bitSet.clear(nextSet);
            nextSet = bitSet.nextSetBit(nextSet);
            if(nextSet<0) return;
        }
    }
}
