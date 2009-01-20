/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVRecord;

/**
 * @author mrk
 *
 */
public interface SupportDatabase {
    PVDatabase getPVDatabase();
    void mergeIntoMaster();
    RecordSupport getRecordSupport(PVRecord pvRecord);
}
