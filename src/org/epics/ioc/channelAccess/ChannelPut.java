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
     * Delete and internal state.
     */
    void destroy();
    /**
     * Put data to a channel.
     * @param fieldGroup The field group for the data.
     * @param callback The listener to notify when done.
     * @param process Process the record after putting data?
     * @param wait Wait until processing is complete?
     */
    void put(ChannelFieldGroup fieldGroup,ChannelPutListener callback,boolean process, boolean wait);
    /**
     *  Cancel a put request.
     *  It is possible for the listener to be called after this is called.
     */
    void cancelPut();
}
