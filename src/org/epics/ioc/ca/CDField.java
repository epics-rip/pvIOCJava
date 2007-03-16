/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * CDField - Data for a field of a CDRecord (ChannelData Record)
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
     * Get the PVField for this CDField.
     * @return The PVField interface.
     */
    PVField getPVField();
    /**
     * Replace the PVField.
     * @param newPVField The new PVField.
     */
    void replacePVField(PVField newPVField);
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
     * This is the maximum number of puts to any particular element of the associated PVField,
     * i.e. dataPut, supportNamePut, enumIndexPut, enumChoicePut, etc.
     * If PVField is a scalar or an array of scalars then this is either the number of calls to dataPut
     * or to supportNamePut, whichever is greater.
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
     * Get the number of calls to <i>supportNamePut</i> since the last <i>clearNumPuts</i>.
     * @return The number of supportNamePuts.
     */
    int getNumSupportNamePuts();
    /**
     * Set all number of puts to 0.
     */
    void clearNumPuts();
    /**
     * The data has been modified.
     * @param targetPVField The pvField to which the channel is connected. 
     */
    void dataPut(PVField targetPVField);
    /**
     * The support name has been modified.
     * @param supportName The pvField to which the channel is connected.
     */
    void supportNamePut(String supportName);
    /**
     * A put to a subfield has occured. 
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The data that has been modified..
     * @return (false,true) if the associated PVField is modified.
     * The return value can be false of the requested field is an array of structures or an array of arrays.
     */
    boolean dataPut(PVField requested,PVField targetPVField);
    /**
     * A put to an enum subfield has occured. 
     * The enum index has been modified.
     * @param requested The target field that has targetPVEnum as a subfield.
     * @param targetPVEnum The enum interface.
     * @return (false,true) if the associated PVField is modified.
     * The return value can be false of the requested field is an array of structures or an array of arrays.
     */
    boolean enumIndexPut(PVField requested,PVEnum targetPVEnum);
    /**
     * A put to an enum subfield has occured. 
     * The enum choices has been modified.
     * @param requested The target field that has targetPVEnum as a subfield.
     * @param targetPVEnum The enum interface.
     * @return (false,true) if the associated PVField is modified.
     * The return value can be false of the requested field is an array of structures or an array of arrays.
     */
    boolean enumChoicesPut(PVField requested,PVEnum targetPVEnum);
    /**
     * A put to the supportName of a subfield has occured. 
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The pvField in the structure.
     * @return (false,true) if the associated PVField is modified.
     * The return value can be false of the requested field is an array of structures or an array of arrays.
     */
    boolean supportNamePut(PVField requested,PVField targetPVField);
    /**
     * A put to the configurationStructure of a pvLink subfield has occured. 
     * @param requested The target field that has targetPVLink as a subfield.
     * @param targetPVLink The link interface.
     * @return (false,true) if the associated PVField is modified.
     * The return value can be false of the requested field is an array of structures or an array of arrays.
     */
    boolean configurationStructurePut(PVField requested,PVLink targetPVLink);
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
