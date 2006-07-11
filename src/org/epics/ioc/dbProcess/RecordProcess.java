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
     * request for permission to call process.
     * @param listener a listenr to call if the record is already active.
     * @return the result of the request.
     */
    RequestProcessReturn requestProcess(ProcessComplete listener);
    /**
     * Process the record instance.
     * The caller must be the owner as a result of requestProcess returning RequestProcessReturn.success.
     * @param listener if the return value is active
     * this is the listener that will be called when the record completes processing.
     * @return the result of the process request.
     */
    ProcessReturn process(ProcessComplete listener);
    /**
     * remove a completion listener.
     * @param listener the listener.
     */
    void removeCompletionListener(ProcessComplete listener);
    /**
     * Request to process a linked record.
     * This is called by record or link support while processing a record.
     * The linked record will not be processed synchronously.
     * Instead RecordProcess will process the record after record support returns.
     * @param record The linked record.
     * @param listener The listener that will be called when the linked record completes processing.
     * A null value is permissible.
     * @return the result of the request.
     */
    RequestProcessReturn requestProcessLinkedRecord(DBRecord record,ProcessComplete listener);
    /**
     * remove a completion listener for a linked record.
     * @param listener the listener.
     */
    void removeLinkedCompletionListener(ProcessComplete listener);
    /**
     * called by record support to signify completion.
     * If the record support returns active than the listener must expect additional calls.
     * @param result the reason for calling. A value of active is permissible.
     * In this case record support will again call recordSupportDone.
     */
    void recordSupportDone(ProcessReturn result);
    /**
     * set the status and severity for the record.
     * the algorithm is to maxamize the severity, i.e. if the requested severity is greater than the current
     * severity than the status and severity are set to the requested values. When a recvord starts processing the
     * status is set to null and the alarmSeverity is set the "not defined". This the first call with a severity of
     * none will set the status and severity.
     * @param status the status
     * @param alarmSeverity the severity
     * @return (false, true) if the status and severity (were not, were) set the requested values.
     */
    boolean setStatusSeverity(String status, AlarmSeverity alarmSeverity);
}
