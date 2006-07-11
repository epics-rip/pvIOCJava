/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

/**
 * Class required by get/put DBMenuArray methods.
 * Get will set data and offset.
 * Put requires that the caller set data and offset.
 * @author mrk
 *
 */
public class MenuArrayData {
    /**
     * The DBMenu[].
     * DBMenuArray.get sets this value.
     * DBMenuArray.put requires that the caller set the value. 
     */
    public DBMenu[] data;
    /**
     * The offset.
     * DBMenuArray.get sets this value.
     * DBMenuArray.put requires that the caller set the value. 
     */
    public int offset;
}
