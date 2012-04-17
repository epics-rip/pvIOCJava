/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
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
 * Implementation for a channel access input link.
 * @author mrk
 *
 */
public class OutputLinkBase extends AbstractIOLink
implements ProcessCallbackRequester, ProcessContinueRequester, RecordProcessRequester
{
    private boolean process = false;
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
    public OutputLinkBase(String supportName,PVRecordField pvRecordField) {
        super(supportName,pvRecordField);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.dbLink.AbstractIOLink#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,null)) return;
        process = pvProcess.get();
        if(process) {
        	processToken = linkRecordProcess.requestProcessToken(this);
            if(processToken==null) {
            	super.message(
                        "can not process record", MessageType.error);
                super.stop();
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.AbstractSupport#stop()
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
        if(!process) {
            putData();
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        alarmMessage = null;
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ProcessCallbackRequester#processCallback()
     */
    @Override
    public void processCallback() {
    	linkRecordProcess.queueProcessRequest(processToken);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.ProcessContinueRequester#processContinue()
     */
    @Override
    public void processContinue() {
        if(alarmMessage!=null) {
            alarmSupport.setAlarm(alarmMessage, AlarmSeverity.MINOR,AlarmStatus.DB);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessComplete()
     */
    @Override
    public void recordProcessComplete() {
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#recordProcessResult(org.epics.pvioc.util.RequestResult)
     */
    @Override
    public void recordProcessResult(RequestResult requestResult) {
        this.requestResult = requestResult;
    }
    /* (non-Javadoc)
     * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
    public void becomeProcessor() {
    	 putData();
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

    private void putData() {
        pvRecord.lockOtherRecord(linkPVRecord);
        try {
            switch(valueType) {
            case scalar:
                convert.copyScalar(valuePVScalar,linkValuePVScalar);
                break;
            case scalarArray:
                convert.copyScalarArray(valuePVArray, 0, linkValuePVArray, 0,valuePVArray.getLength() );
                break;
            case structure:
                convert.copyStructure(valuePVStructure,linkValuePVStructure );
                break;
            }
        } finally {
            linkPVRecord.unlock();
        }
    }
}
