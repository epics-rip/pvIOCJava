/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Interface for data to be monitored.
 * @author mrk
 *
 */
public interface CDBData {
    /**
     * Get the parent CDBStructure.
     * @return The parent or null if this is associated witk an element of a ChannelFieldGroup.
     */
    CDBData getParent();
    /**
     * Get the CDBRecord to which this CDBData belongs.
     * @return The CDBRecord interface.
     */
    CDBRecord getCDBRecord();
    /**
     * Get the PVData for this CDBData.
     * @return The PVData interface.
     */
    PVData getPVData();
    /**
     * @param newPVData
     */
    void replacePVData(PVData newPVData);
    /**
     * Get the number of calls to <i>dataPut</i> since the last <i>clearNumPuts</i>.
     * @return
     */
    int getNumPuts();
    /**
     * Get the number of calls to <i>supportNamePut</i> since the last <i>clearNumPuts</i>.
     * @return
     */
    int getNumSupportNamePuts();
    /**
     * Set number of puts to 0 and initial to false.
     */
    void clearNumPuts();
    /**
     * The data has been modified.
     * @param targetPVData The pvData to which the channel is connected. 
     */
    void dataPut(PVData targetPVData);
    /**
     * The support name has been modified.
     * @param targetPVData The pvData to which the channel is connected.
     */
    void supportNamePut(PVData targetPVData);
    /**
     * A put to a subfield has occured. 
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVData The data that has been modified..
     * @return The number of puts to targetPVData.
     */
    int dataPut(PVData requested,PVData targetPVData);
    /**
     * A put to an enum subfield has occured. 
     * The enum index has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVEnum The enum interface.
     * @return The number of index puts to targetPVData.
     */
    int enumIndexPut(PVData requested,PVEnum targetPVEnum);
    /**
     * A put to an enum subfield has occured. 
     * The enum choices has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVEnum The enum interface.
     * @return The number of choices puts to targetPVData.
     */
    int enumChoicesPut(PVData requested,PVEnum targetPVEnum);
    /**
     * A put to the supportName of a subfield has occured. 
     * The supportName has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVData The pvData in the structure.
     * @return The number of supportName puts to targetPVData.
     */
    int supportNamePut(PVData requested,PVData targetPVData);
    /**
     * A put to the configurationStructure of a pvLink subfield has occured. 
     * The link configration structure has been modified.
     * @param requested The target field that has targetPVData as a subfield.
     * @param targetPVLink The link interface.
     * @return The number of confifurationStructure puts to targetPVData.
     */
    int configurationStructurePut(PVData requested,PVLink targetPVLink);
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
