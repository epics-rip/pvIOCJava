/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base interface for a Structure.
 * It is also a complete implementation.
 * @author mrk
 *
 */
public class StructureBase extends FieldBase implements Structure {
    private Field[] field;
    private String[] fieldName;
    private String structureName;
    private List<String> sortedFieldNameList;
    private int[] fieldIndex;
    private boolean replaceOK = false;
    
    /**
     * Constructor for a structure field.
     * @param name The field name.
     * @param property The field properties.
     * @param fieldAttribute The field attributes.
     * @param structureName The structure name.
     * @param field The array of field definitions for the fields of the structure.
     */
    public StructureBase(String name,Property[] property,FieldAttribute fieldAttribute,String structureName,Field[] field)
    {
        super(name, Type.pvStructure,property,fieldAttribute);
        if(property!=null) for(Property prop : property) {
            for(Field fieldNow : field) {
                if(fieldNow.getFieldName().equals(prop.getName())) {
                    throw new IllegalArgumentException(
                        "propertyName " + prop.getName()
                        + " is the same as a field name");
                }
            }
        }
        this.field = field;
        this.structureName = structureName;
        fieldName = new String[field.length];
        sortedFieldNameList = new ArrayList<String>();
        for(int i = 0; i <field.length; i++) {
            fieldName[i] = field[i].getFieldName();
            sortedFieldNameList.add(fieldName[i]);
        }
        Collections.sort(sortedFieldNameList);
        // look for duplicates
        for(int i=0; i<field.length-1; i++) {
            if(sortedFieldNameList.get(i).equals(sortedFieldNameList.get(i+1))) {
                throw new IllegalArgumentException(
                        "fieldName " + sortedFieldNameList.get(i)
                        + " appears more than once");
            }
        }
        fieldIndex = new int[field.length];
        for(int i=0; i<field.length; i++) {
            FieldBase childField = (FieldBase)field[i];
            childField.setParent(this);
            String value = sortedFieldNameList.get(i);
            for(int j=0; j<field.length; j++) {
                if(value.equals(fieldName[j])) {
                    fieldIndex[i] = j;
                }
            }
        }
   }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#getField(java.lang.String)
     */
    public Field getField(String name) {
        int i = Collections.binarySearch(sortedFieldNameList,name);
        if(i>=0) {
            return field[fieldIndex[i]];
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#getFieldIndex(java.lang.String)
     */
    public int getFieldIndex(String name) {
        int i = Collections.binarySearch(sortedFieldNameList,name);
        if(i>=0) return fieldIndex[i];
        return -1;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#getFieldNames()
     */
    public String[] getFieldNames() {
        return fieldName;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#getFields()
     */
    public Field[] getFields() {
        return field;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#getStructureName()
     */
    public String getStructureName() {
        return structureName;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#copy()
     */
    public Structure copy() {
        Field[] newFields = new Field[field.length];
        for(int i=0; i<field.length; i++) newFields[i] = field[i];
        StructureBase structure = new StructureBase(
            super.getFieldName(),null,super.getFieldAttribute(),structureName,newFields);
        structure.replaceOK = true;
        return structure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#replaceField(java.lang.String, org.epics.ioc.pv.Field)
     */
    public boolean replaceField(String name, Field newField) {
        if(!replaceOK) return false;
        int index = getFieldIndex(name);
        if(index<0) return false;
        Field oldField = field[index];
        if(!oldField.getFieldName().equals(newField.getFieldName())) return false;
        replaceOK = false;
        field[index] = newField;
        return true;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString(indentLevel));
        newLine(builder,indentLevel);
        builder.append(String.format("structure %s {",
            structureName));
        for(int i=0, n= field.length; i < n; i++) {
            builder.append(field[i].toString(indentLevel + 1));
        }
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
