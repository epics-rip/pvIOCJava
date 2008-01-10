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
     * Specify the group of fields to momnitor.
     * @param channelFieldGroup The field group to monitor.
     */
    void setFieldGroup(ChannelFieldGroup channelFieldGroup);
    /**
     * Get data and put it into the CD (ChannelData).
     * @param cd The ChannelData into which the data is put.
     */
    void getData(CD cd);
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
