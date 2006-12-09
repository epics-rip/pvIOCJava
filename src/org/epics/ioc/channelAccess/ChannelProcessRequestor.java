/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.*;

/**
 * @author mrk
 *
 */
public interface ChannelProcessRequestor extends Requestor {
    /**
     * The process request is done. This is always called with no locks held.
     * @param requestResult The result of the request.
     */
    void processDone(RequestResult requestResult);
}
