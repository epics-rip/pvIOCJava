/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * get the PVData for the structure fields.
 * @author mrk
 *
 */
public interface PVStructure extends PVData {	
    /**
     * Get the <i>PVData</i> array for the fields of the structure.
     * @return array of PVData. One for each field.
     */
    PVData[] getFieldPVDatas();
    /**
     * Replace a field of the structure.
     * Dor an ioc record this should only be called when a record is in the readyForInitialization state.
     * @param fieldName The field name.
     * @param pvData The replacement field.
     * @return (false,true) if the field (was not,was) replaced.
     */
    boolean replaceField(String fieldName,PVData pvData);
    /**
     * The caller is ready to modify field of the structure.
     */
    void beginPut();
    /**
     * The caller is done modifying the structure.
     */
    void endPut();
}
