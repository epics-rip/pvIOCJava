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
public interface LinkListener {
    /**
     * Called by support to signify completion.
     * If the support returns active than the listener must expect additional calls.
     * @param result the reason for calling. A value of active is permissible.
     * In this case support will again call processComplete.
     * @param result why done.
     */
    void processComplete(LinkReturn result);
}
