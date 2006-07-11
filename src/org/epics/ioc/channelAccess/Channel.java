/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

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
    Field getField();
    Field getPropertyField(Property property);
    Field getPropertyField(String name);
    AccessRights getAccessRights();
    ChannelFieldGroup createFieldGroup();
    void setTimeout(double timeout);
    boolean get(ChannelFieldGroup fieldGroup,ChannelDataGet callback,ChannelOption[] options);
    ChannelDataPut getChannelDataPut(ChannelFieldGroup fieldGroup);
    void subscribe(ChannelFieldGroup fieldGroup,ChannelNotifyListener listener,Event why);
    void subscribe(ChannelFieldGroup fieldGroup,ChannelDataListener listener,Event why);
    
}
