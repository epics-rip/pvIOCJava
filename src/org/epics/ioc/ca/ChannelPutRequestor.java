/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVData;
import org.epics.ioc.util.*;

/**
 * Listener for ChannelPut requests.
 * @author mrk
 *
 */
public interface ChannelPutRequestor extends Requestor {
    /**
     * Provide the next set of data to put to the channel.
     * The requestor is expected to call the put method.
     * @param field The field.
     * @param data The interface for putting data.
     * @return (false,true) if the requestor (has,has not) obtained all the data.
     * A value of true means that the requestor wants to be called again for this data.
     * The caller must call ChannelPut.putDelayed in order to access more data.
     * The putDelayed call will result in nextDelayedPutData being called. 
     * This normally means that an array is being transfered and the requestor is
     * not able to handle the array as a single chunk of data.
     */
    boolean nextPutData(ChannelField field,PVData data);
    /**
     * Called as a result of a call to ChannelPut.putDelayed,
     * The underlying database is locked and this is called. 
     * @param data The data.
     * @return (false,true) if the requestor (will not, will)
     * call ChannelPut.putDelayed again for this pvData.
     */
    boolean nextDelayedPutData(PVData data);
    /**
     * The request is done. This is always called with no locks held.
     * @param requestResult The result of the request.
     */
    void putDone(RequestResult requestResult);
}
