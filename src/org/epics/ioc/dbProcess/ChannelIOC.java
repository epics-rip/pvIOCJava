/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.channelAccess.*;

/**
 * @author mrk
 *
 */
public interface ChannelIOC extends Channel{
    boolean isLocal();
}
