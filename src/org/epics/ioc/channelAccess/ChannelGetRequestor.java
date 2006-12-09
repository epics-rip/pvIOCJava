/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

/**
 * Requestor for a ChannelGet.get request.
 * @author mrk
 *
 */
public interface ChannelGetRequestor extends Requestor {
    /**
     * Next data for a get request..
     * @param field The field.
     * @param data The data.
     * @return (false,true) if the requestor (has,has not) obtained all the data.
     * A value of true means that the requestor has not retrieved all the data.
     * The caller must call ChannelGet.getDelayed in order to access more data.
     * The getDelayed call will result in nextDelayedGetData being called. 
     * This normally means that an array is being transfered and the requestor is
     * not able to handle the array as a single chunk of data. 
     */
    boolean nextGetData(ChannelField field,PVData data);
    /**
     * Called as a result of a call to ChannelGet.getDelayed,
     * The underlying database is locked and this is called. 
     * @param data The data.
     * @return (false,true) if the requestor (will not, will)
     * call ChannelGet.getDelayed again for this pvData.
     */
    boolean nextDelayedGetData(PVData data);
    /**
     * The request is done. This is always called with no locks held.
     * @param requestResult The result of the request.
     */
    void getDone(RequestResult requestResult);
}
