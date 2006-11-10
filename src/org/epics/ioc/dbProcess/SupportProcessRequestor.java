/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.util.*;

/**
 * An asynchronous process request is done.
 * @author mrk
 *
 */
public interface SupportProcessRequestor {
    /**
     * The asynchronous process request is finished.
     * This must be called with the record locked.
     * @param requestResult The result.
     */
    void supportProcessDone(RequestResult requestResult);
}
