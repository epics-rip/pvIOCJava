/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.dbLink;

import org.epics.ioc.support.ProcessCallbackRequester;
import org.epics.ioc.support.ProcessContinueRequester;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.Alarm;
import org.epics.pvData.property.AlarmFactory;
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
    private boolean isRecordProcessRequester = false;
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
    public void start() {
        super.start();
        if(!super.checkSupportState(SupportState.ready,null)) return;
        process = pvProcess.get();
        if(process) {
            isRecordProcessRequester = linkRecordProcess.setRecordProcessRequester(this);
            if(!isRecordProcessRequester && !linkRecordProcess.canProcessSelf()) {
                super.message(
                        "already has process requester other than self", MessageType.error);
                super.stop();
            }
        }
        pvInheritSeverity = pvDatabaseLink.getBooleanField("inheritSeverity");
        if(pvInheritSeverity==null) {
            super.message(
                    "inheritSeverity no found", MessageType.error);
            super.stop();
        }
        PVField pvField = linkPVRecord.getSubField("alarm");
        if(pvField!=null) {
            linkAlarm = AlarmFactory.getAlarm(pvField);
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.AbstractSupport#stop()
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
    public void processCallback() {
        if(isRecordProcessRequester) {
            if(linkRecordProcess.process(this, true, super.timeStamp)) return;
        } else if(linkRecordProcess.processSelfRequest(this)) {
            if(linkRecordProcess.processSelfProcess(this, true)) return;
        }
        alarmMessage = "could not process record";
        requestResult = RequestResult.failure;
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ProcessContinueRequester#processContinue()
     */
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
    public void recordProcessComplete() {
        recordProcess.processContinue(this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
     */
    public void recordProcessResult(RequestResult requestResult) {
        this.requestResult = requestResult;
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
