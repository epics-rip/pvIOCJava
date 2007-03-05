/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.*;

/**
 * Requestor for ChannelPut requests.
 * @author mrk
 *
 */
public interface ChannelPutRequestor extends Requestor {
    /**
     * Provide the next set of data to put to the channel.
     * The requestor is expected to call the put method.
     * @param channelField The channelField.
     * @param pvField The interface for putting data.
     * @return (false,true) if the requestor (has,has not) obtained all the data.
     * A value of true means that the requestor wants to be called again for this data.
     * The caller must call ChannelPut.putDelayed in order to access more data.
     * The putDelayed call will result in nextDelayedPutData being called. 
     * This normally means that an array is being transfered and the requestor is
     * not able to handle the array as a single chunk of data.
     */
    boolean nextPutField(ChannelField channelField,PVField pvField);
    /**
     * Called as a result of a call to ChannelPut.putDelayed,
     * The underlying database is locked and this is called. 
     * @param field The data.
     * @return (false,true) if the requestor (will not, will)
     * call ChannelPut.putDelayed again for this pvField.
     */
    boolean nextDelayedPutField(PVField field);
    /**
     * The request is done. This is always called with no locks held.
     * @param requestResult The result of the request.
     */
    void putDone(RequestResult requestResult);
}
