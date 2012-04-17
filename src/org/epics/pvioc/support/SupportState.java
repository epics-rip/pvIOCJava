/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support;

import org.epics.pvdata.property.PVEnumerated;
import org.epics.pvdata.property.PVEnumeratedFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;



/**
 * State of support.
 * @author mrk
 *
 */
public enum SupportState {
    /**
     * Ready for initialize.
     * This is the initial state for support.
     */
    readyForInitialize,
    /**
     * Ready for start.
     * The initialize methods sets this state when initialization is successful.
     */
    readyForStart,
    /**
     * The support is ready for processing.
     */
    ready,
    /**
     * The support has been asked to be deleted and will no longer perform any actions.
     */
    zombie;
    /**
     * get the support state.
     * @param value the integer value.
     * @return The supportState.
     */
    public static SupportState getSupportState(int value) {
        switch(value) {
        case 0: return SupportState.readyForInitialize;
        case 1: return SupportState.readyForStart;
        case 2: return SupportState.ready;
        case 3: return SupportState.zombie;
        }
        throw new IllegalArgumentException("SupportState getSupportState("
            + ((Integer)value).toString() + ") is not a valid SupportState");
    }
    
    private static final String[] supportStateChoices = {
        "readyForInitialize","readyForStart","ready","zombie"
    };
    /**
     * Convenience method for code that accesses a supportState structure.
     * @param pvField A field which is potentially a supportState structure.
     * @return The Enumerated interface only if dbField has an Enumerated interface and defines
     * the supportState choices.
     */
    public static PVEnumerated getSupportState(PVField pvField) {
        PVEnumerated enumerated = PVEnumeratedFactory.create();
        if(!enumerated.attach(pvField)) return null;
        String[] choices = enumerated.getChoices();
        int len = choices.length;
        if(len!=supportStateChoices.length) {
            pvField.message("not a supportState structure", MessageType.error);
            return null;
        }
        for (int i=0; i<len; i++) {
            if(!choices[i].equals(supportStateChoices[i])) {
                pvField.message("not an supportState structure", MessageType.error);
                return null;
            }
        }
        PVStructure pvStruct = (PVStructure)pvField;
        PVStringArray pvChoices = (PVStringArray)pvStruct.getScalarArrayField("choices",ScalarType.pvString);
        pvChoices.setImmutable();
        return enumerated;
    }
}
