/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVField;

/**
 * CD (ChannelData). This provides storage for a ChannelFieldGroup.
 * The storage is presented as a CDRecord.
 * @author mrk
 *
 */
public interface CD {
    /**
     * Destroy this CD.
     */
    void destroy();
    /**
     * Get the record name, which is the same as the channel name.
     * @return The name.
     */
    Channel getChannel();
    /**
     * Get the channelFieldGroup for this channelDataRecord.
     * @return The channelFieldGroup.
     */
    ChannelFieldGroup getChannelFieldGroup();
    /**
     * Get the CDRecord for the channelFieldGroup.
     * @return The interface.
     */
    CDRecord getCDRecord();
    /**
     * Set all number of puts to 0.
     */
    void clearNumPuts();
    /**
     * Get data from the CDField and put it into the pvField.
     * @param pvField The pvField into which to put the data.
     * @return (false,true) if data (was not, was) put into the pvField
     */
    boolean get(PVField pvField);
    /**
     * Put the pvData into the CDField.
     * @param pvField The pvField containing the data to put into the CDField.
     * This must be the PVField for the ChannelField.
     * @return (false,true) if data (was not, was) put into the CDField
     */
    boolean put(PVField pvField);
    /**
     * A put to a subfield of a CDField has occured. 
     * @param pvField The pvField 
     * This must be the PVField for the ChannelField.
     * @param pvSubField The pvField containing the data to put into the subfield of the CDField. 
     * This must be the PVField for ChannelField that is a subfield of ChannelField.
     * @return (false,true) if data (was not, was) put into a subfield of the CDField
     */
    boolean put(PVField pvField,PVField pvSubField);
    /**
     * Create a CDGet.
     * @param cdGetRequester The channelDataGetRequester
     * @param process Process before getting data.
     * @return An interface for the CDGet or null if the caller can't process the record.
     */
    CDGet createCDGet(CDGetRequester cdGetRequester,boolean process);
    /**
     * Destroy a channelDataGet.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param cdGet The channelCDGet
     */
    void destroy(CDGet cdGet);
    /**
     * Create a CDPut.
     * @param cdPutRequester The channelDataPutRequester
     * @param process Should record be processed after put.
     * @return An interface for the CDPut or null if the caller can't process the record.
     */
    CDPut createCDPut(CDPutRequester cdPutRequester,boolean process);
    /**
     * Destroy a channelDataPut.
     * If a request is active it will complete but no new requestes will be accepted.
     * @param cdPut The channelCDPut
     */
    void destroy(CDPut cdPut);
}
