/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.dbProcess.*;


/**
 * The base interface for accessing a field of a record instance.
 * @author mrk
 *
 */
public interface DBData extends PVData {
    /**
     * Get the reflection interface for the field.
     * @return The DBDField that describes the field.
     */
    DBDField getDBDField();
    /**
     * Get the parent of this field.
     * @return The parent interface.
     */
    DBStructure getParent();
    /**
     * Get the record instance that contains this field.
     * @return The record interface.
     */
    DBRecord getRecord();
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
     * This is the version of postPut that must be called by the code that implements a put method. 
     */
    void postPut();
    /**
     * The data was modified.
     * This version is called by postPut() of fields of a structure.
     * @param dbData The data that was modified.
     */
    void postPut(DBData dbData);
    /**
     * Get the configuration structure for the support.
     * There is no method setConfigurationStructure since the implementation
     * will define and create the configuration structure when setSupportName is called.
     * @return The configuration structure or null if no configuration.
     */
    DBStructure getConfigurationStructure();
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
