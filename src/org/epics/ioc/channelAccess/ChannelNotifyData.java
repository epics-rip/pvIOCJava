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
    void add(ChannelField channelField,PVData pvData);
    List<PVData> getPVDataList();
    List<ChannelField> getChannelFieldList();
}
