/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * @author mrk
 *
 */
public interface Channel {
    void destroy();
    boolean isConnected();
    ChannelSetFieldResult setField(String name);
    String getOtherChannel();
    String getOtherField();
    ChannelField getChannelField();
    ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener);
    void setTimeout(double timeout);
    ChannelProcess createChannelProcess();
    ChannelGet createChannelGet();
    ChannelPut createChannelPut( );
    ChannelPutGet createChannelPutGet();
    ChannelSubscribe createSubscribe();
    boolean isLocal();
}
