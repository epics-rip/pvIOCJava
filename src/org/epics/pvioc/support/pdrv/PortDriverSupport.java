/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.pdrv;

import org.epics.pvioc.support.Support;


/**
 * Extension of Support for PDRV (portDriver).
 * @author mrk
 *
 */
public interface PortDriverSupport extends Support {
    /**
     * Set the user.
     * This is called by PortDriverLink.start after it has connected to the port and device.
     * @param portDriverLink The interface for PortDriverLink.
     */
    void setPortDriverLink(PortDriverLink portDriverLink);
    /**
     * Called when portDriverLink.process is called.
     */
    void beginProcess();
    /**
     * Called from portDriver.processContinue.
     */
    void endProcess();
    /**
     * A method that must be implemented by all asynchronous support.
     * This probably means all support except interrupt listeners.
     */
    void queueCallback();
}
