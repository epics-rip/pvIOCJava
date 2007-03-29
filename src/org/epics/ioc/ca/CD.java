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
     * The enum index has been modified.
     * @param targetPVEnum The enum interface.
     */
    void enumIndexPut(PVEnum targetPVEnum);
    /**
     * The enum choices has been modified.
     * @param targetPVEnum The enum interface.
     */
    void enumChoicesPut(PVEnum targetPVEnum);
    /**
     * The supportName has been modified.
     * @param targetPVField The pvField.
     */
    void supportNamePut(PVField targetPVField);
    /**
     * The link configration structure has been modified.
     * @param targetPVLink The link interface.
     */
    void configurationStructurePut(PVLink targetPVLink);
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
     * A put to an enum subfield of a structure has occured. 
     * The enum index has been modified.
     * @param requested The target field that has targetPVEnum as a subfield.
     * @param targetPVEnum The enum interface.
     */
    void enumIndexPut(PVField requested,PVEnum targetPVEnum);
    /**
     * A put to an enum subfield of a structure has occured. 
     * The enum choices has been modified.
     * @param requested The target field that has targetPVEnum as a subfield.
     * @param targetPVEnum The enum interface.
     */
    void enumChoicesPut(PVField requested,PVEnum targetPVEnum);
    /**
     * A put to the supportName of a subfield of a structure has occured. 
     * The supportName has been modified.
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The pvField in the structure.
     */
    void supportNamePut(PVField requested,PVField targetPVField);
    /**
     * A put to the configurationStructure of a pvLink subfield of a structure has occured. 
     * The link configration structure has been modified.
     * @param requested The target field that has targetPVLink as a subfield.
     * @param targetPVLink The link interface.
     */
    void configurationStructurePut(PVField requested,PVLink targetPVLink);
}
