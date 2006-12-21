/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

/**
 * A listener to call after and IOCDB has been merged into the master IOCDB.
 * @author mrk
 *
 */
public interface IOCDBMergeListener {
    /**
     * The IOCDB has been merged.
     */
    void merged();
}
