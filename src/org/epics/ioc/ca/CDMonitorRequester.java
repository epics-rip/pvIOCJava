/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.Requester;

/**
 * @author mrk
 *
 */
public interface CDMonitorRequester extends Requester{
    /**
     * New subscribe data value.
     * @param cD The channelData.
     */
    void monitorCD(CD cD);
    /**
     * ChannelMonitorRequester event have been missed.
     * @param number Number of missed monitor events.
     */
    void dataOverrun(int number);
}
