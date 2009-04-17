/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.dbLink;


import org.epics.ioc.install.AfterStart;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVField;
/**
 * Implementation for a channel access output link.
 * @author mrk
 *
 */
public class ProcessLinkBase extends AbstractLink implements ProcessCallbackRequester,ProcessContinueRequester, RecordProcessRequester
{
    private boolean isRecordProcessRequester = false;
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult = RequestResult.success;
    private String alarmMessage = null;
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvField The pvField being supported.
     */
    public ProcessLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.dbLink.AbstractLinkSupport#start()
     */
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,null)) return;
        isRecordProcessRequester = linkRecordProcess.setRecordProcessRequester(this);
        if(!isRecordProcessRequester && !linkRecordProcess.canProcessSelf()) {
            super.message(
                "already has process requester other than self", MessageType.error);
            super.stop();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.dbLink.AbstractLinkSupport#stop()
     */
    public void stop() {
        if(isRecordProcessRequester) {
            linkRecordProcess.releaseRecordProcessRequester(this);
            isRecordProcessRequester = false;
        }
        super.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.support.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        this.supportProcessRequester = supportProcessRequester;
        requestResult = RequestResult.success;
        alarmMessage = null;
        recordProcess.requestProcessCallback(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessCallbackRequester#processCallback()
     */
    public void processCallback() {
        if(isRecordProcessRequester) {
            if(linkRecordProcess.process(this, false, super.timeStamp)) return;
        } else if(linkRecordProcess.processSelfRequest(this)) {
            if(linkRecordProcess.processSelfProcess(this, false)) return;
        }
        alarmMessage = "could not process record";
        requestResult = RequestResult.failure;
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        if(alarmMessage!=null) {
            alarmSupport.setAlarm(alarmMessage, AlarmSeverity.minor);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
     */
    public void recordProcessComplete() {
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
     */
    public void recordProcessResult(RequestResult requestResult) {
        this.requestResult = requestResult;
    }
    
}
