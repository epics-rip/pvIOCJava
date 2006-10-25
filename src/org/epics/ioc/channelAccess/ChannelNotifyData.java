/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.*;

import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public interface ChannelNotifyData {
    void clear();
    void add(PVData pvData);
    ChannelNotifyGetListener getChannelNotifyGetListener();
    Channel getChannel();
    ChannelFieldGroup getChannelFieldGroup();
    List<PVData> getPVDataList();
}
