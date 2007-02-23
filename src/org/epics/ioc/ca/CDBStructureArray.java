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
public interface CDBStructureArray extends CDBData {
    CDBStructure[] getElementCDBStructures();
    void replacePVStructureArray();
    void dataPut(PVStructureArray targetPVStructureArray);
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
}
