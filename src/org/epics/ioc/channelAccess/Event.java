/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.BitSet;

/**
 * Subscription event definitions.
 * @author mrk
 *
 */
public interface Event {
    /**
     * Periodic monitoring?
     * @return If true or false.
     */
    boolean getOnPeriodic();
    /**
     * Monitor periodically.
     * @param value true or false.
     */
    void    setOnPeriodic(boolean value);
    /**
     * Monitoring on value change?
     * @return If true or false.
     */
    boolean getOnChange();
    /**
     * Monitor for value changes.
     * @param value true or false. 
     */
    void    setOnChange(boolean value);
    /**
     * Monitoring for alarm changes?
     * @return If true or false.
     */
    boolean getOnAlarm();
    /**
     * Monitor for alarm changes.
     * @param value true or false.
     */
    void    setOnAlarm(boolean value);
    /**
     * Monitoring for event changes.
     * @return If true or false.
     */
    boolean getOnEvent();
    /**
     * Monitor for event changhes.
     * @param value true or false.
     */
    void    setOnEvent(boolean value);
    /**
     * Get the periodic rate.
     * @return The rate in seconds.
     */
    double  getPeriod();
    /**
     * Set the rate for periodic monitoring.
     * @param seconds The rate in seconds.
     */
    void    setPeriod(double seconds);
    /**
     * Get the absolute deadband for change of value monitoring.
     * @return The deadband.
     */
    double  getAbsoluteDeadband();
    /**
     * Set an absolute deadband for change of value monitoring.
     * @param value The deadband.
     */
    void    setAbsoluteDeadband(double value);
    /**
     * Get the percentage change for change of value monitoring.
     * @return The percantage.
     */
    double  getPercentageDeadband();
    /**
     * Set a percentage change for change of value monitoring.
     * @param value The percentage.
     */
    void    setPercentageDeadband(double value);
    /**
     * Get the event code for event monitoring.
     * @return The event code.
     */
    long    getEvent(); // event code that triggers event
    /**
     * Set the event code for event montitoring.
     * @param value The event code.
     */
    void    setEvent(long value);
    /**
     * Get the event mode mask.
     * @return The mode mask.
     */
    BitSet  getModeMask();
    /**
     * ???
     * @param value ???
     */
    void    setModeMask(BitSet value);
    /**
     * ???
     * @return  ???
     */
    BitSet  getModeFilter();
    /**
     * ???
     * @param value ???
     */
    void    setModeFilter(BitSet value);
}
