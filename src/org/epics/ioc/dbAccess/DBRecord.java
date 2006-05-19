/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
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
