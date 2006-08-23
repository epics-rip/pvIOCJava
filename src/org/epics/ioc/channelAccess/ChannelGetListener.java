/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public interface ChannelGetListener {
    void beginSynchronous(Channel channel);
    void endSynchronous(Channel channel);
    void newData(Channel channel,ChannelField field,PVData data);
    void failure(Channel channel,String reason);
}
