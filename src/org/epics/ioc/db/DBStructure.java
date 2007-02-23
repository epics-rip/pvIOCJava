/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * Interface for an IOC record instance structure field.
 * @author mrk
 *
 */
public interface DBStructure extends DBData {
    /**
     * Get the <i>DBData</i> array for the fields of the structure.
     * @return array of DBData. One for each field.
     */
    DBData[] getFieldDBDatas();
    void replacePVStructure();
    /**
     * The caller  is ready to modify multiple fields of the structure.
     */
    void beginPut();
    /**
     * The caller is done modifying fields of the structure.
     */
    void endPut();
    /**
     * Get the PVStructure for this DBStructure.
     * @return The PVStructure.
     */
    PVStructure getPVStructure();
}
