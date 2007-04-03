/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.process.RecordProcessRequestor;


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
     * @param recordProcessRequestor The requestor to notify if the record did start processing.
     * @return (false,true) if the record is ready to processing.
     */
    boolean processSelfRequest(RecordProcessRequestor recordProcessRequestor);
    /**
     * Set the record active.
     * @param recordProcessRequestor The recordProcessRequestor.
     */
    void processSelfSetActive(RecordProcessRequestor recordProcessRequestor);
    /**
     * start processing.
     * this can only be called after processSelfRequest returns true. 
     * @param recordProcessRequestor The recordProcessRequestor.
     * @param leaveActive Leave the record active when process is done.
     */
    void processSelfProcess(RecordProcessRequestor recordProcessRequestor, boolean leaveActive);
    /**
     * Called by the recordProcessRequestor when it called processSelfProcess with leaveActive true.
     * @param recordProcessRequestor The recordProcessRequestor.
     */
    void processSelfSetInactive(RecordProcessRequestor recordProcessRequestor);
}
