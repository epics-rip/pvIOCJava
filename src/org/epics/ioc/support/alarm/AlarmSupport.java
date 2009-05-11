/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.alarm;

import org.epics.ioc.support.Support;
import org.epics.pvData.property.AlarmSeverity;


/**
 * Support for alarm field.
 * All methods must be called with the record locked.
 * @author mrk
 *
 */
public interface AlarmSupport extends Support{
    /**
     * Called at the beginning of record processing.
     * The alarm in the record is called by recordProcess.
     * Alarm fields in subfields of the record are called by alarmSupport.
     */
    void beginProcess();
    /**
     * Called at the end of record processing.
     */
    void endProcess();
    /**
     * Attempt to set a new alarm.
     * The request is satisfied if the new alarm has higher priority than
     * the current priority. 
     * @param message The message.
     * @param severity The severity for the message.
     * @return (false,true) if the request (was not, was) successful.
     */
    boolean setAlarm(String message, AlarmSeverity severity);
}
