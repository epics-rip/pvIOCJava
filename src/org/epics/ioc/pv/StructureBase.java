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
    private Field[] fields;
    private String[] fieldNames;
    private String structureName;
    private List<String> sortedFieldNameList;
    private int[] fieldIndex;
    
    /**
     * Constructor for a structure fields.
     * @param fieldNames The fields name.
     * @param structureName The structure name.
     * @param fields The array of fields definitions for the fields of the structure.
     * @param property The fields properties.
     * @param fieldAttribute The fields attributes.
     * @throws IllegalArgumentException if structureName is null;
     */
    public StructureBase(String fieldName,String structureName,Field[] field,
            Property[] property,FieldAttribute fieldAttribute)
    {
        super(fieldName, Type.pvStructure,property,fieldAttribute);
        if(property!=null) for(Property prop : property) {
            for(Field fieldNow : field) {
                if(fieldNow.getFieldName().equals(prop.getPropertyName())) {
                    throw new IllegalArgumentException(
                        "propertyName " + prop.getPropertyName()
                        + " is the same as a fields name");
                }
            }
        }
        if(field==null) field = new Field[0];
        this.fields = field;
        if(structureName==null) {
            throw new IllegalArgumentException("structureName is null");
        }
        this.structureName = structureName;
        fieldNames = new String[field.length];
        sortedFieldNameList = new ArrayList<String>(field.length);
        sortedFieldNameList.clear();
        for(int i = 0; i <field.length; i++) {
            fieldNames[i] = field[i].getFieldName();
            sortedFieldNameList.add(fieldNames[i]);
        }
        Collections.sort(sortedFieldNameList);
        // look for duplicates
        for(int i=0; i<field.length-1; i++) {
            if(sortedFieldNameList.get(i).equals(sortedFieldNameList.get(i+1))) {
                throw new IllegalArgumentException(
                        "fieldNames " + sortedFieldNameList.get(i)
                        + " appears more than once");
            }
        }
        fieldIndex = new int[field.length];
        for(int i=0; i<field.length; i++) {
            String value = sortedFieldNameList.get(i);
            for(int j=0; j<field.length; j++) {
                if(value.equals(this.fieldNames[j])) {
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
            return fields[fieldIndex[i]];
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
        return fieldNames;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Structure#getFields()
     */
    public Field[] getFields() {
        return fields;
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
        for(int i=0, n= fields.length; i < n; i++) {
            builder.append(fields[i].toString(indentLevel + 1));
        }
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
