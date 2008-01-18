/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.*;
import org.epics.ioc.pv.*;


/**
 * Interface for accessing a channel.
 * A channel is created via a call to ChannelFactory.createChannel(String pvName, ...).
 * The pvName is of the form recordName.name.name...{options}
 * channel.getField returns name.name...
 * channel.gertProperty returns name.name....value if name.name... locates a structure
 * that has a field named "value", i.e. a structure that follows the data model.
 * For an IOC database a channel allows access to all the fields in a single record instance.
 * @author mrk
 *
 */
public interface Channel extends Requester{
    /**
     * Connect to data source.
     */
    void connect();
    /**
     * Disconnect from data source.
     */
    void disconnect();
    /**
     * Destroy the channel. It will not honor any further requests.
     */
    void destroy();
    /**
     * Get the channel name.
     * @return The name.
     */
    String getChannelName();
    /**
     * Get the PVRecord this channel holds.
     * @return The PVRecord interface.
     */
    PVRecord getPVRecord();
    /**
     * Get the channel listener.
     * @return The listener.
     */
    ChannelListener getChannelListener();
    /**
     * Is the channel connected?
     * @return (false,true) means (not, is) connected.
     */
    boolean isConnected();
    /**
     * Get the propertyName for the channel.
     * @return The name which can be passed to getChannelField.
     */
    String getPropertyName();
    /**
     * Get the fieldName for the channel.
     * @return The name which can be passed to getChannelField.
     */
    String getFieldName();
    /**
     * Get the options for the channel.
     * @return The options.
     */
    String getOptions();
    /**
     * Specify a field to access.
     * @param name The name.
     * @return The ChannelField or null if the field was not found.
     */
    ChannelField createChannelField(String name);
    /**
     * Create a field group.
     * @param listener The listener to call if the field group is deleted.
     * @return The interface for the field group.
     */
    ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener);
    /**
     * Create a ChannelProcess.
     * @param channelProcessRequester The interface for notifying when channel completes processing.
     * @return An interface for the ChannelProcess or null if the caller can't process the record.
     */
    ChannelProcess createChannelProcess(
        ChannelProcessRequester channelProcessRequester);
    /**
     * Create a ChannelGet.
     * The channel will be processed before reading data.
     * @param channelFieldGroup The fieldGroup describing the data to get.
     * @param channelGetRequester The channelGetRequester.
     * @param process Process before getting data.
     * @return An interface for the Get or null if the caller can't process the record.
     */
    ChannelGet createChannelGet(
        ChannelFieldGroup channelFieldGroup,ChannelGetRequester channelGetRequester,
        boolean process);
    /**
     * Create a ChannelPut.
     * @param channelFieldGroup The chanelFieldGroup describing the data to put.
     * @param channelPutRequester The channelPutRequester.
     * @param process Should record be processed after put.
     * @return An interface for the CDPut or null if the caller can't process the record.
     */
    ChannelPut createChannelPut(
        ChannelFieldGroup channelFieldGroup,ChannelPutRequester channelPutRequester,
        boolean process);
    /**
     * Create a ChannelPutGet.
     * @param putFieldGroup The fieldGroup describing the data to put.
     * @param getFieldGroup The fieldGroup describing the data to get.
     * @param channelPutGetRequester The channelPutGetRequester.
     * @param process Process after put and before get.
     * @return An interface for the ChannelPutGet or null if the caller can't process the record.
     */
    ChannelPutGet createChannelPutGet(
        ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
        ChannelPutGetRequester channelPutGetRequester,
        boolean process);
    /**
     * Create a ChannelMonitor.
     * @param channelMonitorRequester The channelMonitorRequester.
     * @return The ChannelMonitor interface.
     */
    ChannelMonitor createChannelMonitor(ChannelMonitorRequester channelMonitorRequester);
    /**
     * Add a channelProcess.
     * @param channelProcess The channelProcess to add.
     * @return (false,true) if the channelProcessd (was not, was) added.
     */
    boolean add(ChannelProcess channelProcess);
    /**
     * Add a channelGet.
     * @param channelGet The channelGet to add.
     * @return (false,true) if the channelGet (was not, was) added.
     */
    boolean add(ChannelGet channelGet);
    /**
     * Add a channelPut.
     * @param channelPut The channelPut to add.
     * @return (false,true) if the channelPut (was not, was) added.
     */
    boolean add(ChannelPut channelPut);
    /**
     * Add a channelPutGet.
     * @param channelPutGet The channelPutGet to add.
     * @return (false,true) if the channelPutGet (was not, was) added.
     */
    boolean add(ChannelPutGet channelPutGet);
    /**
     * Add a channelMonitor.
     * @param channelMonitor The channelMonitor to add.
     * @return (false,true) if the channelMonitor (was not, was) added.
     */
    boolean add(ChannelMonitor channelMonitor);
    /**
     * Remove a ChannelProcess.
     * @param channelProcess The channelProcess to remove.
     * @return (false,true) if the channelProcess (was not, was) removed;
     */
    boolean remove(ChannelProcess channelProcess);
    /**
     * Remove a ChannelGet.
     * @param channelGet The channelGet to remove.
     * @return (false,true) if the channelGet (was not, was) removed;
     */
    boolean remove(ChannelGet channelGet);
    /**
     * Remove a ChannelPut.
     * @param channelPut The channelPut to remove.
     * @return (false,true) if the channelPut (was not, was) removed;
     */
    boolean remove(ChannelPut channelPut);
    /**
     * Remove a ChannelPutGet.
     * @param channelPutGet The channelPutGet to remove.
     * @return (false,true) if the channelPutGet (was not, was) removed;
     */
    boolean remove(ChannelPutGet channelPutGet);
    /**
     * Remove a ChannelMonitor.
     * @param channelMonitor The channelMonitor to remove.
     * @return (false,true) if the channelMonitor (was not, was) removed;
     */
    boolean remove(ChannelMonitor channelMonitor);
}
