/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;

import org.epics.ioc.pv.*;

/**
 * Interface for data to be monitored.
 * @author mrk
 *
 */
public interface ChannelData {
    /**
     * Get the channelFieldGroup for this channelData.
     * @return The channelFieldGroup.
     */
    ChannelFieldGroup getChannelFieldGroup();
    /**
     * Remove a elements from the list of PVData added since the last clear.
     */
    void clear();
    /**
     * Add a pvData to a monitor event.
     * @param pvData The pvData.
     * @return (false,true) if the pvData (was not, was) in the channelFieldGroup.
     */
    boolean add(PVData pvData);
    /**
     * The list of pvDatas in this monitor event.
     * @return The list.
     */
    List<PVData> getPVDataList();
    /**
     * Get the list of channelFields in this monitor event.
     * These is guaranteed to have the same size and order as the pvData list.
     * @return The list.
     */
    List<ChannelField> getChannelFieldList();
}
