/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.Requester;

/**
 * Interface for a Channel Monitor Requester.
 * @author mrk
 *
 */
public interface ChannelMonitorRequester extends Requester{
    /**
     * The initial data is available.
     */
    void initialDataAvailable();
}
