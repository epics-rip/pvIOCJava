/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import org.epics.ioc.ca.ChannelProcess;
import org.epics.ioc.ca.ChannelProcessRequester;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.PVStructure;
/**
 * Implementation for a channel access output link.
 * @author mrk
 *
 */
public class ProcessSupportBase extends AbstractLinkSupport
implements ProcessCallbackRequester,ProcessContinueRequester, ChannelProcessRequester
{
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvStructure The pvStructure for the field being supported.
     */
    public ProcessSupportBase(String supportName,PVStructure pvStructure) {
        super(supportName,pvStructure);
       
    }
    private ChannelProcess channelProcess = null;
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult;
    private boolean isReady = false;

    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            channelProcess = channel.createChannelProcess(this);
            pvRecord.lock();
            try {
                isReady = true;
            } finally {
                pvRecord.unlock();
            }
        } else {
            pvRecord.lock();
            try {
                isReady = false;
            } finally {
                pvRecord.unlock();
            }
            if(channelProcess!=null) channelProcess.destroy();
            channelProcess = null;
        }
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        super.start();
        super.connect();
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
                    pvStructure.getFullFieldName() + " not connected",
                    AlarmSeverity.major);
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessCallbackRequester#processCallback()
     */
    public void processCallback() {            
        channelProcess.process();
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.process.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        supportProcessRequester.supportProcessDone(requestResult);
    }        
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelProcessRequester#processDone(org.epics.ioc.util.RequestResult)
     */
    public void processDone(RequestResult requestResult) {
        this.requestResult = requestResult;
        recordProcess.processContinue(this);
        
    }
}
