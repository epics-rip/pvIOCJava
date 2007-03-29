/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.pv.*;
import org.epics.ioc.pv.Enum;

/**
 * Base class for a CDRecord (Channel Data Record).
 * @author mrk
 *
 */
public class BaseCDRecord implements CDRecord {
    private FieldCreate fieldCreate;
    private PVDataCreate pvDataCreate;
    private PVRecord pvRecord;
    private CDStructure cdStructure;
    
    /**
     * Constructor.
     * @param fieldCreate Factory to create Field introspection objects.
     * @param pvDataCreate Factory to create PVField objects.
     * @param targetFields The array of Field interfaces for the target.
     * @param recordName The record, i.e. channel name.
     * @param structureName The structure name for the PVStructure for the PVRecord.
     * @param supportAlso Should support be read/written?
     */
    public BaseCDRecord(FieldCreate fieldCreate,PVDataCreate pvDataCreate,
        Field[] targetFields,String recordName,String structureName,boolean supportAlso)
    {
        this.fieldCreate = fieldCreate;
        this.pvDataCreate = pvDataCreate;
        int length = targetFields.length;
        Field[] newFields = new Field[length];
        for(int i=0; i<length; i++) {
            newFields[i] = createField(targetFields[i]);
        }
        Structure structure = fieldCreate.createStructure(
            structureName, structureName, newFields);
        pvRecord = pvDataCreate.createPVRecord(
            recordName, structure);
        cdStructure = new BaseCDStructure(null,this,pvRecord,supportAlso);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#createField(org.epics.ioc.pv.Field)
     */
    public Field createField(Field oldField) {
        Field newField = null;
        Type type = oldField.getType();
        FieldAttribute fieldAttribute = oldField.getFieldAttribute();
        Property[] property = new Property[0];
        String fieldName = oldField.getFieldName();
        if(type==Type.pvArray) {
            newField = fieldCreate.createArray(
                fieldName,((Array)oldField).getElementType(),
                property,fieldAttribute);
        } else if(type==Type.pvEnum) {
            Enum enumField = (Enum)oldField;
            newField = fieldCreate.createEnum(
                fieldName, enumField.isChoicesMutable(),
                property,fieldAttribute);
        } else if(type==Type.pvMenu) {
            Menu menu = (Menu)oldField;
            newField = fieldCreate.createMenu(
                fieldName, menu.getMenuName(),
                menu.getMenuChoices(),property, fieldAttribute);
        } else if(type==Type.pvStructure) {
            Structure structure = (Structure)oldField;
            Field[] oldFields = structure.getFields();
            Field[] newFields = new Field[oldFields.length];
            for(int i=0; i<oldFields.length; i++) {
                newFields[i] = createField(oldFields[i]);
            }
            newField = fieldCreate.createStructure(
                fieldName, structure.getStructureName(),newFields,
                property,fieldAttribute);
        } else {
            newField = fieldCreate.createField(
                fieldName, type,
                property,fieldAttribute);
        }
        return newField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#getCDBStructure()
     */
    public CDStructure getCDStructure() {
        return cdStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#getFieldCreate()
     */
    public FieldCreate getFieldCreate() {
        return fieldCreate;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#getPVDataCreate()
     */
    public PVDataCreate getPVDataCreate() {
        return pvDataCreate;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.ca.CDRecord#getPVRecord()
     */
    public PVRecord getPVRecord() {
        return pvRecord;
    }
}
