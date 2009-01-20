/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.caV3;

import gov.aps.jca.dbr.*;
import gov.aps.jca.event.*;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.pvData.test.RequesterForTesting;
import org.epics.pvData.xml.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;

import org.epics.ioc.util.*;


import org.epics.ioc.ca.*;


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
