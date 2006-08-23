package org.epics.ioc.channelAccess;

public interface ChannelFieldGroupListener {
    void destroy();
    void accessRightsChange(Channel channel,ChannelField channelField);
}
