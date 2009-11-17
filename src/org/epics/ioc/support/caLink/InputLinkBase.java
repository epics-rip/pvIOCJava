/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ca.client.ChannelGet;
import org.epics.ca.client.ChannelGetRequester;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.LocateSupport;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.misc.BitSet;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Status;

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
     * @param pvField The field being supported.
     */
    public InputLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    
   
    private PVBoolean processPV = null;  
    private boolean process = false;
     
    private boolean isReady = false;
    private ChannelGet channelGet = null;
   
    private BitSet bitSet = null;
    
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult;   
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {   
        super.initialize(recordSupport);
        if(super.getSupportState()!=SupportState.readyForStart) return;
        processPV = pvStructure.getBooleanField("process");
        if(processPV==null) {
            uninitialize();
            return;
        }
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(super.getSupportState()!=SupportState.ready) return;
        process = processPV.get();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            if(channelGet==null) {
                channel.createChannelGet(this, pvRequest, false, process,null);
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
     * @see org.epics.ca.client.ChannelGetRequester#channelGetConnect(Status, org.epics.ca.client.ChannelGet, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
     */
    @Override
    public void channelGetConnect(Status status, ChannelGet channelGet,PVStructure pvStructure, BitSet bitSet) {
        pvRecord.lock();
        try {
            if(!status.isSuccess()) {
                message("createChannelGet failed " + status.getMessage(),MessageType.error);
                return;
            }
            if(!super.setLinkPVStructure(pvStructure)) {
                channelGet.destroy();
                return;
            }
            this.channelGet = channelGet;
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
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
        return;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessCallbackRequester#processCallback()
     */
    @Override
    public void processCallback() {
        channelGet.get(false);
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.ChannelGetRequester#getDone(Status)
     */
    @Override
    public void getDone(Status success) {
        requestResult = (success.isOK() ? RequestResult.success : RequestResult.failure);
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        boolean allSet = bitSet.get(0);
        for(int i=0; i< linkPVFields.length; i++) {
            if(i==indexAlarmLinkField) {
                alarmSupport.setAlarm(pvAlarmMessage.get(),
                    AlarmSeverity.getSeverity(pvAlarmSeverityIndex.get()));
            } else if(allSet){
                convert.copy(linkPVFields[i],pvFields[i]);
            } else {
                copyChanged(linkPVFields[i],pvFields[i]);
            }
        }
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
