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
public interface SupportProcessRequestor {
    /**
     * Get the name of the SupportProcessRequestor;
     * @return The name.
     */
    String getSupportProcessRequestorName();
    /**
     * Called by support process to signify asynchronous completion.
     * @param requestResult The result of the process request.
     * If support for a record instance, this must be called with the record locked.
     */
    void processComplete(RequestResult requestResult);
}
