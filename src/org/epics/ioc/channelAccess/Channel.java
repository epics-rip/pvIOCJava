/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.dbAccess.DBData;
import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public interface Channel {
    void destroy();
    boolean isConnected();
    void addListener(ChannelStateListener listener);
    void removeListener(ChannelStateListener listener);
    ChannelSetResult setField(String name);
    String getOtherChannel();
    String getOtherField();
    ChannelData getField();
    ChannelData getPropertyField(Property property);
    ChannelData getPropertyField(String name);
    AccessRights getAccessRights();
    void subscribe(ChannelNotifyListener listener,Event why);
    void subscribe(ChannelDataListener listener,Event why);
    ChannelGetReturn get(ChannelDataGet callback,ChannelOption[] options);
    ChannelDataPut getChannelDataPut();
}
