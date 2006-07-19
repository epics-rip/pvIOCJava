/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public interface ChannelDataListener {
    void beginSynchronous();
    void endSynchronous();
    void reason(Event reason);
    void newData(ChannelField field,PVData data);
    void failure(String reason);
}
