/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

/**
 * A callback for announcing completion of processing.
 * @author mrk
 *
 */
public interface ProcessListener {
    /**
     * Called by support to signify completion.
     * If the support returns active than the listener must expect additional calls.
     * @param result the reason for calling. A value of active is permissible.
     * In this case support will again call processComplete.
     * @param result why done.
     */
    void processComplete(ProcessReturn result);
}
