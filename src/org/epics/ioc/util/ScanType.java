/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.util;

import org.epics.ioc.create.Create;
import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStringArray;
import org.epics.ioc.pv.StringArrayData;
import org.epics.ioc.pv.Type;

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
     * @param dbField A field which is potentially a scanType structure.
     * @return The Enumerated interface only if dbField has an Enumerated interface and defines
     * the scanType choices.
     */
    public static Enumerated getScanType(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("field is not an scanType structure", MessageType.error);
            return null;
        }
        DBStructure dbStructure = (DBStructure)dbField;
        Create create = dbStructure.getCreate();
        if(create==null || !(create instanceof Enumerated)) {
            pvField.message("interface Enumerated not found", MessageType.error);
            return null;
        }
        Enumerated enumerated = (Enumerated)create;
        PVStringArray pvChoices = enumerated.getChoicesField();
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
        pvChoices.setMutable(false);
        return enumerated;
    }
}
