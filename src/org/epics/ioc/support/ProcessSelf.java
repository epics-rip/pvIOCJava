/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

/**
 * Interface for a record that is self processed.
 * A self processed record allows multiple clients to process
 * the record.
 * This interface provides access for the clients which want to process the record.
 * This interface is implemented by the RecordProcessFactory.
 * @author mrk
 *
 */
public interface ProcessSelf {
    /**
     * Request to become record process requester.
     * If another requester is already the requester,
     * the new requester is put in a queue. 
     * @param requester The requester.
     */
    void request(ProcessSelfRequester requester);
    /**
     * Called by the current requester when it is done processing the record.
     * @param requester The requester.
     */
    void endRequest(ProcessSelfRequester requester);
    /**
     * Cancel a request if queued.
     * @param requester The requester.
     */
    void cancelRequest(ProcessSelfRequester requester);
}
