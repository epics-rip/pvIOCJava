/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import java.util.*;

import org.epics.ioc.pv.*;

/**
 * Interface for data to be monitored.
 * @author mrk
 *
 */
public interface ChannelData {
    /**
     * Get the channelFieldGroup for this channelData.
     * @return The channelFieldGroup.
     */
    ChannelFieldGroup getChannelFieldGroup();
    /**
     * Remove a elements from the list of PVData added since the last clear.
     */
    void clear();
    /**
     * The initial data when a connection is made.
     * @param channelField The channelField.
     * @param pvData The data.
     */
    void initData(ChannelField channelField,PVData pvData);
    /**
    * Add a pvData to a monitor event.
    * @param pvData The pvData.
    */
   void dataPut(PVData pvData);
   /**
    * The enum index has been modified.
    * @param pvEnum The enum interface.
    */
   void enumIndexPut(PVEnum pvEnum);
   /**
    * The enum choices has been modified.
    * @param pvEnum The enum interface.
    */
   void enumChoicesPut(PVEnum pvEnum);
   /**
    * The supportName has been modified.
    * @param pvData The pvData.
    */
   void supportNamePut(PVData pvData);
   /**
    * The link configration structure has been modified.
    * @param pvLink The link interface.
    */
   void configurationStructurePut(PVLink pvLink);
    /**
     * Start of a structure modification.
     * @param pvStructure The structure.
     */
    void beginPut(PVStructure pvStructure);
    /**
     * End of a structure modification.
     * @param pvStructure The structure.
     */
    void endPut(PVStructure pvStructure);
    /**
     * A put to a subfield of a structure has occured. 
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * It is <i>null</i> if the listener is listening on a non-structure field. 
     * @param pvData The data that has been modified..
     */
    void dataPut(PVStructure pvStructure,PVData pvData);
    /**
     * A put to an enum subfield of a structure has occured. 
     * The enum index has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param pvEnum The enum interface.
     */
    void enumIndexPut(PVStructure pvStructure,PVEnum pvEnum);
    /**
     * A put to an enum subfield of a structure has occured. 
     * The enum choices has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param pvEnum The enum interface.
     */
    void enumChoicesPut(PVStructure pvStructure,PVEnum pvEnum);
    /**
     * A put to the supportName of a subfield of a structure has occured. 
     * The supportName has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param pvData The pvData in the structure.
     */
    void supportNamePut(PVStructure pvStructure,PVData pvData);
    /**
     * A put to the configurationStructure of a pvLink subfield of a structure has occured. 
     * The link configration structure has been modified.
     * @param pvStructure The requester is listening on a structure and this is the structure.
     * @param pvLink The link interface.
     */
    void configurationStructurePut(PVStructure pvStructure,PVLink pvLink);
    
    List<ChannelDataPV> getChannelDataPVList();
    PVData[] getPVDatas();
    ChannelField[] getChannelFields();
}
