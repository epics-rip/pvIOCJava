/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.util.*;;

/**
 * Record processing support.
 * 
 * @author mrk
 *
 */
public interface RecordProcess {
    /**
     * Is the record disabled.
     * A process request while a record is disabled returns a noop.
     * @return (false,true) if the record (is not, is) disabled
     */
    boolean isDisabled();
    /**
     * Set the disabled state to the requested value.
     * @param value true or false.
     * @return (false,true) if the state (was not, was) changed.
     */
    boolean setDisabled(boolean value);
    /**
     * Is the record active.
     * @return (false,true) if the record (is not, is) active.
     */
    boolean isActive();
    /**
     * Get the record this RecordProcess processes.
     * @return The DBRecord interface.
     */
    DBRecord getRecord();
    /**
     * Set process trace.
     * If true a message will displayed whenever process, requestProcessCallback, or processContinue are called.
     * @param value true or false.
     * @return (false,true) if the state (was not, was) changed.
     */
    boolean setTrace(boolean value);
    /**
     * Get the current record support state.
     * @return The state.
     */
    SupportState getSupportState();
    /**
     * Initialize.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     */
    void initialize();
    /**
     * Start.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     */
    void start();
    /**
     * Stop.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     * If the record is active when stop is called then the record will not be stopped
     * until the record completes the current process request.
     */
    void stop();
    /**
     * Uninitialize.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     * If the record is active when uninitialize is called then the record will not be uninitialized
     * until the record completes the current process request.
     */
    void uninitialize();
    /**
     * Attempt to become the record processor, i.e. the code that can call process and preProcess.
     * @param recordProcessRequestor The interface implemented by the record processor.
     * @return (false,true) if the caller (is not, is) has become the record processor.
     */
    boolean setRecordProcessRequestor(RecordProcessRequestor recordProcessRequestor);
    /**
     * Release the current record processor.
     * @param recordProcessRequestor The current record processor.
     * @return (false,true) if the caller (is not, is) has been released as the record processor.
     * This should only fail if the caller was not the record processor.
     */
    boolean releaseRecordProcessRequestor(RecordProcessRequestor recordProcessRequestor);
    /**
     * Release the record processor unconditionally.
     * This should only be used if a record processor failed without calling releaseRecordProcessor.
     */
    void releaseRecordProcessRequestor();
    /**
     * Get the name of the current record processor.
     * @return The name of the current record processor or null if no record processor is registered.
     */
    String getRecordProcessRequestorName();
    /**
     * Prepare for processing a record but do not call record support.
     * A typical use of this method is when the processor wants to modify fields
     * of the record before it is processed.
     * If successful the record is active but unlocked.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @return (false,true) if the record (is not,is) ready for processing.
     * The call can fail for a number of reasons. If false is returned the caller
     * must not modify or process the record.
     */
    boolean setActive(RecordProcessRequestor recordProcessRequestor);
    /**
     * Process the record instance.
     * Unless the record was activated by setActive,
     * the record is prepared for processing just like for setActive.
     * If the record is successfully prepared recordSupport.process is called.
     * All results of record processing are reported
     * via the RecordProcessRequestor methods.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @param leaveActive Leave the record active when process is done.
     * The requestor must call setInactive.
     * @param timeStamp The initial timeStamp for record procsssing.
     * If null the initial timeStamp will be the current time.
     * @return (false,true) if the record (is not,is) ready for processing.
     */
    boolean process(RecordProcessRequestor recordProcessRequestor, boolean leaveActive, TimeStamp timeStamp); 
    /**
     * Call by the recordProcessRequestor when it has called process with leaveActive
     * true and is done.
     * @param recordProcessRequestor
     */
    void setInactive(RecordProcessRequestor recordProcessRequestor);
    /**
     * Ask recordProcess to continue processing.
     * This is called with the record unlocked.
     * Only valid if the record is active.
     * @param processContinueRequestor The requestor to call.
     */
    void processContinue(ProcessContinueRequestor processContinueRequestor);
    /**
     * Request to be called back after process or processContinue
     * has called support but before it returns.
     * This must only be called by code running as a result of process, preProcess, or processContinue. 
     * The callback will be called with the record unlocked.
     * @param processCallbackRequestor The listener to call.
     */
    void requestProcessCallback(ProcessCallbackRequestor processCallbackRequestor);
    /**
     * Set the status and severity for the record.
     * This must only be called by code running as a result of process, preProcess, or processContinue. 
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
     * This must only be called by code running as a result of process, preProcess, or processContinue. 
     * The status will be changed only is the current alarmSeverity is none.
     * @param status The new status.
     * @return (false,true) if the status (was not, was) changed.
     */
    boolean setStatus(String status);
    /**
     * Get the current status.
     * This must only be called by code running as a result of process, preProcess, or processContinue. 
     * or RecordProcessRequestor.processComplete.
     * @return The status. If the record does not have a status field null will be returned.
     */
    String getStatus();
    /**
     * Get the current alarm severity.
     * This must only be called by code running as a result of process, preProcess, or processContinue.  
     * @return The severity. If the record does not have an alarmSeverity field null is returned.
     */
    AlarmSeverity getAlarmSeverity();
    /**
     * Set the timeStamp for the record.
     * This must only be called by code running as a result of process, preProcess, or processContinue. 
     * @param timeStamp The timeStamp.
     */
    void setTimeStamp(TimeStamp timeStamp);
    /**
     * Get the current timeStamp.
     * This must only be called by code running as a result of process, preProcess, or processContinue. 
     * @param timeStamp The current timeStamp.
     */
    void getTimeStamp(TimeStamp timeStamp);
}
