/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;
/**
 * CD (ChannelData). This provides storage for a ChannelFieldGroup.
 * The storage is presented as a CDRecord.
 * @author mrk
 *
 */
public interface CD {
    /**
     * Get the record name, which is the same as the channel name.
     * @return The name.
     */
    Channel getChannel();
    /**
     * Get the channelFieldGroup for this channelDataRecord.
     * @return The channelFieldGroup.
     */
    ChannelFieldGroup getChannelFieldGroup();
    /**
     * Get the array of CDField. One each element of the channelFieldGroup.
     * @return The array.
     */
    CDRecord getCDRecord();
    /**
     * Get the maximum number of put to a single field since the last <i>clearNumPuts</i>.
     * @return The maximum number of puts.
     */
    int getMaxPutsToField();
    /**
     * Clear the number of puts to all fields.
     */
    void clearNumPuts();
    /**
     * The pvField has been modified.
     * @param targetPVField The pvField.
     */
    void dataPut(PVField targetPVField);
    /**
     * The supportName has been modified.
     * @param targetPVField The pvField.
     */
    void supportNamePut(PVField targetPVField);
    /**
     * Start of a structure modification.
     * @param targetPVStructure The structure.
     */
    void beginPut(PVStructure targetPVStructure);
    /**
     * End of a structure modification.
     * @param targetPVStructure The structure.
     */
    void endPut(PVStructure targetPVStructure);
    /**
     * A put to a subfield of a structure has occured. 
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The data that has been modified..
     */
    void dataPut(PVField requested,PVField targetPVField);
    /**
     * A put to the supportName of a subfield of a structure has occured. 
     * The supportName has been modified.
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The pvField in the structure.
     */
    void supportNamePut(PVField requested,PVField targetPVField);
}
