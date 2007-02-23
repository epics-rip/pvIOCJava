/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.PVStructure;

/**
 * 
 * @author mrk
 *
 */
public interface CDBStructure extends CDBData {
    /**
     * Get the <i>CDBData</i> array for the fields of the structure.
     * @return array of CDBData. One for each field.
     */
    CDBData[] getFieldCDBDatas();
    void replacePVStructure();
    PVStructure getPVStructure();
}
