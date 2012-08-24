/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.util;

/**
 * Announce an event.
 * This is created by calling EventScanner.addEventAnnouncer.
 * @author mrk
 *
 */
public interface EventAnnounce {
    /**
     * Announce an event.
     * All records that are event scanned for the event name are scheduled for execution.
     */
    void announce();
}
