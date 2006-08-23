/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.TimeStamp;

/**
 * Support for record processing.
 * This code is called by record support.
 * @author mrk
 *
 */
public interface RecordProcessSupport {
    /**
     * Request to be called back after process has called record support but before it returns.
     * This must only be called by code running as part of a Support.process request. 
     * The callback will be called with the record unlocked.
     * @param processCallbackListener The listener to call.
     */
    void requestProcessCallback(ProcessCallbackListener processCallbackListener);
    /**
     * Ask the support to continue processing.
     * Only valid if the record is active.
     * @param support The support to call.
     */
    void processContinue(Support support);
    /**
     * Set the status and severity for the record.
     * This must only be called by support processing code.
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
     * This must only be called by support processing code.
     * @return The status.
     */
    String getStatus();
    /**
     * Get the current alarm severity.
     * This must only be called by support processing code.
     * @return The severity.
     */
    AlarmSeverity getAlarmSeverity();
    /**
     * Set the timeStamp for the record.
     * This must only be called by support processing code.
     * @param timeStamp The timeStamp.
     */
    void setTimeStamp(TimeStamp timeStamp);
    /**
     * Get the current timeStamp.
     * This must only be called by support processing code.
     * @param timeStamp The current timeStamp.
     */
    void getTimeStamp(TimeStamp timeStamp);
    /**
     * Issue an error message.
     * This is normally only called by support processing code.
     * In particular it is called by AbstractSupport.errorMessage.
     * @param message The error message.
     */
    void errorMessage(String message);
}
