/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

/**
 * Listener for record processing.
 * @author mrk
 *
 */
public interface RecordProcessListener {
    /**
     * Record has started processing.
     */
    void processStart();
    /**
     * Record has reached end of record processing.
     */
    void processEnd();
}
