/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.misc.Executor;
/**
 * Interface for monitoring channel data.
 * @author mrk
 *
 */
public interface CDMonitor {
    /**
     * Look for any put to a channelField.
     * @param channelField The channelField to monitor.
     * @param causeMonitor If true this will cause monitor even if no other changes occur.
     */
    void lookForPut(ChannelField channelField, boolean causeMonitor);
    /**
     * Look for any put to a channelField.
     * This is only valid for the following types of field:
     * boolean, numeric, and string. It is also valid for the index of a menu or enum field.
     * @param channelField The channelField to monitor.
     * @param causeMonitor If true this will cause monitor even if no other changes occur.
     */
    void lookForChange(ChannelField channelField, boolean causeMonitor);
    /**
     * Look for a put in the value of the field.
     * This can only be used for scalar numeric field.
     * @param channelField The channelField to monitor.
     * @param value The deadband value for changes.
     */
    void lookForAbsoluteChange(ChannelField channelField,double value);
    /**
     * Look for a percentage put in the value of the field.
     * @param channelField The channelField to monitor.
     * @param value The deadband value for changes.
     */
    void lookForPercentageChange(ChannelField channelField,double value);
    /**
     * Start monitoring for changes and send notification of changes but not the data.
     * @param queueSize The queueSize. This must be at least 3.
     * @param executor executor for calling requester.
     */
    void start(int queueSize, Executor executor);   
    /**
     * Stop monitoring.
     */
    void stop();
}
