/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * PVStructure interface.
 * @author mrk
 *
 */
public interface PVStructure extends PVField {	
    /**
     * Get the <i>PVField</i> array for the fields of the structure.
     * @return array of PVField. One for each field.
     */
    PVField[] getFieldPVFields();
    /**
     * Replace a field of the structure that is itself a structure.
     * For an ioc record. This should only be called when a record is in the readyForInitialization state.
     * @param fieldName The field name.
     * @param structure The replacement structure.
     * @return (false,true) if the field (was not,was) replaced.
     */
    boolean replaceStructureField(String fieldName,Structure structure);
}
