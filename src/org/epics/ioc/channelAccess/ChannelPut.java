/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * Interface for a channel access put request.
 * @author mrk
 *
 */
public interface ChannelPut {
    /**
     * Delete any internal state.
     */
    void destroy();
    /**
     * Put data to a channel.
     * @param fieldGroup The field group for the data.
     * @param channelPutListener The listener that provides data.
     * @param process (false,true) if server (should not, should) process after put.
     * @return The result of the request.
     */
    ChannelRequestReturn put(ChannelFieldGroup fieldGroup,ChannelPutListener channelPutListener,boolean process);
    /**
     *  Cancel a put request.
     *  It is possible for the listener to be called after this is called.
     */
    void cancelPut();
}
