/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.Context;
import org.epics.ioc.ca.*;
import org.epics.ioc.db.*;

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
    gov.aps.jca.Channel getJcaChannel();
    /**
     * Get the JCA Context.
     * @return The interface.
     */
    Context getContext();
    /**
     * Get the DBRecord that was created for this channel.
     * @return The interface.
     */
    DBRecord getDBRecord();
    /**
     * Get the DBRType for the value field for this channel.
     * @return The DBRType.
     */
    DBRType getValueDBRType();
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
}
