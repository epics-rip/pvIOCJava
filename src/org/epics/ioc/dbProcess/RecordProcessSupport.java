/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.util.*;

/**
 * Support for record processing.
 * This code is called by record support.
 * @author mrk
 *
 */
public interface RecordProcessSupport extends SupportProcessRequestor  {
    /**
     * Ask recordProcess to call the support to continue processing.
     * This is called with no locks held.
     * Only valid if the record is active.
     * @param processContinueListener The support to call.
     */
    void processContinue(ProcessContinueListener processContinueListener);
    /**
     * Called by SupportPreProcessRequestor when a preProcess request was issued.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @return The result of the processNow request.
     */
    RequestResult processNow(RecordProcessRequestor recordProcessRequestor);
    /**
     * Request to be called back after process or processContinue
     * has called record support but before it returns.
     * This must only be called by code running as a result of Support.process or Support.processContinue. 
     * The callback will be called with the record unlocked.
     * @param processCallbackListener The listener to call.
     */
    void requestProcessCallback(ProcessCallbackListener processCallbackListener);
    /**
     * Set the status and severity for the record.
     * This must only be called by code running as a result of Support.process or Support.processContinue. 
     * The algorithm is to maxamize the severity, i.e. if the requested severity is greater than the current
     * severity than the status and severity are set to the requested values. When a recvord starts processing the
     * status is set to null and the alarmSeverity is set the "not defined". This the first call with a severity of
     * none will set the status and severity.
     * @param status The status
     * @param alarmSeverity The severity
     * @return (false, true) if the status and severity (were not, were) set the requested values.
     */
    boolean setStatusSeverity(String status, AlarmSeverity alarmSeverity);
    /**
     * Set the status for the record.
     * The status will be changed only is the current alarmSeverity is none.
     * @param status The new status.
     * @return (false,true) if the status (was not, was) changed.
     */
    boolean setStatus(String status);
    /**
     * Get the current status.
     * This must only be called by code running as a result of Support.process, Support.processContinue,
     * or RecordProcessRequestor.processComplete.
     * @return The status. If the record does not have a status field null will be returned.
     */
    String getStatus();
    /**
     * Get the current alarm severity.
     * This must only be called by code running as a result of Support.process, Support.processContinue,
     * or RecordProcessRequestor.processComplete. 
     * @return The severity. If the record does not have an alarmSeverity field null is returned.
     */
    AlarmSeverity getAlarmSeverity();
    /**
     * Set the timeStamp for the record.
     * This must only be called by code running as a result of Support.process or Support.processContinue. 
     * @param timeStamp The timeStamp.
     */
    void setTimeStamp(TimeStamp timeStamp);
    /**
     * Get the current timeStamp.
     * This must only be called by code running as a result of Support.process, Support.processContinue,
     * or RecordProcessRequestor.processComplete. 
     * @param timeStamp The current timeStamp.
     */
    void getTimeStamp(TimeStamp timeStamp);
}
