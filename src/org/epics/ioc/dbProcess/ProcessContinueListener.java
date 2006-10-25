/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * @author mrk
 *
 */
public interface ProcessContinueListener {
    /**
     * Get the name.
     * @return The name.
     */
    String getName();
    /**
     * Continue processing. This is called by RecordProcess.processContinue
     */
    void processContinue();
}
