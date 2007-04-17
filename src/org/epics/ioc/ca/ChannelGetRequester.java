/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Requester for a ChannelGet.get request.
 * @author mrk
 *
 */
public interface ChannelGetRequester extends Requester {
    /**
     * Next data for a get request.
     * @param channelField The channelField.
     * @param pvField The data.
     * @return (false,true) if the requester (has,has not) obtained all the data.
     * A value of true means that the requester has not retrieved all the data.
     * The caller must call ChannelGet.getDelayed in order to access more data.
     * The getDelayed call will result in nextDelayedGetData being called. 
     * This normally means that an array is being transfered and the requester is
     * not able to handle the array as a single chunk of data. 
     */
    boolean nextGetField(ChannelField channelField,PVField pvField);
    /**
     * Called as a result of a call to ChannelGet.getDelayed,
     * The underlying database is locked and this is called. 
     * @param pvField The data.
     * @return (false,true) if the requester (will not, will)
     * call ChannelGet.getDelayed again for this pvField.
     */
    boolean nextDelayedGetField(PVField pvField);
    /**
     * The request is done. This is always called with no locks held.
     * @param requestResult The result of the request.
     */
    void getDone(RequestResult requestResult);
}
