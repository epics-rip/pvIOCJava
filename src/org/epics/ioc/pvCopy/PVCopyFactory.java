/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvCopy;

import org.epics.ioc.database.PVRecord;
import org.epics.pvData.pv.PVStructure;
/**
 * A Factory that creates a PVCopy interface which describes a subset of the fields
 * within a PVRecord. It can be used by Channel Access servers.
 * @author mrk
 *
 */
public class PVCopyFactory {
    /**
     * Map a subset of the fields within a PVRecord.
     * @param pvRecord The PVRecord.
     * @param pvRequest A PVStructure which describes the set of fields of PVRecord that
     * should be mapped. See the packaged overview for details.
     * PVRecord of should it keep a separate copy.
     * @param structureName Must be one of null, "field", "putField", or "getField".
     * @return The PVCopy interface.
     */
    public static PVCopy create(PVRecord pvRecord,PVStructure pvRequest,String structureName) {
    	return PVCopyImpl.create(pvRecord, pvRequest,structureName);
    }
}