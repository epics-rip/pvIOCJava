/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;


import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;

import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.misc.BitSet;


/**
 * Interface to the data for the channel.
 * @author mrk
 *
 */
public interface V3ChannelStructure {
    /**
     * Get the native DBRType for the value field.
     * @return The DBRType.
     */
    DBRType getNativeDBRType();
    /**
     * Create the PVStructure.
     * @param v3ChannelRecordRequester The requester.
     * @param fieldName The field name for the structure.
     * @return (false,true) if the request can be satisfied.
     */
    boolean createPVStructure(V3ChannelStructureRequester v3ChannelRecordRequester,String fieldName);
    /**
     * Get the PVStructure interface.
     * @return The interface.
     */
    PVStructure getPVStructure();
    /**
     * Get the bitSet for changes.
     * @return The bitSet.
     */
    BitSet getBitSet();
    /**
     * Update the PVStructure with data from a DBR.
     * @param fromDBR The DBR that holds the new data.
     */
    void toStructure(DBR fromDBR);
}
