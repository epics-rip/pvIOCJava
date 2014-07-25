/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support;

import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVScalarArray;
import org.epics.pvdata.pv.PVStringArray;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdata.pv.StringArrayData;
import org.epics.pvdata.pv.Type;



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
    public static PVStructure isSupportStateStructure(PVField pvField) {
        while(true) {
            if(pvField.getField().getType()!=Type.structure) break;
            PVStructure pvStructure = (PVStructure)pvField;
            PVField pvf = pvStructure.getSubField("index");
            if(pvf==null) break;
            if(pvf.getField().getType()!=Type.scalar) break;
            PVScalar pvs = (PVScalar)pvf;
            if(pvs.getScalar().getScalarType()!=ScalarType.pvInt) break;
            pvf = pvStructure.getSubField("choices");
            if(pvf==null) break;
            if(pvf.getField().getType()!=Type.scalarArray) break;
            PVScalarArray pvsa = (PVScalarArray)pvf;
            if(pvsa.getScalarArray().getElementType()!=ScalarType.pvString) break;
            PVStringArray pvStringArray = (PVStringArray)pvsa;
            StringArrayData data = new StringArrayData();
            int len = pvStringArray.getLength();
            pvStringArray.get(0,len , data);
            String[] choices = data.data;
            if(len!=supportStateChoices.length) break;
            for (int i=0; i<len; i++) {
                if(!choices[i].equals(supportStateChoices[i])) break;
            }
            return pvStructure;
        }
        return null;
    }
}
