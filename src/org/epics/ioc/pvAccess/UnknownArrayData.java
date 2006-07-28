/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * /**
 * Class required by get/put PVUnknownArray methods.
 * Get will set data and offset.
 * Put requires that the caller set data and offset.
 * @author mrk
 *
 */
public class UnknownArrayData {
    /**
     * The PVData[].
     * PVUnknownArray.get sets this value.
     * PVUnknownArray.put requires that the caller set the value. 
     */
    public PVData[] data;
    /**
     * The offset.
     * PVUnknownArray.get sets this value.
     * PVUnknownArray.put requires that the caller set the value. 
     */
    public int offset;
}
