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
 * AlarmSeverity definitions.
 * @author mrk
 *
 */
public enum AlarmSeverity {
    /**
     * Not in alarm.
     */
    none,
    /**
     * Minor alarm.
     */
    minor,
    /**
     * Major Alarm.
     */
    major,
    /**
     * The value is invalid.
     */
    invalid;
    
    /**
     * get the alarm severity.
     * @param value the integer value.
     * @return The alarm severity.
     */
    public static AlarmSeverity getSeverity(int value) {
        switch(value) {
        case 0: return AlarmSeverity.none;
        case 1: return AlarmSeverity.minor;
        case 2: return AlarmSeverity.major;
        case 3: return AlarmSeverity.invalid;
        }
        throw new IllegalArgumentException("AlarmSeverity.getSeverity("
            + ((Integer)value).toString() + ") is not a valid AlarmSeverity");
    }
    
    private static final String[] alarmSeverityChoices = {
        "none","minor","major","invalid"
    };
    /**
     * Convenience method for code the raises alarms.
     * @param dbField A field which is potentially an alarmSeverity structure.
     * @return The Enumerated interface only if dbField has an Enumerated interface and defines
     * the alarmSeverity choices.
     */
    public static Enumerated getAlarmSeverity(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("field is not a structure", MessageType.error);
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
        if(len!=alarmSeverityChoices.length) {
            pvField.message("not an alarmSeverity structure", MessageType.error);
            return null;
        }
        StringArrayData data = new StringArrayData();
        pvChoices.get(0, len, data);
        String[] choices = data.data;
        for (int i=0; i<len; i++) {
            if(!choices[i].equals(alarmSeverityChoices[i])) {
                pvField.message("not an alarmSeverity structure", MessageType.error);
                return null;
            }
        }
        pvChoices.setMutable(false);
        return enumerated;
    }
}
