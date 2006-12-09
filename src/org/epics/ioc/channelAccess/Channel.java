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
     * Prevent any further access.
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
    ChannelProcess createChannelProcess(ChannelProcessRequestor channelProcessRequestor);
    /**
     * Destroy a channelProcess.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelProcess The channelProcess.
     */
    void destroy(ChannelProcess channelProcess);
    /**
     * Create a ChannelGet.
     * @param channelGetRequestor The channelGetRequestor.
     * @param process Process before getting data.
     * @return An interface for the ChannelGet.
     */
    ChannelGet createChannelGet(ChannelGetRequestor channelGetRequestor,boolean process);
    /**
     * Destroy a channelGet.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelGet The channelGet.
     */
    void destroy(ChannelGet channelGet);
    /**
     * Create a ChannelPut.
     * @param channelPutRequestor The channelPutRequestor.
     * @param process Should record be processed after put.
     * @return An interface for the ChannelPut.
     */
    ChannelPut createChannelPut(ChannelPutRequestor channelPutRequestor,boolean process);
    /**
     * Destroy a channelPut.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelPut The channelPut.
     */
    void destroy(ChannelPut channelPut);
    /**
     * Create a ChannelPutGet.
     * @param channelPutGetRequestor The channelPutGetRequestor.
     * @param process Process after put and before get.
     * @return An interface for the ChannelPutGet.
     */
    ChannelPutGet createChannelPutGet(ChannelPutGetRequestor channelPutGetRequestor,boolean process);
    /**
     * Destroy a channelPut.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelPutGet The channelPut.
     */
    void destroy(ChannelPutGet channelPutGet);
    /**
     * Create a ChannelMonitor.
     * @param onlyWhileProcessing Monitor only while processing?
     * @return An interface for the ChannelMonitor.
     */
    ChannelMonitor createChannelMonitor(boolean onlyWhileProcessing);
    /**
     * Destroy a channelMonitor.
     * @param channelMonitor The channelMonitor.
     */
    void destroy(ChannelMonitor channelMonitor);
    /**
     * Is the channel a local channel?
     * @return (false,true) if channel (is not, is) local.
     */
    boolean isLocal();
}
