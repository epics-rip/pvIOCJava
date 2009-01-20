/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import org.epics.ioc.ca.Channel;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.pvData.test.RequesterForTesting;
import org.epics.pvData.xml.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;

import org.epics.ioc.util.*;


import org.epics.ioc.ca.*;


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
    Executor getExecutor();
    /**
     * If the native type is enum what is the request type.
     * @return The Type.
     */
    ScalarType getEnumRequestScalarType();
}
