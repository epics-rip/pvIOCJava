/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.Map;

/**
 * Interface for creating introspection interraces.
 * @author mrk
 *
 */
public interface FieldCreate {
    /**
     * Get the Type from a map of attributes.
     * @param attributes The map of attributes.
     * @return The Type.
     * If the attributes does not have a key "type" the result will be Type.pvUnknown.
     */
    public Type getType(Map<String,String> attributes);
    /**
     * Get the element Type from a map of attributes.
     * @param attributes The map of attributes.
     * @return The Type.
     * If the attributes does not have a key "elementType" the result will be Type.pvUnknown.
     */
    public Type getElementType(Map<String,String> attributes);
    /**
     * Get the Type from string.
     * @param value A string with the name of the type.
     * @return The Type.
     * If the string is null or is not the name of a Type, null is returned.
     */
    public Type getType(String value);
    /**
     * Create a FieldAttribute with default attributes.
     * @return The FieldAttribute.
     */
    public FieldAttribute createFieldAttribute();
    /**
     * Create a <i>Field</i> with no properties and default attributes.
     * This must only be called for scalar types,
     * i.e. <i>pvBoolean</i>, ... , <i>pvString</i>
     * For <i>pvEnum</i>, <i>pvArray</i>, and <i>pvStructure</i>
     * the appropriate create method must be called.
     * @param fieldName The field name.
     * @param type The field type.
     * @return a <i>Field</i> interface for the newly created object.
     * @throws <i>IllegalArgumentException</i> if an illegal type is specified.
     */
    public Field createField(String fieldName,Type type);
    /**
     * Create a <i>Field</i>.
     * This must only be called for scalar types,
     * i.e. <i>pvBoolean</i>, ... , <i>pvString</i>
     * For <i>pvEnum</i>, <i>pvArray</i>, and <i>pvStructure</i>
     * the appropriate create method must be called.
     * @param fieldName The field name.
     * @param type The field type .
     * @param fieldAttribute The attributes for the field.
     * If <i>null</i> then a default set of attributes is created.
     * @return a <i>Field</i> interface for the newly created object.
     * @throws <i>IllegalArgumentException</i> if an illegal type is specified.
     */
    public Field createField(String fieldName,Type type,FieldAttribute fieldAttribute);
    /**
     * Create an <i>Array</i> field with no properties and default attributes.
     * @param fieldName The field name
     * @param elementType The <i>Type</i> for array elements
     * @return An <i>Array</i> Interface for the newly created object.
     */
    public Array createArray(String fieldName,Type elementType);
    /**
     * Create an <i>Array</i> field.
     * @param fieldName The field name
     * @param elementType The <i>Type</i> for array elements
     * @param fieldAttribute The attributes for the field.
     * If <i>null</i> then a default set of attributes is created.
     * @return An <i>Array</i> Interface for the newly created object.
     */
    public Array createArray(String fieldName,Type elementType,FieldAttribute fieldAttribute);
    /**
     * Create a <i>Structure</i> field with no properties and default attributes.
     * @param fieldName The field name
     * @param structureName The structure name
     * @param field The array of <i>Field</i> for the structure.
     
     * @return a <i>Structure</i> interface for the newly created object.
     */
    public Structure createStructure(String fieldName,String structureName, Field[] field);
    /**
     * Create a <i>Structure</i> field.
     * @param fieldName The field name
     * @param structureName The structure name
     * @param field The array of <i>Field</i> for the structure.
     * @param fieldAttribute The attributes for the field.
     * If <i>null</i> then a default set of attributes is created.
   
     * @return a <i>Structure</i> interface for the newly created object.
     */
    public Structure createStructure(String fieldName,String structureName, Field[] field,
        FieldAttribute fieldAttribute);
}
