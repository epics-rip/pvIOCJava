/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbProcess.RequestResult;
import org.epics.ioc.util.*;


/**
 * Channel access put/get request.
 * The put is performed first, followed optionally by a process request, and then by a get request.
 * @author mrk
 *
 */
public interface ChannelPutGet {
    /**
     * Issue a put/get request.
     * @param putFieldGroup The put field group.
     * @param getFieldGroup The get field group.
     * @return The result of the request.
     */
    RequestResult putGet(
        ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup);
}
