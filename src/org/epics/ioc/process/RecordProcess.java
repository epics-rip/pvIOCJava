/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import org.epics.ioc.db.*;
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
     * Can the record process itself?
     * @return (false,true) if the record (can not, can) process itself.
     */
    boolean canProcessSelf();
    /**
     * Request that record process itself.
     * This will only be successful of scan.selfScan is true and the record is not active.
     * @return (false,true) if the record started processing.
     */
    boolean processSelf();
    /**
     * Prepare for processing a record but do not call record support.
     * A typical use of this method is when the processor wants to modify fields
     * of the record before it is processed.
     * If successful the record is active but unlocked.
     * If not successful recordProcessRequestor.recordProcessResult
     * and recordProcessComplete are called before setActive returns.
     * @param recordProcessRequestor The recordProcessRequestor.
     */
    void setActive(RecordProcessRequestor recordProcessRequestor);
    /**
     * Process the record instance.
     * Unless the record was activated by setActive,
     * the record is prepared for processing just like for setActive.
     * All results of record processing are reported
     * via the RecordProcessRequestor methods.
     * If not successful recordProcessRequestor.recordProcessResult
     * and recordProcessComplete are called before setActive returns.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @param leaveActive Leave the record active when process is done.
     * The requestor must call setInactive.
     * @param timeStamp The initial timeStamp for record procsssing.
     * If null the initial timeStamp will be the current time.
     */
    void process(RecordProcessRequestor recordProcessRequestor,
        boolean leaveActive, TimeStamp timeStamp);
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
