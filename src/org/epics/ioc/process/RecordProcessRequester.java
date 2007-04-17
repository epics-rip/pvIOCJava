/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import org.epics.ioc.util.*;

/**
 * A callback for announcing completion of record processing.
 * @author mrk
 *
 */
public interface RecordProcessRequester extends Requester{
    /**
     * The result of the process request.
     * This is called with the record still active and locked.
     * The requester can read data from the record.
     * @param requestResult The result of the process request.
     */
    void recordProcessResult(RequestResult requestResult);
    /**
     * Called by record process to signify asynchronous completion.
     * This is called with the record no longer active and also unlocked.
     * This is NOT called for a postProcess request.
     */
    void recordProcessComplete();
}
