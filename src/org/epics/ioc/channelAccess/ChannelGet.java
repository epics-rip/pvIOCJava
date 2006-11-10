/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.*;

/**
 * Request to get data from a channel.
 * @author mrk
 *
 */
public interface ChannelGet {
    /**
     * Refuse further requests.
     */
    void destroy();
    /**
     * Get data from the channel.
     * @param fieldGroup The description of the data to get.
     */
    void get(ChannelFieldGroup fieldGroup);
}
