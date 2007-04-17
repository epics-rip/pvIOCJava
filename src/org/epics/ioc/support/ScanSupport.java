/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.process.RecordProcessRequester;


/**
 * Interface implemented by ScanFactory for field scan.
 * @author mrk
 *
 */
public interface ScanSupport extends Support {
    /**
     * Can the record process itself.
     * This is true if scan.processSelf is true.
     * @return (false,true) if the record (can not, can) scan itself.
     */
    boolean canProcessSelf();
    /**
     * Ask the record to scan itself.
     * This is called with the record locked.
     * If the request returns true then startScan must be called with the record unlocked.
     * @param recordProcessRequester The requester to notify if the record did start processing.
     * @return (false,true) if the record is ready to processing.
     */
    boolean processSelfRequest(RecordProcessRequester recordProcessRequester);
    /**
     * Set the record active.
     * @param recordProcessRequester The recordProcessRequester.
     */
    void processSelfSetActive(RecordProcessRequester recordProcessRequester);
    /**
     * start processing.
     * this can only be called after processSelfRequest returns true. 
     * @param recordProcessRequester The recordProcessRequester.
     * @param leaveActive Leave the record active when process is done.
     */
    void processSelfProcess(RecordProcessRequester recordProcessRequester, boolean leaveActive);
    /**
     * Called by the recordProcessRequester when it called processSelfProcess with leaveActive true.
     * @param recordProcessRequester The recordProcessRequester.
     */
    void processSelfSetInactive(RecordProcessRequester recordProcessRequester);
}
