/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.caLink;

import org.epics.ca.client.ChannelProcess;
import org.epics.ca.client.ChannelProcessRequester;
import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.property.AlarmStatus;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.Status;
/**
 * Implementation for a channel access output link.
 * @author mrk
 *
 */
public class ProcessLinkBase extends AbstractLink
implements ProcessCallbackRequester,ProcessContinueRequester, ChannelProcessRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvRecordField The field being supported.
     */
    public ProcessLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    
    private volatile boolean isReady = false;
    private ChannelProcess channelProcess = null;
    private SupportProcessRequester supportProcessRequester = null;
    private boolean success = true;

    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            if(channelProcess==null) {
                channelProcess = channel.createChannelProcess(this,null);
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
     * @see org.epics.ca.client.ChannelProcessRequester#channelProcessConnect(Status,org.epics.ca.client.ChannelProcess)
     */
    @Override
    public void channelProcessConnect(Status status,ChannelProcess channelProcess) {
        if(!status.isSuccess()) {
            message("createChannelProcess failed " + status.getMessage(),MessageType.error);
            try {
                this.channelProcess = channelProcess;
                channelProcess = null;
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
        } else {
            pvRecord.lock();
            try {
                this.channelProcess = channelProcess;
                isReady = true;
            } finally {
                pvRecord.unlock();
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#stop()
     */
    public void stop() {
        channelProcess.destroy();
        channelProcess = null;
        super.stop();
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        if(!isReady) {
            if(alarmSupport!=null) alarmSupport.setAlarm(
                    pvRecordField.getFullFieldName() + " not connected",
                    AlarmSeverity.MAJOR,AlarmStatus.DB);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessCallbackRequester#processCallback()
     */
    @Override
    public void processCallback() {
        channelProcess.process(false);
    }
    /* (non-Javadoc)
     * @see org.epics.ca.client.ChannelProcessRequester#processDone(boolean)
     */
    @Override
    public void processDone(Status success) {
        this.success = success.isOK();
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        supportProcessRequester.supportProcessDone((success ? RequestResult.success : RequestResult.failure));
    }        
}
