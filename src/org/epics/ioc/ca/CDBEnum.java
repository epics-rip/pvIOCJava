/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVEnum;

/**
 * @author mrk
 *
 */
public interface CDBEnum extends CDBData{
    /**
     * The enum index has been modified.
     * @param targetPVEnum The enum interface.
     */
    void enumIndexPut(PVEnum targetPVEnum);
    /**
     * The enum choices has been modified.
     * @param targetPVEnum The enum interface.
     */
    void enumChoicesPut(PVEnum targetPVEnum);
    /**
     * Get the number of index puts since the last <i>clearNumPuts</i>.
     * @return The number of index puts.
     */
    int getNumIndexPuts();
    /**
     * Get the number of choics puts since the last <i>clearNumPuts</i>.
     * @return The number of choice puts.
     */
    int getNumChoicesPut();
    /**
     * Get the PVEnum.
     * @return The PVEnum interface.
     */
    PVEnum getPVEnum();
}
