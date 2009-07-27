/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import org.epics.pvData.channelAccess.Channel;
import org.epics.pvData.channelAccess.ChannelArray;
import org.epics.pvData.channelAccess.ChannelGet;
import org.epics.pvData.channelAccess.ChannelMonitor;
import org.epics.pvData.channelAccess.ChannelProcess;
import org.epics.pvData.channelAccess.ChannelPut;
import org.epics.pvData.channelAccess.ChannelPutGet;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.pv.ScalarType;


/**
 * Channel interface for communicating with V3 IOCs.
 * @author mrk
 *
 */
public interface V3Channel extends Channel
{
    /**
     * Add a channelProcess
     * @param channelProcess The channelProcess to add.
     * @return (false,true) if the channelProcess (was not, was) added.
     */
    boolean add(ChannelProcess channelProcess);
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
     * Add a channelPutGet
     * @param channelPutGet The channelPutGet to add.
     * @return (false,true) if the channelPutGet (was not, was) added.
     */
    boolean add(ChannelPutGet channelPutGet);
    /**
     * Add a channelMonitor
     * @param channelMonitor The channelMonitor to add.
     * @return (false,true) if the channelMonitor (was not, was) added.
     */
    boolean add(ChannelMonitor channelMonitor);
    /**
     * Add a channelArray
     * @param channelArray The channelArray to add.
     * @return (false,true) if the channelArray (was not, was) added.
     */
    boolean add(ChannelArray channelArray);
    /**
     * Remove a ChannelProcess 
     * @param channelProcess The channelProcess to remove.
     * @return (false,true) if the channelProcess (was not, was) removed.
     */
    boolean remove(ChannelProcess channelProcess);
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
     * Remove a ChannelPutGet 
     * @param channelPutGet The channelPutGet to remove.
     * @return (false,true) if the channelPutGet (was not, was) removed.
     */
    boolean remove(ChannelPutGet channelPutGet);
    /**
     * Remove a ChannelMonitor 
     * @param channelMonitor The channelMonitor to remove.
     * @return (false,true) if the channelMonitor (was not, was) removed.
     */
    boolean remove(ChannelMonitor channelMonitor);
    /**
     * Remove a ChannelArray 
     * @param channelArray The channelArray to remove.
     * @return (false,true) if the channelArray (was not, was) removed.
     */
    boolean remove(ChannelArray channelArray);
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
