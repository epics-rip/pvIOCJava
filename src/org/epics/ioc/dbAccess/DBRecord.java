/**
 * 
 */
package org.epics.ioc.dbAccess;


/**
 * interface for a record instance.
 * @author mrk
 *
 */
public interface DBRecord extends DBStructure {
    /**
     * get the record instance name.
     * @return the name.
     */
    String getRecordName();
}
