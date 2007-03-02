/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

/**
 * Interface for non scalar arrays.
 * @author mrk
 *
 */
public interface DBNonScalarArray extends DBField{
    /**
     * Get the array of DBField for the elementds of the array.
     * @return The array of elements.
     * An element is null if the corresponding pvArray element is null.
     */
    DBField[] getElementDBFields();
    /**
     * Replace the current PVArray.
     */
    void replacePVArray();
}
