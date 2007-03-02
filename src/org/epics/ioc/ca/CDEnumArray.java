/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * ChannelDataBaseEnumArray - A CDRecord field that holds a PVEnumArray.
 * @author mrk
 *
 */
public interface CDEnumArray extends CDField {
    /**
     * Get the CDEnum array for the array elements.
     * @return The CDEnum array.
     */
    CDEnum[] getElementCDEnums();
    /**
     * Replace the PVEnumArray.
     */
    void replacePVEnumArray();
    /**
     * Put to the PVEnumArray.
     * @param targetPVEnumArray The new values to put.
     */
    void dataPut(PVEnumArray targetPVEnumArray);
    /**
     * A put to a subfield has occured. 
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The data that has been modified..
     */
    boolean fieldPut(PVField requested,PVField targetPVField);
    /**
     * A put to an enum subfield has occured. 
     * The enum index has been modified.
     * @param requested The target field that has targetPVEnum as a subfield.
     * @param targetPVEnum The enum interface.
     */
    boolean enumIndexPut(PVField requested,PVEnum targetPVEnum);
    /**
     * A put to an enum subfield has occured. 
     * The enum choices has been modified.
     * @param requested The target field that has targetPVEnum as a subfield.
     * @param targetPVEnum The enum interface.
     */
    boolean enumChoicesPut(PVField requested,PVEnum targetPVEnum);
    /**
     * A put to the supportName of a subfield has occured. 
     * The supportName has been modified.
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The pvField in the structure.
     */
    boolean supportNamePut(PVField requested,PVField targetPVField);
}
