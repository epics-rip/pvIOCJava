/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.install.AfterStart;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.pv.PVRecord;

/**
 * Record processing support.
 * 
 * @author mrk
 *
 */
public interface RecordProcess {
    /**
     * Is the record enabled.
     * @return (false,true) if the record (is not, is) enabled
     */
    boolean isEnabled();
    /**
     * Set the enable state to the requested value.
     * @param value true or false.
     * @return (false,true) if the state (was not, was) changed.
     */
    boolean setEnabled(boolean value);
    /**
     * Is the record active.
     * @return (false,true) if the record (is not, is) active.
     */
    boolean isActive();
    /**
     * Get the record this RecordProcess processes.
     * @return The PVRecord interface.
     */
    PVRecord getRecord();
    /**
     * Is trace active for this record.
     * @return (false,true) if trace (is not, is) active
     */
    boolean isTrace();
    /**
     * Set trace true.
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
     * JavaIOC.
     * This must be called rather than directly calling record support.
     * This handles global fields like scan and then calls record support.
     * @param afterStart interface for being called after all support has started.
     */
    void start(AfterStart afterStart);
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
     * @param recordProcessRequester The interface implemented by the record processor.
     * @return (false,true) if the caller (is not, is) has become the record processor.
     */
    boolean setRecordProcessRequester(RecordProcessRequester recordProcessRequester);
    /**
     * Release the current record processor.
     * @param recordProcessRequester The current record processor.
     * @return (false,true) if the caller (is not, is) has been released as the record processor.
     * This should only fail if the caller was not the record processor.
     */
    boolean releaseRecordProcessRequester(RecordProcessRequester recordProcessRequester);
    /**
     * Release the record processor unconditionally.
     * This should only be used if a record processor failed without calling releaseRecordProcessor.
     */
    void releaseRecordProcessRequester();
    /**
     * Get the name of the current record processor.
     * @return The name of the current record processor or null if no record processor is registered.
     */
    String getRecordProcessRequesterName();
    /**
     * Prepare for processing a record but do not call record support.
     * A typical use of this method is when the processor wants to modify fields
     * of the record before it is processed.
     * If successful the record is active but unlocked.
     * @param recordProcessRequester The recordProcessRequester.
     * @return (false,true) if the request was successful.
     * If false is returned then recordProcessRequester.message is called to report
     * the reason.
     * @throws IllegalStateException if recordProcessRequester is null.
     */
    boolean setActive(RecordProcessRequester recordProcessRequester);
    /**
     * Process the record instance.
     * Unless the record was activated by setActive,
     * the record is prepared for processing just like for setActive.
     * All results of record processing are reported
     * via the RecordProcessRequester methods.
     * @param recordProcessRequester The recordProcessRequester.
     * @param leaveActive Leave the record active when process is done.
     * The requester must call setInactive.
     * @param timeStamp The initial timeStamp for record processing.
     * If null the initial timeStamp will be the current time.
     * @return (false,true) if the request was successful.
     * If false is returned then recordProcessRequester.message is called to report
     * the reason.
     * @throws IllegalStateException if recordProcessRequester is null.
     */
    boolean process(RecordProcessRequester recordProcessRequester,
        boolean leaveActive, TimeStamp timeStamp);
    /**
     * Called by the recordProcessRequester when it has called process with leaveActive
     * true and is done.
     * @param recordProcessRequester
     */
    void setInactive(RecordProcessRequester recordProcessRequester);
    /**
     * Can the record process itself?
     * @return (false,true) if the record (can not, can) process itself.
     */
    boolean canProcessSelf();
    /**
     * Request that record process itself.
     * If successful the caller is the temporary holder of the right to call processSelfSetActive,
     * processSelfProcess, and processSelfSetInactive. When record completes processing the caller will no longer have this right.
     * @param recordProcessRequester The requester to call if the request is successful.
     * @return (false,true) if the record started processing.
     */
    boolean processSelfRequest(RecordProcessRequester recordProcessRequester);
    /**
     * Set the record active.
     * Similar to setActive since the actual recordProcessor calls setActive.
     * @param recordProcessRequester The recordProcessRequester.
     * @return (false,true) if the request was successful.
     */
    boolean processSelfSetActive(RecordProcessRequester recordProcessRequester);
    /**
     * JavaIOC processing.
     * Similar to process since the actual recordProcessor calls process.
     * @param recordProcessRequester The recordProcessRequester.
     * @param leaveActive Leave the record active when process is done.
     * The requester must call setInactive.
     * @return (false,true) if the request was successful.
     */
    boolean processSelfProcess(RecordProcessRequester recordProcessRequester, boolean leaveActive);
    /**
     * Called if processSelfProcess was called with leaveActive true.
     * Similar to setInactive since the actual recordProcessor calls setInactive.
     * @param recordProcessRequester The recordProcessRequester.
     */
    void processSelfSetInactive(RecordProcessRequester recordProcessRequester);
    
    /**
     * Ask recordProcess to continue processing.
     * This must be called with the record unlocked.
     * ProcessContinueRequester.processContinue will be called with the record locked.
     * Only valid if the record is active.
     * @param processContinueRequester The requester to call.
     */
    void processContinue(ProcessContinueRequester processContinueRequester);
    /**
     * Request to be called back after process or processContinue
     * has called support but before it returns.
     * This must only be called by code implementing Support.process() or ProcessContinueRequester.processContinue(),
     * which means that it is called with the record locked.
     * ProcessCallbackRequester.processCallback() will be called with the record unlocked.
     * @param processCallbackRequester The requester to call.
     */
    void requestProcessCallback(ProcessCallbackRequester processCallbackRequester);
    /**
     * Set the timeStamp for the record.
     * This must only be called by code implementing process or processContinue. 
     * @param timeStamp The timeStamp.
     */
    void setTimeStamp(TimeStamp timeStamp);
    /**
     * Get the current timeStamp.
     * This must only be called by code implementing process or processContinue. 
     * @param timeStamp The current timeStamp.
     */
    void getTimeStamp(TimeStamp timeStamp);
}
