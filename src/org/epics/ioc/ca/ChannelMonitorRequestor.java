/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.*;


/**
 * Requestor that monitors a channel and wants data returned when a monitor event occurs.
 * @author mrk
 *
 */
public interface ChannelMonitorRequestor extends Requestor{
    /**
     * New subscribe data value.
     * @param channelData The channelData.
     */
    void monitorData(ChannelData channelData);
    /**
     * Monitor event have been missed.
     * @param number Number of missed monitor events.
     */
    void dataOverrun(int number);
}
