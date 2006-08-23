/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.channelAccess.*;
import org.epics.ioc.dbAccess.*;

/**
 * A channel for a link field in an IOC record.
 * @author mrk
 *
 */
public interface ChannelLink extends Channel {
    /**
     * Set the record instance that contains the Channel Access link.
     * This must be called by link support immediately after creating a new channel.
     * @param record The record instance containing the link.
     */
    void setLinkRecord(DBRecord record);
    /**
     * Get the record containing the link.
     * @return The record interface.
     */
    DBRecord getLinkRecord();
}
