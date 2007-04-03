/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.*;


/**
 * Interface for accessing a channel.
 * For an IOC database a channel allows access to all the fields in a single record instance.
 * @author mrk
 *
 */
public interface Channel {
    /**
     * Get the channel name.
     * @return The name.
     */
    String getChannelName();
    /**
     * Report a message.
     * @param message The message.
     * @param messageType The message type.
     */
    public void message(String message, MessageType messageType);
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
     * Specify a field to access. See package org.epics.ioc.pv for how IOC database fields are accessed.
     * @param name The name.
     * @return The result.
     */
    ChannelFindFieldResult findField(String name);
    /**
     * If the result of findField was otherChannel, This returns the name of the other channel.
     * @return the name or null if findField did not return otherChannel.
     */
    String getOtherChannel();
    /**
     * If the result of findField was other otherChannel, This returns the name of the field in the other channel.
     * @return The name of the field or null if findField did not return otherChannel.
     */
    String getOtherField();
    /**
     * If findField returned thisChannel get the interface for the field.
     * @return The interface for the field or null if findField did not return otherChannel.
     */
    ChannelField getChannelField();
    /**
     * Create a field group.
     * @param listener The listener to call if the field group is deleted.
     * @return The interface for the field group.
     */
    ChannelFieldGroup createFieldGroup(ChannelFieldGroupListener listener);
    /**
     * Create a ChannelProcess.
     * @param channelProcessRequestor The interface for notifying when channel completes processing.
     * @param processSelfOK If record is self processed should ChannelProcess be created?
     * @return An interface for the ChannelProcess or null if the caller can't process the record.
     */
    ChannelProcess createChannelProcess(
        ChannelProcessRequestor channelProcessRequestor,boolean processSelfOK);
    /**
     * Destroy a channelProcess.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelProcess The channelProcess.
     */
    void destroy(ChannelProcess channelProcess);
    /**
     * Create a ChannelGet.
     * The channel will not be processed before data is read.
     * @param channelFieldGroup The fieldGroup describing the data to get.
     * @param channelGetRequestor The channelGetRequestor.
     * @return An interface for the ChannelGet.
     */
    ChannelGet createChannelGet(
        ChannelFieldGroup channelFieldGroup,ChannelGetRequestor channelGetRequestor);
    /**
     * Create a ChannelGet.
     * The channel will be processed before reading data.
     * @param channelFieldGroup The fieldGroup describing the data to get.
     * @param channelGetRequestor The channelGetRequestor.
     * @param process Process before getting data.
     * @param processSelfOK If record is self processed should ChannelGet be created?
     * @return An interface for the ChannelGet or null if the caller can't process the record.
     */
    ChannelGet createChannelGet(
        ChannelFieldGroup channelFieldGroup,ChannelGetRequestor channelGetRequestor,
        boolean process,boolean processSelfOK);
    /**
     * Destroy a channelGet.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelGet The channelGet.
     */
    void destroy(ChannelGet channelGet);
    /**
     * Create a ChannelCDGet.
     * @param channelFieldGroup The chanelFieldGroup describing the data to get.
     * @param channelCDGetRequestor The channelDataGetRequestor
     * @param supportAlso Should support be read/written?
     * @return An interface for the ChannelCDGet.
     */
    ChannelCDGet createChannelCDGet(
            ChannelFieldGroup channelFieldGroup,
            ChannelCDGetRequestor channelCDGetRequestor,boolean supportAlso);
    /**
     * Create a ChannelCDGet.
     * @param channelFieldGroup The chanelFieldGroup describing the data to get.
     * @param channelCDGetRequestor The channelDataGetRequestor
     * @param supportAlso Should support be read/written?
     * @param process Process before getting data.
     * @param processSelfOK If record is self processed should ChannelCDGet be created?
     * @return An interface for the ChannelCDGet or null if the caller can't process the record.
     */
    ChannelCDGet createChannelCDGet(
            ChannelFieldGroup channelFieldGroup,
            ChannelCDGetRequestor channelCDGetRequestor,boolean supportAlso,
            boolean process,boolean processSelfOK);
    /**
     * Destroy a channelDataGet.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelCDGet The channelCDGet
     */
    void destroy(ChannelCDGet channelCDGet);
    /**
     * Create a ChannelPut.
     * @param channelFieldGroup The chanelFieldGroup describing the data to put.
     * @param channelPutRequestor The channelPutRequestor.
     * @return An interface for the ChannelCDPut.
     */
    ChannelPut createChannelPut(
        ChannelFieldGroup channelFieldGroup,ChannelPutRequestor channelPutRequestor);
    /**
     * Create a ChannelPut.
     * @param channelFieldGroup The chanelFieldGroup describing the data to put.
     * @param channelPutRequestor The channelPutRequestor.
     * @param process Should record be processed after put.
     * @param processSelfOK If record is self processed should ChannelPut be created?
     * @return An interface for the ChannelCDPut or null if the caller can't process the record.
     */
    ChannelPut createChannelPut(
        ChannelFieldGroup channelFieldGroup,ChannelPutRequestor channelPutRequestor,
        boolean process,boolean processSelfOK);
    /**
     * Destroy a channelPut.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelPut The channelPut.
     */
    void destroy(ChannelPut channelPut);
    /**
     * Create a ChannelCDPut.
     * @param channelFieldGroup The chanelFieldGroup describing the data to put.
     * @param channelCDPutRequestor The channelDataPutRequestor
     * @param supportAlso Should support be read/written?
     * @return An interface for the ChannelPut.
     */
    ChannelCDPut createChannelCDPut(
            ChannelFieldGroup channelFieldGroup,
            ChannelCDPutRequestor channelCDPutRequestor,boolean supportAlso);
    /**
     * Create a ChannelCDPut.
     * @param channelFieldGroup The chanelFieldGroup describing the data to put.
     * @param channelCDPutRequestor The channelDataPutRequestor
     * @param supportAlso Should support be read/written?
     * @param process Should record be processed after put.
     * @param processSelfOK If record is self processed should ChannelCDPut be created?
     * @return An interface for the ChannelPut or null if the caller can't process the record.
     */
    ChannelCDPut createChannelCDPut(
            ChannelFieldGroup channelFieldGroup,
            ChannelCDPutRequestor channelCDPutRequestor,boolean supportAlso,
            boolean process,boolean processSelfOK);
    /**
     * Destroy a channelDataPut.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelCDPut The channelCDPut
     */
    void destroy(ChannelCDPut channelCDPut);
    /**
     * Create a ChannelPutGet.
     * @param putFieldGroup The fieldGroup describing the data to put.
     * @param getFieldGroup The fieldGroup describing the data to get.
     * @param channelPutGetRequestor The channelPutGetRequestor.
     * @return An interface for the ChannelPutGet.
     */
    ChannelPutGet createChannelPutGet(
        ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
        ChannelPutGetRequestor channelPutGetRequestor);
    /**
     * Create a ChannelPutGet.
     * @param putFieldGroup The fieldGroup describing the data to put.
     * @param getFieldGroup The fieldGroup describing the data to get.
     * @param channelPutGetRequestor The channelPutGetRequestor.
     * @param process Process after put and before get.
     * @param processSelfOK If record is self processed should ChannelPutGet be created?
     * @return An interface for the ChannelPutGet or null if the caller can't process the record.
     */
    ChannelPutGet createChannelPutGet(
        ChannelFieldGroup putFieldGroup,ChannelFieldGroup getFieldGroup,
        ChannelPutGetRequestor channelPutGetRequestor,
        boolean process,boolean processSelfOK);
    /**
     * Destroy a channelPut.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param channelPutGet The channelPut.
     */
    void destroy(ChannelPutGet channelPutGet);
    /**
     * Create a ChannelMonitor.
     * @param onlyWhileProcessing Monitor only while processing?
     * @param supportAlso Should support be read?
     * @return An interface for the ChannelMonitor.
     */
    ChannelMonitor createChannelMonitor(boolean onlyWhileProcessing,boolean supportAlso);
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
