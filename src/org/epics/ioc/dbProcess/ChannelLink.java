/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.channelAccess.*;
import org.epics.ioc.dbAccess.*;

/**
 * A channel in a link field of an IOC record.
 * @author mrk
 *
 */
public interface ChannelLink extends Channel {
    /**
     * Set the link instance that contains the Channel Access link.
     * This must be called by link support immediately after creating a new channel.
     * @param dbLink The link.
     */
    void setDBLink(DBLink dbLink);
    /**
     * Get the link using the channel.
     * @return The link interface.
     */
    DBLink getDBLink();
}
