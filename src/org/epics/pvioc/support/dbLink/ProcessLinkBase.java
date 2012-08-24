/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.dbLink;

import org.epics.pvdata.property.AlarmSeverity;
import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.support.ProcessCallbackRequester;
import org.epics.pvioc.support.ProcessContinueRequester;
import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcessRequester;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.RequestResult;
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
    private TimeStamp timeStamp = TimeStampFactory.create();
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvRecordField The field being supported.
     */
    public ProcessLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.dbLink.AbstractLinkSupport#start()
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
     * @see org.epics.pvioc.support.dbLink.AbstractLinkSupport#stop()
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
     * @see org.epics.pvioc.support.AbstractSupport#process(org.epics.pvioc.support.SupportProcessRequester)
     */
    @Override
    public void process(SupportProcessRequester supportProcessRequester) {
        this.supportProcessRequester = supportProcessRequester;
        requestResult = RequestResult.success;
        alarmMessage = null;
        recordProcess.requestProcessCallback(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ProcessCallbackRequester#processCallback()
     */
    public void processCallback() {
    	linkRecordProcess.queueProcessRequest(processToken);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ProcessContinueRequester#processContinue()
     */
    public void processContinue() {
        if(alarmMessage!=null) {
            alarmSupport.setAlarm(alarmMessage, AlarmSeverity.MINOR,AlarmStatus.DB);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
     */
    public void recordProcessComplete() {
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessResult(org.epics.pvioc.util.RequestResult)
     */
    public void recordProcessResult(RequestResult requestResult) {
        this.requestResult = requestResult;
    }
  	/* (non-Javadoc)
  	 * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
  	 */
  	@Override
	public void becomeProcessor() {
  	  if(pvTimeStamp.isAttached()) {
          pvTimeStamp.get(timeStamp);
          linkRecordProcess.process(processToken,false,timeStamp);
      } else {
          linkRecordProcess.process(processToken,false);
      }
	}
	/* (non-Javadoc)
	 * @see org.epics.pvioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
	 */
	@Override
	public void canNotProcess(String reason) {
		this.requestResult = RequestResult.failure;
		alarmMessage = "could not process record";
        recordProcess.processContinue(this);
	}
	/* (non-Javadoc)
	 * @see org.epics.pvioc.support.RecordProcessRequester#lostRightToProcess()
	 */
	@Override
	public void lostRightToProcess() {
		processToken = null;
		super.message(
                "can not process record", MessageType.error);
        super.stop();
	}
    
}
