/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.*;

/**
 * Request to get data from a channel.
 * @author mrk
 *
 */
public interface ChannelGet {
    /**
     * Get data from the channel.
     * @param fieldGroup The description of the data to get.
     * @return (false,true) if the request (is not, is) started.
     * This fails if the request can not be satisfied.
     */
    boolean get(ChannelFieldGroup fieldGroup);
    /**
     * If ChannelGetRequestor.nextGetData or ChannelGetRequestor.nextDelayedGetData returns true
     * this is the call to ask again for the data. The result is that the underlying database
     * is locked and ChannelGetRequestor.nextDelayedGetData is called.
     * @param pvData
     */
    void getDelayed(PVData pvData);
}
