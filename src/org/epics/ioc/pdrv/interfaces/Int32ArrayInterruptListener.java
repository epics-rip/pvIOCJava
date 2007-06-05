/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pdrv.interfaces;

/**
 * Interface implemented by an Int32ArrayInterruptListener.
 * @author mrk
 *
 */
public interface Int32ArrayInterruptListener{
    /**
     * An interrupt has occured.
     * @param data The data array.
     * @param length The number of elements in the array.
     */
    void interrupt(int[] data,int length);
}
