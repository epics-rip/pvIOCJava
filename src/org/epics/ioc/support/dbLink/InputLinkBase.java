/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.dbLink;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.ProcessToken;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.alarm.AlarmSupport;
import org.epics.ioc.support.alarm.AlarmSupportFactory;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.Alarm;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVBoolean;
import org.epics.pvData.pv.PVField;

/**
 * Implementation for a channel access input link.
 * @author mrk
 *
 */
public class InputLinkBase extends AbstractIOLink
implements ProcessCallbackRequester, ProcessContinueRequester, RecordProcessRequester
{
    private boolean process = false;
    private ProcessToken processToken = null;
    private SupportProcessRequester supportProcessRequester = null;
    private RequestResult requestResult = RequestResult.success;
    private String alarmMessage = null;
    private PVBoolean pvInheritSeverity = null;
    private Alarm linkAlarm = null;
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvField The pvField being supported.
     */
    public InputLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.dbLink.AbstractIOLink#start()
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
        pvInheritSeverity = pvDatabaseLink.getBooleanField("inheritSeverity");
        if(pvInheritSeverity==null) {
            super.message(
                    "inheritSeverity not found", MessageType.error);
            super.stop();
        }
        PVField pvField = linkPVRecord.getSubField("alarm");
        if(pvField!=null) {
            
            AlarmSupport alarmSupport = AlarmSupportFactory.findAlarmSupport(
                    pvField,linkRecordLocateSupport);
            if(alarmSupport==null || alarmSupport.getPVField()!=pvField) {
                super.message("illegal alarm field", MessageType.error);
                super.uninitialize();
                return;
            }
            linkAlarm = alarmSupport.getAlarm();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
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
        if(!process) {
            getData();
            supportProcessRequester.supportProcessDone(RequestResult.success);
            return;
        }
        alarmMessage = null;
        this.supportProcessRequester = supportProcessRequester;
        recordProcess.requestProcessCallback(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessCallbackRequester#processCallback()
     */
    @Override
    public void processCallback() {
    	linkRecordProcess.queueProcessRequest(processToken);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessContinueRequester#processContinue()
     */
    @Override
    public void processContinue() {
        getData();
        if(alarmMessage!=null) {
            alarmSupport.setAlarm(alarmMessage, AlarmSeverity.minor);
        }
        supportProcessRequester.supportProcessDone(requestResult);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
     */
    @Override
    public void recordProcessComplete() {
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
     */
    @Override
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
	
    private void getData() {
        pvRecord.lockOtherRecord(linkPVRecord);
        try {
            switch(valueType) {
            case scalar:
                convert.copyScalar(linkValuePVScalar, valuePVScalar);
                break;
            case scalarArray:
                convert.copyArray(linkValuePVArray, 0, valuePVArray, 0,linkValuePVArray.getLength() );
                break;
            case structure:
                convert.copyStructure(linkValuePVStructure,valuePVStructure );
                break;
            }
            if(pvInheritSeverity.get() && linkAlarm!=null) {
                int ind = linkAlarm.getAlarmSeverityIndex().get();
                if(ind!=0) {
                    alarmSupport.setAlarm(linkAlarm.getAlarmMessage().get(), AlarmSeverity.getSeverity(ind));
                }
            }
        } finally {
            linkPVRecord.unlock();
        }
    }
}
