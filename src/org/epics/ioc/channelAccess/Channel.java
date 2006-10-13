/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * Interface for accessing a channel.
 * For an IOC database a channel allows access to all the fields in a single record instance.
 * @author mrk
 *
 */
public interface Channel {
    /**
     * Prevent any further access and cleanup internal state.
     * If the channel is connected it will be disconnected.
     */
    void destroy();
    /**
     * Is the channel connected?
     * @return (false,true) means (not, is) connected.
     */
    boolean isConnected();
    /**
     * Specify a field to access. See package org.epics.ioc.dbAccess for how IOC database fields are accessed.
     * @param name The name.
     * @return The result
     */
    ChannelSetFieldResult setField(String name);
    /**
     * If the result of setField was other otherChannel, This returns the name of the other channel.
     * @return the name or null if setField did not return otherChannel.
     */
    String getOtherChannel();
    /**
     * If the result of setField was other otherChannel, This returns the name of the field in the other channel.
     * @return The name of the field or null if setField did not return otherChannel.
     */
    String getOtherField();
    /**
     * If setField returned thisChannel this is the interface for the field.
     * @return The interrace for the field or null if setField did not return otherChannel.
     */
    ChannelField getChannelField();
    /**
     * Create a field group.
     * @param listener The liustener to call if the field group is deleted.
     * @return The interface for the field group.
     */
    ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener);
    /**
     * Create a ChannelProcess.
     * @return An interface for the ChannelProcess.
     */
    ChannelProcess createChannelProcess();
    /**
     * Create a ChannelGet.
     * @return An interface for the ChannelGet.
     */
    ChannelGet createChannelGet();
    /**
     * Create a ChannelPut.
     * @return An interface for the ChannelPut.
     */
    ChannelPut createChannelPut( );
    /**
     * Create a ChannelPutGet.
     * @return An interface for the ChannelPutGet.
     */
    ChannelPutGet createChannelPutGet();
    /*
     * Create a ChannelSubscribe.
     * @param queueCapacity capacity of queue for events.
     * @return An interface for the ChannelSunscribe.
     */
    ChannelSubscribe createSubscribe(int queueCapacity);
    /**
     * Is the channel a local channel?
     * @return (false,true) if channel (is not, is) local.
     */
    boolean isLocal();
}
