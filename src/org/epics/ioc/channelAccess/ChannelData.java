/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.*;

import org.epics.ioc.pvAccess.*;

/**
 * Interface for data to be monitored.
 * @author mrk
 *
 */
public interface ChannelData {
    /**
     * Remove a elements from the list of PVData to monitor.
     */
    void clear();
    /**
     * Add a pvData to a monitor event.
     * @param channelField The channelField for the pvData.
     * @param pvData The pvData.
     */
    void add(ChannelField channelField,PVData pvData);
    /**
     * The list of pvDatas in this monitor event.
     * @return The list.
     */
    List<PVData> getPVDataList();
    /**
     * Get the list of channelFields in this monitor event.
     * These is guaranteed to have the same size and order ad the pvData list.
     * @return The list.
     */
    List<ChannelField> getChannelFieldList();
}
