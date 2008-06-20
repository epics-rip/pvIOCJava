/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.process;

import org.epics.ioc.util.RequestResult;

/**
 * A process request is done.
 * @author mrk
 *
 */
public interface SupportProcessRequester {
    /**
     * The process request is finished.
     * This must be called with the record locked.
     * @param requestResult The result.
     */
    void supportProcessDone(RequestResult requestResult);
}
