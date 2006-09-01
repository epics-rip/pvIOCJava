/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * Interface for getting data from a group of fields.
 * @author mrk
 *
 */
public interface ChannelFieldGroup {
    /**
     * Allow no further requests and clean up any internal state.
     */
    void destroy();
    /**
     * Add a field to the group.
     * @param channelField The field to add.
     */
    void addChannelField(ChannelField channelField);
    /**
     * Remove a field from the group.
     * @param channelField The field to remove.
     */
    void removeChannelField(ChannelField channelField);
}
