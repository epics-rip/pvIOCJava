/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVEnumerated;
import org.epics.ioc.pv.PVField;

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
     * Get the PVField for this channelField.
     * @return The PVField interface.
     */
    PVField getPVField();
    /**
     * The client has put data to channelField.
     */
    void postPut();
    /**
     * Find the channelField for propertyName.
     * @param propertyName The name of the property.
     * @return The ChannelField.
     */
    ChannelField findProperty(String propertyName);
    /**
     * Create a ChannelField for the fieldName.
     * This is only valid if the ChannelField contains a structure.
     * @param fieldName The fieldName.
     * @return The ChannelField or null if invalid request.
     */
    ChannelField createChannelField(String fieldName);
    /**
     * Get the names of properties for this field.
     * @return The array of names.
     */
    String[] getPropertyNames();
    /**
     * Get the Enumerated interface if the field is an enumereated structure.
     * @return The interface or null if the field is not an enumerated structure.
     */
    PVEnumerated getEnumerated();
    /**
     * Get the access rights for the field.
     * @return The access rights.
     */
    AccessRights getAccessRights();
}
