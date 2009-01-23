/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;

import org.epics.ioc.ca.ChannelMonitorRequester;
import org.epics.pvData.pv.PVRecord;


/**
 * Interface to the data for the channel.
 * @author mrk
 *
 */
public interface V3ChannelRecord {
    /**
     * Get the PVRecord interface.
     * @return The interface.
     */
    PVRecord getPVRecord();
    /**
     * Get the native DBRType for the value field.
     * @return The DBRType.
     */
    DBRType getNativeDBRType();
    /**
     * Update the DBRecord with data from a DBR.
     * @param fromDBR The DBR that holds the new data.
     * @param channelMonitorRequester channelMonitorRequester or null.
     */
    void toRecord(DBR fromDBR,ChannelMonitorRequester channelMonitorRequester);
}
