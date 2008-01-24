/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import org.epics.ioc.ca.Channel;
import org.epics.ioc.util.IOCExecutor;

/**
 * Channel interface for communicating with V3 IOCs.
 * @author mrk
 *
 */
public interface V3Channel extends Channel
{
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
    V3ChannelRecord getV3ChannelRecord();
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
    IOCExecutor getIOCExecutor();
}
