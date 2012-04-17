/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;

import org.epics.pvioc.database.PVRecord;

/**
 * Shell to select the name of a local JavaIOC record.
 * @author mrk
 *
 */
public interface SelectLocalRecord {
    /**
     * Prompt for and return the name of a local javaIOOC record.
     * @return The name of the record.
     */
    public String getRecordName();
    /**
     * Get the DBRecord interface.
     * @return The interface or null if no records.
     */
    public PVRecord getPVRecord();
}
