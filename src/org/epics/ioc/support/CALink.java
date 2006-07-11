/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.dbProcess.*;

/**
 * @author mrk
 *
 */
public interface CALink {
    public ChannelAccessIOC getChannelAccess();
    public ChannelIOC getChannel();
    public void destroy();
}
