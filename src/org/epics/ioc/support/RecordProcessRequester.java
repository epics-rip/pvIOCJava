/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.util.RequestResult;
import org.epics.pvData.pv.Requester;

/**
 * An interface that must be implemented by code that calls RecordProcess.setRecordProcessRequester().
 * The methods are used to report the result and completion of record processing.
 * @author mrk
 *
 */
public interface RecordProcessRequester extends Requester{
	/**
     * The requester has become the current RecordProcessRequester.
     */
    void becomeProcessor();
    /**
     * A queueProcessRequest failed.
     * @param reason The reason why the request failed.
     */
    void canNotProcess(String reason);
    /**
     * The recordProcessRequester has lost the right to queue a process request.
     * The requester does not have to call releaseProcessToken.
     */
    void lostRightToProcess();
    /**
     * The result of the process request.
     * This is called with the record still active and locked.
     * The requester can read data from the record.
     * @param requestResult The result of the process request.
     */
    void recordProcessResult(RequestResult requestResult);
    /**
     * Called by record process to signify completion.
     * This is called with the record unlocked.
     */
    void recordProcessComplete();
}
