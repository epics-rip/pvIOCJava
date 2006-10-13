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
     * @param channelPutListener The put callback listener.
     * @param getFieldGroup The get field group.
     * @param channelGetListener The get callback listener.
     * @param process (false,true) if server (should not, should) process after put.
     * @return The result of the request.
     */
    ChannelRequestReturn putGet(
        ChannelFieldGroup putFieldGroup,ChannelPutListener channelPutListener,
        ChannelFieldGroup getFieldGroup,ChannelGetListener channelGetListener,
        boolean process);
    /**
     * Cancel the put/get. The listeners may be called after this request is issued.
     */
    void cancelPutGet();
}
