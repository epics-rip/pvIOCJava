/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.*;

/**
 * Requestor that monitors a channel without requesting data.
 * @author mrk
 *
 */
public interface ChannelMonitorNotifyRequestor extends Requestor {
    /**
     * A monitor event has occured.
     */
    void monitorEvent();
}
