/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.create;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.*;
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
        DBField valueDBField = dbField;
        DBField dbParent = dbField.getParent();
        pvField = dbParent.getPVField();
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("parent is not a structure", MessageType.error);
            return null;
        }
        DBStructure dbStructure = (DBStructure)dbField;
        DBField[] dbFields = dbStructure.getFieldDBFields();
        PVStructure pvStructure = dbStructure.getPVStructure();
        PVField[] pvFields = pvStructure.getPVFields();
        Structure structure = pvStructure.getStructure();
        int index = structure.getFieldIndex("control");
        if(index<0) {
            pvField.message("parent does not have a field named control", MessageType.error);
            return null;
        }
        pvField = pvFields[index];
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("parent.control is not a structure", MessageType.error);
            return null;
        }
        dbStructure = (DBStructure)dbFields[index];
        dbFields = dbStructure.getFieldDBFields();
        pvStructure = (PVStructure)pvFields[index];
        pvFields = pvStructure.getPVFields();
        structure = pvStructure.getStructure();
        index = structure.getFieldIndex("low");
        if(index<0) {
            pvField.message("parent.control does not have a field named low", MessageType.error);
            return null;
        }
        pvField = pvFields[index];
        if(!pvField.getField().getType().isNumeric()) {
            pvField.message("parent.control.low is not numeric", MessageType.error);
            return null;
        }
        DBField lowDBField = dbFields[index];
        index = structure.getFieldIndex("high");
        if(index<0) {
            pvField.message("parent.control does not have a field named high", MessageType.error);
            return null;
        }
        pvField = pvFields[index];
        if(!pvField.getField().getType().isNumeric()) {
            pvField.message("parent.control.high is not numeric", MessageType.error);
            return null;
        }
        DBField highDBField = dbFields[index];
        Create create = new ControlLimitImpl(valueDBField,lowDBField,highDBField);
        return create;
    }

    private static class ControlLimitImpl extends BaseControlLimit {
        private ControlLimitImpl(DBField valueDBField, DBField lowDBField, DBField highDBField) {
            super(valueDBField,lowDBField,highDBField);
        }
    }
}
