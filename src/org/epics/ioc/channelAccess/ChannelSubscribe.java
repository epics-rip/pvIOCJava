/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

/**
 * Subscribe for changes.
 * @author mrk
 *
 */
public interface ChannelSubscribe {
    /**
     * Stop any activity and refuse further requests.
     */
    void destroy();
    /**
     * Start monitoring for changes and send notification of changes but not the data.
     * @param fieldGroup The field group to monitor.
     * @param listener The listener to send notification messages.
     * @param why The types of events desired.
     */
    void start(ChannelFieldGroup fieldGroup,ChannelNotifyListener listener,Event why);
    /**
     * Start monitoring for changes and send notification and data.
     * @param fieldGroup The field group to monitor.
     * @param listener the listener to receive the data.
     * @param why The types of events desired.
     */
    void start(ChannelFieldGroup fieldGroup,ChannelNotifyGetListener listener,Event why);
    /**
     * Stop monitoring.
     */
    void stop();
}
