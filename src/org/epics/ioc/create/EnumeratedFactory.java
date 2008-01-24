/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.create;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Array;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.MessageType;

/**
 * Factory for an enumerated structure.
 * @author mrk
 *
 */
public class EnumeratedFactory {

    /**
     * The create method.
     * @param dbField The field, which must be an enumerated structure,
     * which is a structure that must have the following fields:
     * <ul>
     *    <li>index<br />
     *        An int that is an index that selects one of the choices
     *    </li>
     *    <li>choice<br />
     *        A string that is the choices[index] choice.
     *    </li>
     *    <li>choices<br />
     *        A string array that defined the choices.
     *    </li>
     * </ul>
     * @return The Create interface.
     */
    public static Enumerated create(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("field is not an enumerated structure", MessageType.error);
            return null;
        }
        DBStructure dbStructure = (DBStructure)dbField;
        PVStructure pvStructure = dbStructure.getPVStructure();
        Structure structure = pvStructure.getStructure();
        Field[] fields = structure.getFields();
        Field field = fields[0];
        if(!field.getFieldName().equals("index") || field.getType()!=Type.pvInt) {
            pvStructure.message("structure does not have field index of type int", MessageType.error);
            return null;
        }
        field = fields[1];
        if(!field.getFieldName().equals("choice") || field.getType()!=Type.pvString) {
            pvStructure.message("structure does not have field choice of type string", MessageType.error);
            return null;
        }
        field = fields[2];
        if(!field.getFieldName().equals("choices") || field.getType()!=Type.pvArray) {
            pvStructure.message("structure does not have field choices of type array", MessageType.error);
            return null;
        }
        Array array = (Array)fields[2];
        if(array.getElementType()!=Type.pvString) {
            pvStructure.message("elementType for choices is not string", MessageType.error);
            return null;
        }
        DBField[] dbFields = dbStructure.getDBFields();
        Enumerated enumerated = new EnumeratedImpl(dbField.getPVField(),dbFields[0],dbFields[1],dbFields[2]);
        dbField.setCreate(enumerated);
        return enumerated;
    }

    private static class EnumeratedImpl extends BaseEnumerated {
        private EnumeratedImpl(PVField pvField,DBField dbIndex, DBField dbChoice, DBField dbChoices) {
            super(pvField,dbIndex,dbChoice,dbChoices);
        }
    }
}
