/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import org.epics.ca.client.Channel;
import org.epics.ca.client.ChannelGet;
import org.epics.ca.client.ChannelPut;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.monitor.Monitor;
import org.epics.pvData.pv.ScalarType;


/**
 * Channel interface for communicating with V3 IOCs.
 * @author mrk
 *
 */
public interface V3Channel extends Channel
{
    /**
     * Add a channelGet
     * @param channelGet The channelGet to add.
     * @return (false,true) if the channelGet (was not, was) added.
     */
    boolean add(ChannelGet channelGet);
    /**
     * Add a channelPut
     * @param channelPut The channelPut to add.
     * @return (false,true) if the channelPut (was not, was) added.
     */
    boolean add(ChannelPut channelPut);
    /**
     * Add a monitor
     * @param monitor The monitor to add.
     * @return (false,true) if the monitor (was not, was) added.
     */
    boolean add(Monitor monitor);
    /**
     * Remove a ChannelGet 
     * @param channelGet The channelGet to remove.
     * @return (false,true) if the channelGet (was not, was) removed.
     */
    boolean remove(ChannelGet channelGet);
    /**
     * Remove a ChannelPut 
     * @param channelPut The channelPut to remove.
     * @return (false,true) if the channelPut (was not, was) removed.
     */
    boolean remove(ChannelPut channelPut);
    /**
     * Remove a Monitor 
     * @param monitor The monitor to remove.
     * @return (false,true) if the monitor (was not, was) removed.
     */
    boolean remove(Monitor monitor);
    /**
     * Get the JCA Channel.
     * @return The interface.
     */
    gov.aps.jca.Channel getJCAChannel();
    /**
     * Get the pvName for this channel.
     * @return The name.
     */
    String getPVName();
    /**
     * Get the V3ChannelRecord for this channel.
     * @return The v3ChannelRecord or null if not connected.
     */
    V3ChannelStructure getV3ChannelStructure();
    /**
     * Get the name of the value field for this channel.
     * @return The fieldName.
     */
    String getValueFieldName();
    /**
     * Get the array of propertyNames for this channel.
     * @return An array of strings.
     */
    String[] getPropertyNames();
    /**
     * Get a general purpose IOCExecutor.
     * @return The iocExecutor;
     */
    Executor getExecutor();
    /**
     * If the native type is enum what is the request type.
     * @return The Type.
     */
    ScalarType getEnumRequestScalarType();
}
