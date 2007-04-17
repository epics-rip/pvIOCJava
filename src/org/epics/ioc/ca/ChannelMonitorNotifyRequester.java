/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.*;

/**
 * Requester that monitors a channel without requesting data.
 * @author mrk
 *
 */
public interface ChannelMonitorNotifyRequester extends Requester {
    /**
     * A monitor event has occured.
     */
    void monitorEvent();
}
