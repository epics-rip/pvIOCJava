/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbDefinition.*;

/**
 * The base interface for accessing a field of a record instance.
 * @author mrk
 *
 */
public interface DBData extends PVData {
    /**
     * get the reflection interface for the field.
     * @return the DBDField that describes the field.
     */
    DBDField getDBDField();
    /**
     * get the parent of this field.
     * @return the parent interface.
     */
    DBStructure getParent();
    /**
     * get the record instance that contains this field.
     * @return the record interface.
     */
    DBRecord getRecord();
    /**
     * add a listener for puts.
     * @param listener the listener.
     */
    void addListener(DBListener listener);
    /**
     * remove a listener.
     * @param listener the listener.
     */
    void removeListener(DBListener listener);
    /**
     * the data was modified.
     */
    void postPut();
    /**
     * the data was modified.
     * @param dbData the data that was modified.
     */
    void postPut(DBData dbData);
}
