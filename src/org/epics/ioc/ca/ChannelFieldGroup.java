/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.List;

/**
 * Interface for getting data from a group of fields.
 * @author mrk
 *
 */
public interface ChannelFieldGroup {
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
    /**
     * Get the list of channel fields.
     * @return The list.
     */
    public List<ChannelField> getList();
}
