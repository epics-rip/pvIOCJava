/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import java.util.BitSet;

/**
 * @author mrk
 *
 */
public interface Event {
    boolean getOnPeriodic();
    void    setOnPeriodic(boolean value);
    boolean getOnChange();
    void    setOnChange(boolean value);
    boolean getOnAlarm();
    void    setOnAlarm(boolean value);
    boolean getOnEvent();
    void    setOnEvent(boolean value);
    double  getPeriod();
    void    setPeriod(double seconds);
    double  getAbsoluteDeadband();
    void    setAbsoluteDeadband(double value);
    double  getPercentageDeadband();
    void    setPercentageDeadband(double value);
    long    getEvent(); // event code that triggers event
    void    setEvent(long value);
    BitSet  getModeMask();
    void    setModeMask(BitSet value);
    BitSet  getModeFilter();
    void    setModeFilter(BitSet value);
}
