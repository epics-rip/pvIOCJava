/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVField;


/**
 * Interface for a channel access put request.
 * @author mrk
 *
 */
public interface ChannelPut {
    /**
     * Put data to a channel.
     * This fails if the request can not be satisfied.
     * If it fails ChannelPutRequestor.putDone is called before put returns.
     */
    void put();
    /**
     * If ChannelPutRequestor.nextPutData or ChannelPutRequestor.nextDelayedPutData returns true
     * this is the call to ask again for the data. The result is that the underlying database
     * is locked and ChannelPutRequestor.nextDelayedPutData is called.
     * @param pvField The pvField to put.
     */
    void putDelayed(PVField pvField);
}
