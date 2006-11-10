/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.*;


/**
 * Interface for a channel access put request.
 * @author mrk
 *
 */
public interface ChannelPut {
    /**
     * Refuse further requests.
     */
    void destroy();
    /**
     * Put data to a channel.
     * @param fieldGroup The field group for the data.
     */
    void put(ChannelFieldGroup fieldGroup);
}
