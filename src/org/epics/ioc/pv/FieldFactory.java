/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.Map;

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
     * Create a FieldAttribute from a map of attribute values.
     * @param attributes The map of attributes.
     * @return The DBDFieldAttributes.
     */
    public static FieldAttribute createFieldAttribute(Map<String,String> attributes) {
        int asl = 1;
        String defaultValue = null;
        boolean isDesign = true;
        boolean isLink = false;
        boolean isReadOnly = false;
        String value = attributes.get("default");
        if(value!=null) defaultValue = value;
        value = attributes.get("asl");
        if(value!=null) asl = Integer.parseInt(value);
        value = attributes.get("design");
        if(value!=null) isDesign = Boolean.parseBoolean(value);
        value = attributes.get("link");
        if(value!=null) isLink = Boolean.parseBoolean(value);
        value = attributes.get("readonly");
        if(value!=null) isReadOnly = Boolean.parseBoolean(value);
        return new FieldAttributeImpl(asl,defaultValue,isDesign,isLink,isReadOnly);
    }
    /**
     * Create default field attributes.
     * @return The FieldAttribute with default values.
     */
    public static FieldAttribute createFieldAttribute() {
        return new FieldAttributeImpl(1,null,true,false,false);
    }
    /**
     * Create an <i>Array</i>
     * @param name The field name
     * @param elementType The <i>Type</i> for array elements
     * @param property The field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @param fieldAttribute The attributes for the field.
     * @return An <i>Array</i> Interface for the newly created object.
     */
    public static Array createArrayField(String name,
    Type elementType,Property[] property,FieldAttribute fieldAttribute) {
        return new AbstractArray(name,property,fieldAttribute,elementType);
    } 

    /**
     * Create an <i>Enum</i>
     * @param name The field name
     * @param choicesMutable Can the choices be modified?
     * If no then <i>Enum.isChoicesMutable</i> will return <i>false</i>
     * and an implementation of <i>PVEnum</i> must
     * not allow a caller to modify the choices. 
     * @param property The field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @param fieldAttribute The attributes for the field.
     * @return An <i>Enum</i> interface for the newly created object.
     */
    public static Enum createEnumField(String name,
    boolean choicesMutable,Property[] property,FieldAttribute fieldAttribute)
    {
        return new AbstractEnum(name,property,fieldAttribute,choicesMutable);
    }
    
    /**
     * Create a <i>Menu</i>.
     * @param fieldName The field name.
     * @param property The field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @param fieldAttributeThe attributes for the field.
     * @param menuName The menu name.
     * @return A <i>Menu</i> interface for the newly created object.
     */
    public static Menu createMenuField(String fieldName,
            Property[]property,FieldAttribute fieldAttribute, String menuName)
    {
        return new AbstractMenu(fieldName,property,fieldAttribute,menuName);
    }
    /**
     * Create a <i>Field</i>.
     * This must only be called for scalar types,
     * i.e. <i>pvBoolean</i>, ... , <i>pvString</i>
     * For <i>pvEnum</i>, <i>pvArray</i>, and <i>pvStructure</i>
     * the appropriate create method must be called.
     * @param name The field name.
     * @param type The field type .
     * @param property The field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @param fieldAttribute The attributes for the field.
     * @return a <i>Field</i> interface for the newly created object.
     * @throws <i>IllegalArgumentException</i> if an illegal type is specified.
     */
    public static Field createField(String name,Type type,Property[] property,FieldAttribute fieldAttribute)
    {
        if(!type.isScalar() && type!=Type.pvLink) throw new IllegalArgumentException(
                "Illegal PVType. Must be scalar but it is " + type.toString() );
        return new AbstractField(name,type,property,fieldAttribute);
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
     * @param fieldAttribute The attributes for the field.
     * @return a <i>Structure</i> interface for the newly created object.
     */
    public static Structure createStructureField(String name,
    String structureName, Field[] field,Property[] property,FieldAttribute fieldAttribute)
    {
        return new AbstractStructure(name,property,fieldAttribute,structureName,field);
    }
    
    private static class FieldAttributeImpl implements FieldAttribute {
        private int asl = 1;
        private String defaultValue = null;
        private boolean isDesign = true;
        private boolean isLink = false;
        private boolean isReadOnly = false;
        
        private FieldAttributeImpl(int asl, String defaultValue,
            boolean isDesign, boolean isLink, boolean isReadOnly)
        {
            super();
            this.asl = asl;
            this.defaultValue = defaultValue;
            this.isDesign = isDesign;
            this.isLink = isLink;
            this.isReadOnly = isReadOnly;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldAttribute#getAsl()
         */
        public int getAsl() {
            return asl;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldAttribute#getDefault()
         */
        public String getDefault() {
            return defaultValue;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldAttribute#isDesign()
         */
        public boolean isDesign() {
            return isDesign;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldAttribute#isLink()
         */
        public boolean isLink() {
            return isLink;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDFieldAttribute#isReadOnly()
         */
        public boolean isReadOnly() {
            return isReadOnly;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldAttribute#toString(int)
         */
        public String toString(int indentLevel) {
            return String.format(
                " asl %d design %b link %b readOnly %b",
                asl,isDesign,isLink,isReadOnly);
        }
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
         * @see org.epics.ioc.pv.Property#toString(int)
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
         * @see org.epics.ioc.pv.Property#getFieldName()
         */
        public String getFieldName() { return fieldName;}
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.Property#getName()
         */
        public String getName() { return name;}
    }
}
