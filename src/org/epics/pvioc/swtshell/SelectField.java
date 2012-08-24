/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.swtshell;
import org.epics.pvioc.database.PVRecord;

/**
 * A shell for selecting a field of a record.
 * @author mrk
 *
 */
public interface SelectField {
    /**
     * Select a field from a record.
     * @param pvRecord The record.
     * @return The field name.
     */
    String selectFieldName(PVRecord pvRecord);
}
