/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
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
     * For an ioc record. This should only be called when a record is in the readyForInitialization state.
     * @param fieldName The field name.
     * @param structureName The nmame of the structure for the replacement field.
     * @return (false,true) if the field (was not,was) replaced.
     */
    boolean replaceStructureField(String fieldName,String structureName);
    /**
     * The caller is ready to modify field of the structure.
     */
    void beginPut();
    /**
     * The caller is done modifying the structure.
     */
    void endPut();
}
