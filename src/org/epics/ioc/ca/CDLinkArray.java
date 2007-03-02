/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * ChannelDataBaseLinkArray - A CDRecord field that holds a PVLinkArray.
 * @author mrk
 *
 */
public interface CDLinkArray extends CDField {
    /**
     * Get the CDLink array for the array elements.
     * @return The CDLink array.
     */
    CDLink[] getElementCDLinks();
    /**
     * Replace the PVLinkArray.
     */
    void replacePVLinkArray();
    /**
     * Put to the PVLinkArray.
     * @param targetPVLinkArray The new values to put.
     */
    void dataPut(PVLinkArray targetPVLinkArray);
    /**
     * A put to a subfield has occured. 
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The data that has been modified..
     */
    boolean fieldPut(PVField requested,PVField targetPVField);
    /**
     * A put to the supportName of a subfield has occured. 
     * The supportName has been modified.
     * @param requested The target field that has targetPVField as a subfield.
     * @param targetPVField The pvField in the structure.
     */
    boolean supportNamePut(PVField requested,PVField targetPVField);
    /**
     * A put to the configurationStructure of a pvLink subfield has occured. 
     * The link configration structure has been modified.
     * @param requested The target field that has targetPVLink as a subfield.
     * @param targetPVLink The link interface.
     */
    boolean configurationStructurePut(PVField requested,PVLink targetPVLink);
}
