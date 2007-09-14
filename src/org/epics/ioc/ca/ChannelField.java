/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.Field;

/**
 * Interface for a field of a channel.
 * @author mrk
 *
 */
public interface ChannelField {
    /**
     * Get the introspection interface for the field.
     * @return the introspection interface.
     */
    Field getField();
    /**
     * Find the channelField for propertyName.
     * @param propertyName The name of the property.
     * @return The ChannelField.
     */
    ChannelField findProperty(String propertyName);
    /**
     * Get the names of properties for this field.
     * @return The array of names.
     */
    String[] getPropertyNames();
    /**
     * Get the access rights for the field.
     * @return The access rights.
     */
    AccessRights getAccessRights();
}
