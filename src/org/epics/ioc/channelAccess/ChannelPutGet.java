/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.PVData;

/**
 * @author mrk
 *
 */
public interface ChannelPutGet {
    void destroy();
    void putGet(
        ChannelFieldGroup putFieldGroup,ChannelPutListener putCallback,
        ChannelFieldGroup getFieldGroup,ChannelGetListener getCallback,
        boolean process, boolean wait);
    void cancelPutGet();
}
