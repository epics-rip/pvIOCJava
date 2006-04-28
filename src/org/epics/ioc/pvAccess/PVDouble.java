/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * get/put double data
 * @author mrk
 *
 */
public interface PVDouble extends PVData{
    /**
     * get the <i>double</i> value stored in the field.
     * @return double value of field.
     */
    double get();
    /**
     * put the <i>double</i> value into the field.
     * @param value new double value for field.
     * @throws IllegalStateException if the field is not mutable.
     */
    void put(double value);
}
