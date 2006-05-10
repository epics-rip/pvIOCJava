package org.epics.ioc.dbAccess;

/**
 * listener interface.
 * @author mrk
 *
 */
public interface DBListener {
    /**
     * called when data has been modified.
     * @param dbData the interface for the modified data.
     */
    void newData(DBData dbData);
}
