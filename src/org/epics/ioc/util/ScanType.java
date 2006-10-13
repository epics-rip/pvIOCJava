/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

/**
 * Defines that scan types, i.e. what causes a record to process.
 * @author mrk
 *
 */
public enum ScanType {
    /**
     * A passively scanned record.
     * This is a record that is processed only because some other record or a channel
     * access client asks that the record be processed.
     */
    passive,
    /**
     * An event scanned record.
     * The record is processed when an event announcer requests processing.
     */
    event,
    /**
     * A periodically scanned record.
     */
    periodic
}
