/**
 * 
 */
package org.epics.ioc.dbAccess;


/**
 * @author mrk
 *
 */
public interface DBRecord extends DBStructure {
    /**
     * get the record instance name
     * @return the name
     */
    String getRecordName();
}
