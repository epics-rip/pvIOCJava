/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;
/**
 * @author mrk
 *
 */
public interface ChannelData {
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
     * Get the array of CDBData. One each element of the channelFieldGroup.
     * @return The array.
     */
    CDBRecord getCDBRecord();
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
     * Is this data initialization?
     * @param targetPVData The pvData at initialization.
     */
    void initData(PVData targetPVData);
    /**
     * The pvData has been modified.
     * @param targetPVData The pvData.
     */
    void dataPut(PVData targetPVData);
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
     * @param targetPVData The pvData.
     */
    void supportNamePut(PVData targetPVData);
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
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVData The data that has been modified..
     */
    void dataPut(PVData requested,PVData targetPVData);
    /**
     * A put to an enum subfield of a structure has occured. 
     * The enum index has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVEnum The enum interface.
     */
    void enumIndexPut(PVData requested,PVEnum targetPVEnum);
    /**
     * A put to an enum subfield of a structure has occured. 
     * The enum choices has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVEnum The enum interface.
     */
    void enumChoicesPut(PVData requested,PVEnum targetPVEnum);
    /**
     * A put to the supportName of a subfield of a structure has occured. 
     * The supportName has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVData The pvData in the structure.
     */
    void supportNamePut(PVData requested,PVData targetPVData);
    /**
     * A put to the configurationStructure of a pvLink subfield of a structure has occured. 
     * The link configration structure has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVLink The link interface.
     */
    void configurationStructurePut(PVData requested,PVLink targetPVLink);
    /**
     * Convert the ChannelData to a string.
     * @return The string.
     */
    String toString();
    /**
     * Convert the ChannelData to a string.
     * Each line is indented.
     * @param indentLevel The indentation level.
     * @return The string.
     */
    String toString(int indentLevel);
}
