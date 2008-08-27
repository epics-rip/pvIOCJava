/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.PVRecord;

/**
 * Factory that creates an instance of a DBRecord.
 * @author mrk
 *
 */
public class DBRecordFactory {
    /**
     * Create a DBRecord.
     * @param pvRecord The PVRecord for this DBRecord.
     * @param iocdb The IOCDB into which this record will be placed.
     * @param dbd The dbd for this record.
     * @return The DBRecord interface.
     */
    public static DBRecord create(PVRecord pvRecord,IOCDB iocdb,DBD dbd) {
        ImplDBRecord dbRecord = new ImplDBRecord(pvRecord,iocdb,dbd);
        dbRecord.init();
        return dbRecord;
    }   
}
