/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.dbLink;

import org.epics.ioc.database.PVRecordField;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
/**
 * Implementation for a channel access output link.
 * @author mrk
 *
 */
public class ProcessLinkBase extends AbstractLink
implements ProcessCallbackRequester,ProcessContinueRequester,RecordProcessRequester
{
    private ProcessToken processToken = null;
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult = RequestResult.success;
    private String alarmMessage = null;
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvRecordField The field being supported.
     */
    public ProcessLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.dbLink.AbstractLinkSupport#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,null)) return;
        processToken = linkRecordProcess.requestProcessToken(this);
        if(processToken==null) {
        	super.message(
                    "can not process record", MessageType.error);
            super.stop();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.dbLink.AbstractLinkSupport#stop()
     */
    @Override
    public void stop() {
        if(processToken!=null) {
            linkRecordProcess.releaseProcessToken(processToken);
            processToken = null;
        }
        super.stop();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#process(org.epics.ioc.support.SupportProcessRequester)
     */
    @Override
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
    	linkRecordProcess.queueProcessRequest(processToken);
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
  	/* (non-Javadoc)
  	 * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
  	 */
  	@Override
	public void becomeProcessor() {
		linkRecordProcess.process(processToken,false, super.timeStamp);
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
	 */
	@Override
	public void canNotProcess(String reason) {
		this.requestResult = RequestResult.failure;
		alarmMessage = "could not process record";
        recordProcess.processContinue(this);
	}
	/* (non-Javadoc)
	 * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
	 */
	@Override
	public void lostRightToProcess() {
		processToken = null;
		super.message(
                "can not process record", MessageType.error);
        super.stop();
	}
    
}
