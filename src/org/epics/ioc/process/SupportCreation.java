/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;


/**
 * An interface for creating and initializing all support for a set of record instances.
 * @author mrk
 *
 */
public interface SupportCreation {
    /**
     * Create support for all records in the iocdb.
     * @return (false,true) if all necessary support (was not, was) created.
     */
    boolean createSupport();
    /**
     * Initialize all records in the iocdb.
     * @return (false,true) if all record support entered state SupportState.readyForStart.
     */
    boolean initializeSupport();
    /**
     * Start all records in the iocdb
     * @return (false,true) if all record support entered state SupportState.ready.
     */
    boolean  startSupport();
    /**
     * Unintialize all records in the iocdb.
     */
    void uninitializeSupport();
    /**
     * Stop all records in the iocdb.
     */
    void stopSupport();
}
