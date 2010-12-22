/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.pvData.property.PVEnumerated;
import org.epics.pvData.property.PVEnumeratedFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVArray;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVStringArray;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.StringArrayData;
import org.epics.pvData.pv.Type;

/**
 * Defines that scan types, i.e. what causes a record to process.
 * @author mrk
 *
 */
public enum ScanType {
    /**
     * A passively scanned record.
     * This is a record that is processed only because some other record or a channel
     * access client asks that the record be processed.
     */
    passive,
    /**
     * An event scanned record.
     * The record is processed when an event announcer requests processing.
     */
    event,
    /**
     * A periodically scanned record.
     */
    periodic;
    
    private static final String[] scanTypeChoices = {
        "passive","event","periodic"
    };
    /**
     * Convenience method for code the raises alarms.
     * @param pvField A field which is potentially a scanType structure.
     * @return The Enumerated interface only if pvField has an Enumerated interface and defines
     * the scanType choices.
     */
    public static PVEnumerated getScanType(PVField pvField) {
        if(pvField.getField().getType()!=Type.structure) {
            pvField.message("field is not a structure", MessageType.error);
            return null;
        }
        PVEnumerated enumerated = PVEnumeratedFactory.create();
        if(!enumerated.attach(pvField)) {
            pvField.message("interface Enumerated not found", MessageType.error);
            return null;
        }
        PVStructure pvStruct = (PVStructure)pvField;
        PVArray pvArray = pvStruct.getScalarArrayField("choices", ScalarType.pvString);
        PVStringArray pvChoices = (PVStringArray)pvArray;
        int len = pvChoices.getLength();
        if(len!=scanTypeChoices.length) {
            pvField.message("not an scanType structure", MessageType.error);
            return null;
        }
        StringArrayData data = new StringArrayData();
        pvChoices.get(0, len, data);
        String[] choices = data.data;
        for (int i=0; i<len; i++) {
            if(!choices[i].equals(scanTypeChoices[i])) {
                pvField.message("not an scanType structure", MessageType.error);
                return null;
            }
        }
        pvChoices.setImmutable();
        return enumerated;
    }
}
