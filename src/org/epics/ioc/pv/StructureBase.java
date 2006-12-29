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
    
    /**
     * Constructor for a structure field.
     * @param fieldName The field name.
     * @param property The field properties.
     * @param fieldAttribute The field attributes.
     * @param structureName The structure name.
     * @param field The array of field definitions for the fields of the structure.
     */
    public StructureBase(String fieldName,Property[] property,FieldAttribute fieldAttribute,
        String structureName,Field[] field)
    {
        super(fieldName, Type.pvStructure,property,fieldAttribute);
        if(property!=null) for(Property prop : property) {
            for(Field fieldNow : field) {
                if(fieldNow.getFieldName().equals(prop.getPropertyName())) {
                    throw new IllegalArgumentException(
                        "propertyName " + prop.getPropertyName()
                        + " is the same as a field name");
                }
            }
        }
        this.field = field;
        this.structureName = structureName;
        this.fieldName = new String[field.length];
        sortedFieldNameList = new ArrayList<String>(field.length);
        createNameLists();
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
    
    private void createNameLists() {
        sortedFieldNameList.clear();
        for(int i = 0; i <field.length; i++) {
            fieldName[i] = field[i].getFieldName();
            sortedFieldNameList.add(this.fieldName[i]);
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
            String value = sortedFieldNameList.get(i);
            for(int j=0; j<field.length; j++) {
                if(value.equals(this.fieldName[j])) {
                    fieldIndex[i] = j;
                }
            }
        }
    }
}
