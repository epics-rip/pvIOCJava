/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVLink;

/**
 * ChannelDataBaseLink - A CDRecord field that holds a PVLink.
 * @author mrk
 *
 */
public interface CDLink extends CDField{
    /**
     * The link configration structure has been modified.
     * @param targetPVLink The link interface.
     */
    void configurationStructurePut(PVLink targetPVLink);
    /**
     * Get the number times configurationStructurePut has been called since the last call to <i>clearNumPuts</i>.
     * @return
     */
    public int getNumConfigurationStructurePuts();
    /**
     * Get the PVLink
     * @return The PVLink interface.
     */
    PVLink getPVLink();
}
