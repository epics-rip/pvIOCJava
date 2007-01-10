/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;

/**
 * Data for a ChannelData.
 * @author mrk
 *
 */
public interface ChannelDataPV {
    /**
     * Get the ChannelData for this ChannelDataPV.
     * @return The ChannelData.
     */
    public ChannelData getChannelData();
    /**
     * Get the ChannelField describing this ChannelPV.
     * @return The ChannelField.
     */
    public ChannelField getChannelField();
    /**
     * Get the PVData.
     * If the ChannelField is for a structure this can be a subfield of the structure.
     * @return The PVData.
     */
    public PVData getPVData();
    /**
     * Is this the initialization data for this channelField.
     * @return (false,true) if this is the initialization for the channelField.
     */
    public boolean isInitial();
    /**
     * For a PVEnum has the index changed.
     * @return (false,true) if the index (did not, did) change.
     */
    public boolean enumIndexChange();
    /**
     * For a PVEnum have the choices changed.
     * @return (false,true) if the choices (did not, did) change.
     */
    public boolean enumChoicesChange();
    /**
     * Has the supportName changed.
     * @return (false,true) if the supportName (did not, did) change.
     */
    public boolean supportNameChange();
    /**
     * For a PVLink has the configuration structure changed.
     * @return (false,true) if the configurationStructure (did not, did) change.
     */
    public boolean configurationStructureChange();
    /**
     * @return A dump of the contents,
     */
    public String toString();
}
