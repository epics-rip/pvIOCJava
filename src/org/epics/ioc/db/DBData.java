/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;


/**
 * The base interface for accessing a field of a record instance.
 * @author mrk
 *
 */
public interface DBData extends PVData {
    /**
     * Get the DBRecord interface.
     * @return The interface.
     */
    DBRecord getDBRecord();
    /**
     * Add a listener for puts.
     * @param listener The listener.
     */
    void addListener(RecordListener listener);
    /**
     * Remove a listener.
     * @param listener The listener.
     */
    void removeListener(RecordListener listener);
    /**
     * The data was modified.
     * This is must be called by the code that implements a put method. 
     */
    void postPut();
    /**
     * Get the Support for the field.
     * @return The support or null if no support exists.
     */
    Support getSupport();
    /**
     * Set the support.
     * @param support The support.
     */
    void setSupport(Support support);
}
