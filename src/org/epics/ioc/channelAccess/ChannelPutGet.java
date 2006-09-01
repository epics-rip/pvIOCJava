/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * Channel access put/get request.
 * The put is performed first, followed optionally by a process request, and then by a get request.
 * @author mrk
 *
 */
public interface ChannelPutGet {
    /**
     * Refuse further requests and clean up internal state.
     */
    void destroy();
    /**
     * Issue a put/get request.
     * @param putFieldGroup The put field group.
     * @param putCallback The put callback listener.
     * @param getFieldGroup The get field group.
     * @param getCallback The get callback listener.
     * @param process Should the record be processed after the put but before the get?
     * @param wait Should the get wait until after process completes?
     */
    void putGet(
        ChannelFieldGroup putFieldGroup,ChannelPutListener putCallback,
        ChannelFieldGroup getFieldGroup,ChannelGetListener getCallback,
        boolean process, boolean wait);
    /**
     * Cancel the put/get. The listeners may be called after this request is issued.
     */
    void cancelPutGet();
}
