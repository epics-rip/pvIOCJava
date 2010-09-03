/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.database.PVRecord;
import org.epics.ioc.install.AfterStart;
import org.epics.pvData.property.TimeStamp;

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
     * Is the record active, i.e. is the record being processed.
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
     * This handles global nodes like scan and then calls record support.
     */
    void initialize();
    /**
     * JavaIOC.
     * This must be called rather than directly calling record support.
     * This handles global nodes like scan and then calls record support.
     * @param afterStart interface for being called after all support has started.
     */
    void start(AfterStart afterStart);
    /**
     * Stop.
     * This must be called rather than directly calling record support.
     * This handles global nodes like scan and then calls record support.
     * If the record is active when stop is called then the record will not be stopped
     * until the record completes the current process request.
     */
    void stop();
    /**
     * Uninitialize.
     * This must be called rather than directly calling record support.
     * This handles global nodes like scan and then calls record support.
     * If the record is active when uninitialize is called then the record will not be uninitialized
     * until the record completes the current process request.
     */
    void uninitialize();
    /**
     * Request a token that will allow the caller to call queueProcessRequest.
     * @param recordProcessRequester The interface implemented by the record processor.
     * @return A ProcessToken if the caller can become the record processor.
     * null is returned if the caller can not call queueProcessRequest.
     */
    ProcessToken requestProcessToken(RecordProcessRequester recordProcessRequester);
    /**
     * Release the current record processor.
     * @param processToken The token returned by setRecordProcessRequester
     */
    void releaseProcessToken(ProcessToken processToken);
    /**
     * Release the record processor unconditionally.
     * This should only be used if a record processor failed leaving the record active.
     */
    void forceInactive();
    /**
     * Get the name of the current record processor.
     * @return The name of the current record processor or null if no record processor is registered.
     */
    String getRecordProcessRequesterName();
    /**
     * queue a request to become record process requester.
     * @param processToken The token returned by requestProcessToken.
     */
    void queueProcessRequest(ProcessToken processToken);
    /**
     * Process the record instance.
     * Unless the record was activated by setActive,
     * the record is prepared for processing just like for setActive.
     * All results of record processing are reported
     * via the RecordProcessRequester methods.
     * @param processToken The token returned by requestProcessToken.
     * @param leaveActive Leave the record active when process is done.
     * The requester must call setInactive.
     * @param timeStamp The initial timeStamp for record processing.
     * If null the initial timeStamp will be the current time.
     */
    void process(ProcessToken processToken,boolean leaveActive,TimeStamp timeStamp);
    /**
     * Called by the recordProcessRequester when it has called process with leaveActive
     * true and is done.
     * @param processToken The token returned by requestProcessToken.
     */
    void setInactive(ProcessToken processToken);
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
