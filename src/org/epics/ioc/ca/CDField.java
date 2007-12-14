/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * CDField - Data for a field of a CDRecord (Channel Data Record)
 * @author mrk
 *
 */
public interface CDField {
    /**
     * Get the parent CDField.
     * @return The parent or null if this is associated with an element of a ChannelFieldGroup.
     */
    CDField getParent();
    /**
     * Get the CDRecord to which this CDField belongs.
     * @return The CDRecord interface.
     */
    CDRecord getCDRecord();
    /**
     * Get the ChannelField for this CDField.
     * @return The ChannelField.
     */
    ChannelField getChannelField();
    /**
     * Get the PVField that has the data for this CDField.
     * @return The PVField interface.
     */
    PVField getPVField();
    /**
     * Get the number of dataPuts to this field.
     * @return The number of dataPuts.
     */
    int getNumPuts();
    /**
     * Increment the number of dataPuts to this field.
     */
    void incrementNumPuts();
    /**
     * Get the maximum number of puts since the last <i>clearNumPuts</i>.
     * This is the maximum number of puts to any particular element of the associated PVField.
     * If PVField is a scalar or an array of scalars then this is the same as numPuts.
     * @return The maximum number.
     */
    int getMaxNumPuts();
    /**
     * Set the maximum number of puts.
     * This is called by derived classes.
     * @param numPuts The number of puts.
     */
    void setMaxNumPuts(int numPuts);
    /**
     * Set all number of puts to 0.
     */
    void clearNumPuts();
    /**
     * Get data from the CDField and put it into the pvField.
     * @param pvField The pvField into which to put the data.
     * @param postPut Should channelField.postPut be called?
     */
    void get(PVField pvField,boolean postPut);
    /**
     * Put the pvData into the CDField.
     * @param pvField The pvField containing the data to put into the CDField.
     * This must be the PVField for the ChannelField.
     */
    void put(PVField pvField);
    /**
     * A put to a subfield of a CDField has occured. 
     * @param pvField The pvField 
     * This must be the PVField for the ChannelField.
     * @param pvSubField The pvField containing the data to put into the subfield opf the CDField. 
     * This must be the PVField for ChannelField that is a subfield of ChannelField.
     */
    void put(PVField pvField,PVField pvSubField);
    /**
     * Report current state.
     * @return A String describing the state.
     */
    String toString();
    /**
     * Report current state.
     * @param indentLevel Indentation level for newlines.
     * @return A String describing the state.
     */
    String toString(int indentLevel);

}
