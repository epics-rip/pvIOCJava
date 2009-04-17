/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.install;


/**
 * An interface for creating and initializing all support for a set of record instances.
 * @author mrk
 *
 */
public interface SupportCreation {
    /**
     * Create support for all records in the supportDatabase.
     * @return (false,true) if all necessary support (was not, was) created.
     */
    boolean createSupport();
    /**
     * Initialize all records in the supportDatabase.
     * @return (false,true) if all record support entered state SupportState.readyForStart.
     */
    boolean initializeSupport();
    /**
     * JavaIOC all records in the supportDatabase
     * @param afterStart interface for being called after all support has started.
     * @return (false,true) if all record support entered state SupportState.ready.
     */
    boolean  startSupport(AfterStart afterStart);
}
