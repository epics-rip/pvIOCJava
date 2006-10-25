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
     * Process the record instance.
     * If the record is already active the request will fail.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @return The result of the process request.
     */
    RequestResult process(RecordProcessRequestor recordProcessRequestor); 
    /**
     * Process the record instance with the caller supplying the time stamp.
     * If the record is already active the request will fail.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @param timeStamp The initial timeStamp for the record.
     * @return The result of the process request.
     */
    RequestResult process(RecordProcessRequestor recordProcessRequestor,
        TimeStamp timeStamp);
    /**
     * Request that the record become active but wait for the caller
     * to request that the record support be called.
     * If the record is already active the request will fail.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @param supportPreProcessRequestor TODO
     * @return The result of the preProcess request.
     */
    RequestResult preProcess(RecordProcessRequestor recordProcessRequestor,
        SupportPreProcessRequestor supportPreProcessRequestor);
    /**
     * Request that the record become active but wait for the caller
     * to request that the record support be called.
     * The caller provides the initial timeStamp.
     * If the record is already active the request will fail.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @param supportPreProcessRequestor TODO
     * @return The result of the preProcess request.
     */
    RequestResult preProcess(RecordProcessRequestor recordProcessRequestor,
        SupportPreProcessRequestor supportPreProcessRequestor, TimeStamp timeStamp);
    /**
     * Called by recordProcessRequestor when a preProcess request was issued.
     * This is called with the record locked.
     * @param recordProcessRequestor The recordProcessRequestor.
     * @return The result of the processNow request.
     */
    RequestResult processNow(RecordProcessRequestor recordProcessRequestor);
    /**
     * Get RecordProcessSupport interface.
     * RecordProcessSupport is normally only used by support code.
     * @return  The interface for RecordProcessSupport.
     */
    RecordProcessSupport getRecordProcessSupport();
    /**
     * Set process trace.
     * If true a message will displayed whenever process, requestProcessCallback, or processContinue are called.
     * @param value true or false.
     * @return (false,true) if the state (was not, was) changed.
     */
    boolean setTrace(boolean value);
}
