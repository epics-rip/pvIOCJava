/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support;

/**
 * The interface that must be implemented by code that calls RecordProcess.processContinue().
 * @author mrk
 *
 */
public interface ProcessContinueRequester {
    /**
     * Continue processing.
     * This is called by RecordProcess.processContinue with the record locked.
     */
    void processContinue();
}
