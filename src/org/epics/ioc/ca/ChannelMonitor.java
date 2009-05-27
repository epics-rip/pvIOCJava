/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;
/**
 * Monitor data from a channel.
 * @author mrk
 *
 */
public interface ChannelMonitor {
    /**
     * Specify the group of nodes to monitor.
     * @param channelFieldGroup The field group to monitor.
     */
    void setFieldGroup(ChannelFieldGroup channelFieldGroup);
    /**
     * Start monitoring.
     */
    void start();
    /**
     * Stop Monitoring.
     */
    void stop();
    /**
     * Destroy the ChannelMonitor;
     */
    void destroy();
}
