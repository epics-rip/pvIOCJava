/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;

/**
 * Record processing support.
 * @author mrk
 *
 */
public interface RecordProcess {
    /**
     * lock for reading.
     * Rules are not yet decided.
     */
    void readLock();
    /**
     * unlock for reading.
     * Rules are not yet decided.
     */
    void readUnlock();
    /**
     * lock for writing,
     * Rules are not yet decided.
     */
    void writeLock();
    /**
     * unlock for writing.
     * Rules are not yet decided.
     */
    void writeUnlock();
    /**
     * is the record disabled.
     * A process request while a record is disabled returns a noop.
     * @return (false,true) if the record (is not, is) disabled
     */
    boolean isDisabled();
    /**
     * set the disabled state to the requested value.
     * @param value true or false.
     * @return (false,true) if the state (was not, was) changed.
     */
    boolean setDisabled(boolean value);
    /**
     * is the record active.
     * @return (false,true) if the record (is not, is) active.
     */
    boolean isActive();
    /**
     * get the record this RecordProcess processes.
     * @return the DBRecord interface.
     */
    DBRecord getRecord();
    /**
     * get the record support for this record instance.
     * @return rge record support.
     */
    RecordSupport getRecordSupport();
    /**
     * set the record support for this record instance.
     * @param support the support,
     * @return (false,true) if the support (was not, was) set.
     * A false return means a record support module was already registered,
     */
    boolean setRecordSupport(RecordSupport support);
    /**
     * add a listener for record completion.
     * @param listener the listener.
     * @return (false,true) if the listener (was not, was) added to listener list.
     * A value of false means the listener was already in the list.
     */
    boolean addCompletionListener(ProcessComplete listener);
    /**
     * remove a completion listener.
     * @param listener the listener.
     */
    void removeCompletionListener(ProcessComplete listener);
    
    /**
     * request record processing.
     * @param listener if the return value is active this is the luistener that will be
     * called when the record completes processing.
     * @return the result of the process request.
     */
    ProcessReturn requestProcess(ProcessComplete listener);
    /**
     * called by record support to signify completion.
     * If the record support returns active than the listener must expect additional calls.
     * @param result the reason for calling. A value of active is permissible.
     * In this case record support will again call recordSupportDone.
     */
    void recordSupportDone(ProcessReturn result);
    /**
     * called by record/link support to request that a linked record be processed.
     * If the call succeeds the record is marked active but not processed.
     * The process request will be made when then record/link support returns.
     * @param linkedRecord the record to process.
     * @param listener the listener that is passed to the process request for the linked record.
     * @return (false,true> if the request is successfull. A return value of false means the record was already active.
     */
    boolean requestProcess(
        RecordProcess linkedRecord, ProcessComplete listener);
}
