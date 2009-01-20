/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.Requester;

/**
 * Interface 
 * @author mrk
 *
 */
public interface ChannelMonitorRequester extends Requester{
    /**
     * Begin put. Start of one or more dataPuts.
     */
    void beginPut();
    /**
     * End of dataPuts.
     */
    void endPut();
    /**
     * A put to a channelField has occured.
     * @param modifiedPVField The pvField that has been modified.
     */
    void dataPut(PVField modifiedPVField);
    /**
     * A put to a subfield of a channelField has occured.
     * @param requestedPVField The target pvField of the channelField.
     * It can be any field that has subfields. This the pvType can be.
     * <ol>
     *  <li>pvStructure.</li>
     *  <li>pvArray that has a elementType of
     *     <ol>
     *        <li>pvStructure</li>
     *        <li>pvArray</li>
     *     </ol>
     *     </li>
     * </ol>
     * @param modifiedPVField The data that has been modified.
     */
    void dataPut(PVField requestedPVField,PVField modifiedPVField);
}
