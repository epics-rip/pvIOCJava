/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

/**
 * reflection interface for a recordType.
 * @author mrk
 *
 */
public interface DBDRecordType extends DBDStructure {
    /**
     * get the name of the record support.
     * @return the name or null if the support was never created.
     */
    String getRecordSupportName();
    /**
     * set the record support name.
     * @param supportName the name of the support.
     * @return true if the name was set and false if a name was previously set. 
     */
    boolean setRecordSupportName(String supportName);
}
