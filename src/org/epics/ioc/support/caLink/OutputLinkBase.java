/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ca.channelAccess.client.ChannelPut;
import org.epics.ca.channelAccess.client.ChannelPutRequester;
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
    
    private PVBoolean processAccess = null;
    private boolean process = false;
    
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult = null;   
    
    private boolean isReady = false;
    private ChannelPut channelPut = null;
    protected PVStructure linkPVStructure = null;
    protected PVField linkValuePVField = null;
    protected BitSet bitSet = null;
    
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(LocateSupport recordSupport) {
        super.initialize(recordSupport);
        if(super.getSupportState()!=SupportState.readyForStart) return;
        processAccess = pvStructure.getBooleanField("process");
        if(processAccess==null) {
            uninitialize();
            return;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(super.getSupportState()!=SupportState.ready) return;
        process = processAccess.get();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            if(channelPut==null) {
                channel.createChannelPut(this, pvRequest, "put",false, process,null);
            }
        } else {
            pvRecord.lock();
            try {
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
            if(channelPut!=null) channelPut.destroy();
            channelPut = null;
            linkPVStructure = null;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.ChannelPutRequester#channelPutConnect(Status,org.epics.ca.channelAccess.client.ChannelPut, org.epics.pvData.pv.PVStructure, org.epics.pvData.misc.BitSet)
     */
    @Override
    public void channelPutConnect(Status status, ChannelPut channelPut,PVStructure pvStructure, BitSet bitSet) {
    	// TODO status
        this.channelPut = channelPut;
        linkPVStructure = pvStructure;
        this.bitSet = bitSet;
        boolean isCompatible = false;
        if(valueIsEnumerated) {
            linkValuePVField = linkPVStructure.getSubField("value.index");
            isCompatible = convert.isCopyCompatible(valueIndexPV.getField(), linkValuePVField.getField());
        } else {
            linkValuePVField = linkPVStructure.getSubField("value");
            isCompatible = convert.isCopyCompatible(valuePVField.getField(), linkValuePVField.getField());
        }
        pvRecord.lock();
        try {
            if(isCompatible) {
                isReady = true;
            } else {
                message("link type is not compatible with pvname " + pvnamePV.get(),MessageType.error);
            }
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
        if(valueIsEnumerated) {
            convert.copy(valueIndexPV, linkValuePVField);
        } else {
            convert.copy(valuePVField, linkValuePVField);
        }
        bitSet.set(linkValuePVField.getFieldOffset());
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
     * @see org.epics.ca.channelAccess.client.ChannelPutRequester#getDone(Status)
     */
    @Override
    public void getDone(Status success) {/*nothing to do*/}
    /* (non-Javadoc)
     * @see org.epics.ca.channelAccess.client.ChannelPutRequester#putDone(Status)
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
