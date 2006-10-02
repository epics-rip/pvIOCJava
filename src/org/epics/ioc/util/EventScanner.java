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
public interface EventScanner {
    void addRecord(DBRecord dbRecord);
    void removeRecord(DBRecord dbRecord);
    EventAnnounce addEventAnnouncer(String eventName,String announcer);
    void removeEventAnnouncer(EventAnnounce eventAnnounce,String announcer);
    String toString();
    String show(String eventName);
}
