/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.util.*;

/**
 * Record processing support.
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
     * Get the record support for this record instance.
     * @return The record support.
     */
    RecordSupport getRecordSupport();
    /**
     * Set the record support for this record instance.
     * @param support The support.
     */
    void setRecordSupport(RecordSupport support);
    /**
     * Request for permission to call process.
     * @param listener A listener to call if the record is already active.
     * @return The result of the request.
     */
    RequestProcessReturn requestProcess(ProcessListener listener);
    /**
     * Process the record instance.
     * The caller must be the owner as a result of requestProcess returning RequestProcessReturn.success.
     * @param listener If the return value is active
     * this is the listener that will be called when the record completes processing.
     * @return The result of the process request.
     */
    ProcessReturn process(ProcessListener listener);
    /**
     * Remove a completion listener.
     * @param listener The listener.
     */
    void removeCompletionListener(ProcessListener listener);
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
    RequestProcessReturn requestProcessLinkedRecord(DBRecord record,ProcessListener listener);
    /**
     * Remove a completion listener for a linked record.
     * @param listener The listener.
     */
    void removeLinkedCompletionListener(ProcessListener listener);
    /**
     * Set the status and severity for the record.
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
     * Get the current status.
     * @return The status.
     */
    String getStatus();
    /**
     * Get the current alarm severity.
     * @return The severity.
     */
    AlarmSeverity getAlarmSeverity();
    /**
     * Set the timeStamp for the record.
     * @param timeStamp The timeStamp.
     */
    void setTimeStamp(TimeStamp timeStamp);
    /**
     * Get the current timeStamp.
     * @param timeStamp The current timeStamp.
     */
    void getTimeStamp(TimeStamp timeStamp);
}
