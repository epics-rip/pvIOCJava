/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;


/**
 * Interface implemented by ScanFactory for field scan.
 * @author mrk
 *
 */
public interface ScanSupport extends Support {
    /**
     * Can the record scan itself.
     * This is true if scan.scanSelf is true.
     * @return (false,true) if the record (can not, can) scan itself.
     */
    boolean canScanSelf();
    /**
     * Ask the record to scan itself.
     * @return (false,true) if the record started processing.
     */
    boolean scanSelf();
}
