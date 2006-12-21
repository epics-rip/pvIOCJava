/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Class required by get/put PVDoubleArray methods.
 * Get will set data and offset.
 * Put requires that the caller set data and offset.
 * @author mrk
 *
 */
public class DoubleArrayData {
    /**
     * The PVDouble[].
     * PVDoubleArray.get sets this value.
     * PVDoubleArray.put requires that the caller set the value. 
     */
    public double[] data;
    /**
     * The offset.
     * PVDoubleArray.get sets this value.
     * PVDoubleArray.put requires that the caller set the value. 
     */
    public int offset;
}
