/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;
/**
 * Class required by get/put PVFloatArray methods.
 * Get will set data and offset.
 * Put requires that the caller set data and offset.
 * @author mrk
 *
 */
public class FloatArrayData {
    /**
     * The PVFloat[].
     * PVFloatArray.get sets this value.
     * PVFloatArray.put requires that the caller set the value. 
     */
    public float[] data;
    /**
     * The offset.
     * PVFloatArray.get sets this value.
     * PVFloatArray.put requires that the caller set the value. 
     */
    public int offset;
}
