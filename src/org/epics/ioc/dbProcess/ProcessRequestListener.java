/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.util.*;

/**
 * A callback for announcing completion of processing.
 * @author mrk
 *
 */
public interface ProcessRequestListener {
    /**
     * The result of the process request.
     * @param alarmSeverity The alarm Severity after processing.
     * @param status The status after processing.
     * @param timeStamp The time stamp after processing.
     */
    void requestResult(AlarmSeverity alarmSeverity,String status,TimeStamp timeStamp);
    /**
     * Called by record process to signify asynchronous completion.
     * If the support returns active than the listener must expect additional calls.
     */
    void processComplete();
}
