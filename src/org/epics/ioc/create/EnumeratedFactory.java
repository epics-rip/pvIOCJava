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
 * @author mrk
 *
 */
public class EnumeratedFactory {

    public static Create create(DBField dbField) {
        PVField pvField = dbField.getPVField();
        if(pvField.getField().getType()!=Type.pvStructure) {
            pvField.message("field is not an enumerated structure", MessageType.error);
            return null;
        }
        DBStructure dbStructure = (DBStructure)dbField;
        PVStructure pvStructure = dbStructure.getPVStructure();
        Structure structure = pvStructure.getStructure();
        Field[] fields = structure.getFields();
        if(fields.length!=3) {
            pvStructure.message("structure is not proper enum", MessageType.error);
            return null;
        }
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
        DBField[] dbFields = dbStructure.getFieldDBFields();
        Enumerated enumerated = new EnumeratedImpl(dbFields[0],dbFields[1],dbFields[2]);
        dbField.setCreate(enumerated);
        return enumerated;
    }

    private static class EnumeratedImpl extends BaseEnumerated {
        private EnumeratedImpl(DBField dbIndex, DBField dbChoice, DBField dbChoices) {
            super(dbIndex,dbChoice,dbChoices);
        }
    }
}
