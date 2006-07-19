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
public interface ChannelDataPutGet {
    void destroy();
    void beginSynchronous(ChannelFieldGroup inputFieldGroup,ChannelDataGetListener callback);
    PVData getPutPVData(ChannelField field);
    void endSynchronous(boolean process,boolean wait);
    void cancelGetPut();
}
