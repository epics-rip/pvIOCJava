/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Request to get data from a channel.
 * @author mrk
 *
 */
public interface ChannelGet {
    /**
     * Get data from the channel.
     * This fails if the request can not be satisfied.
     * If it fails ChannelGetRequestor.getDone is called before get returns.
     */
    void get();
    /**
     * If ChannelGetRequestor.nextGetData or ChannelGetRequestor.nextDelayedGetData returns true
     * this is the call to ask again for the data. The result is that the underlying database
     * is locked and ChannelGetRequestor.nextDelayedGetData is called.
     * @param pvField The pvField to get.
     */
    void getDelayed(PVField pvField);
}
