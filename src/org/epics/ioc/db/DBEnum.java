/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.PVEnum;

/**
 * Interface for an IOC record instance enum field.
 * @author mrk
 *
 */
public interface DBEnum extends DBField {
    /**
     * Get the PVEnum for this DBEnum.
     * @return The pvEnum.
     */
    PVEnum getPVEnum();
    /**
     * Replace the current PVEnum.
     */
    void replacePVEnum();
    /**
     * Get the index of the current selected choice.
     * @return index of current choice.
     */
    int getIndex();
    /**
     * Set the choice.
     * @param index for choice.
     */
    void setIndex(int index);
    /**
     * Get the choice values.
     * @return String[] specifying the choices.
     * @throws IllegalStateException if the field is not mutable.
     */
    String[] getChoices();
    /**
     * Set the choice values. 
     * @param choice a String[] specifying the choices.
     * @return (true,false) if the choices were modified.
     * A value of false normally means the choice strings were readonly.
     * @throws UnsupportedOperationException if the choices are not mutable.
     */
    boolean setChoices(String[] choice);    
}
