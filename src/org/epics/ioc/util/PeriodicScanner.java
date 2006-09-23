/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.dbAccess.*;

/**
 * @author mrk
 *
 */
public interface PeriodicScanner {
    void schedule(DBRecord dbRecord);
    void unschedule(DBRecord dbRecord);
    String toString();
    String show(ScanPriority priority);
    String show(double rate);
    String show(double rate,ScanPriority priority);
}
