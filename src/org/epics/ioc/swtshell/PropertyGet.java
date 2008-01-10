/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.epics.ioc.ca.ChannelField;

/**
 * Get an array of the property names for a channelField.
 * @author mrk
 *
 */
public interface PropertyGet {
    /**
     * Get property names for a channelField.
     * @param channelField The channelField.
     * @return A array of the property names.
     */
    String[] getPropertyNames(ChannelField channelField);
}
