/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.util.*;

/**
 * A callback for announcing completion of record processing.
 * @author mrk
 *
 */
public interface RecordProcessRequestor {
    /**
     * Get the name of the SupportProcessRequestor;
     * @return The name.
     */
    String getRecordProcessRequestorName();
    /**
     * Called by record process to signify asynchronous completion.
     * @param requestResult The result of the process request.
     * This is always success or failure.
     */
    void recordProcessComplete(RequestResult requestResult);
    /**
     * The result of the process request.
     * @param alarmSeverity The alarm Severity after processing.
     * @param status The status after processing.
     * @param timeStamp The time stamp after processing.
     */
    void recordProcessResult(AlarmSeverity alarmSeverity,String status,TimeStamp timeStamp);
}
