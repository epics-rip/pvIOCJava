package org.epics.ioc.channelAccess;

public interface ChannelFieldGroupListener {
    void accessRightsChange(Channel channel,ChannelField channelField);
}
