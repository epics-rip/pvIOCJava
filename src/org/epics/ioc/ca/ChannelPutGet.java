/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVField;


/**
 * Channel access put/get request.
 * The put is performed first, followed optionally by a process request, and then by a get request.
 * @author mrk
 *
 */
public interface ChannelPutGet {
    /**
     * Issue a put/get request.
     * @return (false,true) if the request (is not, is) started.
     * This fails if the request can not be satisfied.
     */
    boolean putGet();
    /**
     * If ChannelPutGetRequestor.nextPutData or ChannelPutGetRequestor.nextDelayedPutData returns true
     * this is the call to ask again for the data. The result is that the underlying database
     * is locked and ChannelPutGetRequestor.nextDelayedPutData is called.
     * @param pvField The pvField to put.
     */
    void putDelayed(PVField pvField);
    /**
     * If ChannelPutGetRequestor.nextGetData or ChannelPutGetRequestor.nextDelayedGetData returns true
     * this is the call to ask again for the data. The result is that the underlying database
     * is locked and ChannelPutGetRequestor.nextDelayedGetData is called.
     * @param pvField The pvField to get.
     */
    void getDelayed(PVField pvField);
}
