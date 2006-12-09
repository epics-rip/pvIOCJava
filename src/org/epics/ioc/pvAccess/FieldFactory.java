/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * FieldFactory creates Field instances.
 * This is a complete factory for the PV reflection.
 * Most PV database implementations should find this sufficient for
 * PV reflection.
 * @author mrk
 *
 */


public final class FieldFactory {
    
    private FieldFactory(){} // dont create
    /**
     * Create an <i>Array</i>
     * @param name field name
     * @param elementType the <i>Type</i> for array elements
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return  an <i>Array</i> interface for the newly created object.
     */
    public static Array createArrayField(String name,
    Type elementType,Property[] property) {
        return new AbstractArray(name,property,elementType);
    } 

    /**
     * Create an <Enum</i>
     * @param name field name
     * @param choicesMutable Can the choices be modified?
     * If no then <i>Enum.isChoicesMutable</i> will return <i>false</i>
     * and an implementation of <i>PVEnum</i> must
     * not allow a caller to modify the choices. 
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return an <i>Enum</i> interface for the newly created object.
     */
    public static Enum createEnumField(String name,
    boolean choicesMutable,Property[] property)
    {
        return new AbstractEnum(name,property,choicesMutable);
    } 

    /**
     * Create a <i>Field</i>.
     * This must only be called for scalar types,
     * i.e. <i>pvBoolean</i>, ... , <i>pvString</i>
     * For <i>pvEnum</i>, <i>pvArray</i>, and <i>pvStructure</i>
     * the appropriate create method must be called.
     * @param name field name.
     * @param type field type .
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return a <i>Field</i> interface for the newly created object.
     * @throws <i>IllegalArgumentException</i> if an illegal type is specified.
     */
    public static Field createField(String name,Type type,Property[] property)
    {
        if(!type.isScalar() && type!=Type.pvUnknown) throw new IllegalArgumentException(
                "Illegal PVType. Must be scalar but it is " + type.toString() );
        return new AbstractField(name,type,property);
    } 
    
    /**
     * Create a <i>Property</i>
     * @param name the property name
     * @param fieldName the associated field
     * @return a <i>Property<i/> interface for the newly created object.
     */
    public static Property createProperty(String name, String fieldName) {
        return new PropertyInstance(name,fieldName);
    } 

    /**
     * Create a <i>Structure</i>
     * @param name The field name
     * @param structureName The structure name
     * @param field The array of <i>Field</i> for the structure.
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return a <i>Structure</i> interface for the newly created object.
     */
    public static Structure createStructureField(String name,
    String structureName, Field[] field,Property[] property)
    {
        return new AbstractStructure(name,property,structureName,field);
    } 

    private static class PropertyInstance implements Property {
        private String fieldName;
        private String name;
    
        PropertyInstance(String name, String fieldName) {
            this.name = name;
            this.fieldName = fieldName;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() { return getString(0);}

        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Property#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            AbstractField.newLine(builder,indentLevel);
            builder.append(String.format("{name = %s field = %s}",
                    name,fieldName));
            return builder.toString();
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Property#getFieldName()
         */
        public String getFieldName() { return fieldName;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Property#getName()
         */
        public String getName() { return name;}
    }
}
