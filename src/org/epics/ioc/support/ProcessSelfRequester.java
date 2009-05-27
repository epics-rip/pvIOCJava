/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

/**
 * A requester to process a record that is self processed.
 * A record that is self processed allows multiple clients
 * to process the record. Each client can get permission to process.
 * The client must implement this interface.
 * @author mrk
 *
 */
public interface ProcessSelfRequester extends RecordProcessRequester {
    /**
     * The requester has become the current RecordProcessRequester.
     * @param recordProcess The RecordProcess for the self processed record.
     */
    void becomeProcessor(RecordProcess recordProcess);
}
