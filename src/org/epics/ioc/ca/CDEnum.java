/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVEnum;

/**
 * ChannelDataBaseEnum - A CDRecord field that holds a PVEnum.
 * @author mrk
 *
 */
public interface CDEnum extends CDField{
    /**
     * The enum index has been modified.
     * @param index The enum interface.
     */
    void enumIndexPut(int index);
    /**
     * The enum choices has been modified.
     * @param choices The enum interface.
     */
    void enumChoicesPut(String[] choices);
    /**
     * Get the number of index puts since the last <i>clearNumPuts</i>.
     * @return The number of index puts.
     */
    int getNumIndexPuts();
    /**
     * Get the number of choices puts since the last <i>clearNumPuts</i>.
     * @return The number of choice puts.
     */
    int getNumChoicesPut();
    /**
     * Get the PVEnum.
     * @return The PVEnum interface.
     */
    PVEnum getPVEnum();
}
