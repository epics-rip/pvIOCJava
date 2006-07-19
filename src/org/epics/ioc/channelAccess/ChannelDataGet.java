/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * @author mrk
 *
 */
public interface ChannelDataGet {
    void destroy();
    void get(ChannelFieldGroup fieldGroup,ChannelDataGetListener callback,boolean process, boolean wait);
    void cancelGet();
}
