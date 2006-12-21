/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVData;


/**
 * Interface for a channel access put request.
 * @author mrk
 *
 */
public interface ChannelPut {
    /**
     * Put data to a channel.
     * @param fieldGroupImpl The field group for the data.
     * @return (false,true) if the request (is not, is) started.
     * This fails if the request can not be satisfied.
     */
    boolean put(ChannelFieldGroup channelFieldGroup);
    /**
     * If ChannelPutRequestor.nextPutData or ChannelPutRequestor.nextDelayedPutData returns true
     * this is the call to ask again for the data. The result is that the underlying database
     * is locked and ChannelPutRequestor.nextDelayedPutData is called.
     * @param pvData The pvData to put.
     */
    void putDelayed(PVData pvData);
}
