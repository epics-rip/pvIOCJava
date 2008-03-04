/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.create;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBRecord;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.util.MessageType;

/**
 * Factory for an enumerated structure.
 * @author mrk
 *
 */
public class ControlLimitFactory {

    /**
     * The create method.
     * @param dbField The field.
     * This assumes a structure of the form:
     * <pre>
     * someStructure
     *     control
     *         limit
     *             low
     *             high
     *     value
     * </pre>
     * value must be a numeric scalar field that has a parent which is structure that has a field named
     * control. The control field must be a structure that has a field named limit.
     * The limit field must have two numeric scalar fields named low and high.
     * @return The Create interface.
     */
    public static Create create(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(!pvField.getField().getType().isNumeric()) {
            pvField.message("field is not numeric", MessageType.error);
            return null;
        }
        PVField pvControl = pvField.findProperty("control");
        if(pvControl==null) {
            pvField.message("control is not a property", MessageType.error);
            return null;
        }
        PVField pvLow = pvControl.findProperty("limit.low");
        PVField pvHigh = pvControl.findProperty("limit.high");
        if(pvLow==null || pvHigh==null) {
            pvField.message("invalid control structure", MessageType.error);
            return null;
        }
        DBRecord dbRecord = dbField.getDBRecord();
        DBField dbLow = dbRecord.findDBField(pvLow);
        DBField dbHigh = dbRecord.findDBField(pvHigh);
        Create create = new ControlLimitImpl(dbField,dbLow,dbHigh);
        return create;
    }

    private static class ControlLimitImpl extends BaseControlLimit {
        private ControlLimitImpl(DBField valueDBField, DBField lowDBField, DBField highDBField) {
            super(valueDBField,lowDBField,highDBField);
        }
    }
}
