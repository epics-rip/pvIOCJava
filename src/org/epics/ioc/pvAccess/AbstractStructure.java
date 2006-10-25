/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mrk
 *
 */
public class AbstractStructure extends AbstractField implements Structure {
    private Field[] field;
    private String[] fieldName;
    private String structureName;
    private List<String> sortedFieldNameList;
    private int[] fieldIndex;
    
    public AbstractStructure(String name,Property[] property,String structureName,Field[] field)
    {
        super(name, Type.pvStructure,property);
        if(property!=null) for(Property prop : property) {
            for(Field fieldNow : field) {
                if(fieldNow.getName().equals(prop.getName())) {
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
            fieldName[i] = field[i].getName();
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
            AbstractField childField = (AbstractField)field[i];
            childField.setParent(this);
            String value = sortedFieldNameList.get(i);
            for(int j=0; j<field.length; j++) {
                if(value.equals(fieldName[j])) {
                    fieldIndex[i] = j;
                }
            }
        }
   }

    /**
     * Get the Field definition.
     * @return The Field.
     */
    public Field[] getField() {
        return field;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Structure#getField(java.lang.String)
     */
    public Field getField(String name) {
        int i = Collections.binarySearch(sortedFieldNameList,name);
        if(i>=0) {
            return field[fieldIndex[i]];
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Structure#getFieldIndex(java.lang.String)
     */
    public int getFieldIndex(String name) {
        int i = Collections.binarySearch(sortedFieldNameList,name);
        if(i>=0) return fieldIndex[i];
        return -1;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Structure#getFieldNames()
     */
    public String[] getFieldNames() {
        return fieldName;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Structure#getFields()
     */
    public Field[] getFields() {
        return field;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Structure#getStructureName()
     */
    public String getStructureName() {
        return structureName;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Field#toString(int)
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
