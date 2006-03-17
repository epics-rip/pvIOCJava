/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * FieldFactory creates Field instances.
 * This is a complete factory for the PV introspection.
 * Most PV database implementations should find this sufficient for
 * creating all PV introspection objects.
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
        return new ArrayInstance(name,elementType,property);
    } 

    /**
     * Create an <Enum</i>
     * @param name field name
     * @param choicesMutable Can the choices be modified?
     * If no then <i>Enum.isChoicesMutable</i> will return <i>false</i>
     * and any implementation of <i>PVEnum</i> must never allow a caller to modify the
     * choices. 
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return an <i>Enum</i> interface for the newly created object.
     */
    public static Enum createEnumField(String name,
    boolean choicesMutable,Property[] property) {
        return new EnumInstance(name,choicesMutable,property);
    } 

    /**
     * Create a <i>Field</i>.
     * This must only be called for scalar types, i.e. <i>pvBoolean</i>, ... , <i>pvString</i>
     * For <i>pvEnum</i>, <i>pvArray</i>, and <i>pvStructure</i>
     * the appropriate create method must be called.
     * An <i>IllegalArgumentException</i> is thrown if an illegal type is specified.
     * @param name field name
     * @param type field type 
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return a <i>Field</i> interface for the newly created object.
     */
    public static Field createField(String name,
    Type type,Property[] property)
    {
        if(!type.isScalar()) throw new IllegalArgumentException(
                "Illegal PVType. Must be scalar but it is " + type.toString() );
        return new FieldInstance(name,type,property);
    } 
    
    /**
     * Create a <i>Property</i>
     * @param name the property name
     * @param field name of the associated field
     * @return a <i>Property<i/> interface for the newly created object.
     */
    public static Property createProperty(String name, Field field) {
        return new PropertyInstance(name,field);
    } 

    /**
     * Create a <i>Structure</i>
     * @param name The field name
     * @param structureName The structure name
     * @param field The array of <i>Field</i> that the structure contains.   
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return a <i>Structure</i> interface for the newly created object.
     */
    public static Structure createStructureField(String name,
    String structureName, Field[] field,Property[] property)
    {
        return new StructureInstance(name,structureName,field,property);
    } 

    static private class ArrayInstance extends FieldInstance implements Array {
        Type elementType;
    
        ArrayInstance(String name, Type elementType, Property[] property) {
            super(name, Type.pvArray,property);
            this.elementType = elementType;
        }
    
        public Type getElementType() {
            return elementType;
        }
    }
    
    static private class EnumInstance extends FieldInstance implements Enum{
    
        private boolean choicesMutable;
        EnumInstance(String name, boolean choicesMutable, Property[] property) {
            super(name, Type.pvEnum, property);
            this.choicesMutable = choicesMutable;
        }
        public boolean isChoicesMutable() {
            return choicesMutable;
        }
    }
    
    static private class FieldInstance implements Field {
        protected boolean isConstant;
        protected String name;
        protected Property[] property;
        protected Type type;
    
        FieldInstance(String name, Type type,Property[] property) {
            this.name = name;
            this.type = type;
            this.property = property;
            isConstant = false;
        }
    
        public String getName() {
            return(name);
        }
    
        public Property[] getPropertys() {
            return property;
        }
    
        public Property getProperty(String propertyName) {
            for(int i=0; i<property.length; i++) {
                if(property[i].getName().equals(propertyName)) return property[i];
            }
            return null;
        }
    
        public Type getType() {
            return type;
        }
    
        public boolean isConstant() {
            return(isConstant);
        }
    
        public void setConstant(boolean value) {
            isConstant = value;
            
        }
    }
    
    static private class PropertyInstance implements Property {
        private Field field;
        private String name;
    
        PropertyInstance(String name, Field field) {
            this.name = name;
            this.field = field;
        }
    
        public Field getField() { return field;}
        public String getName() { return name;}
    }
    
    static private class StructureInstance extends FieldInstance
        implements Structure
    {
        Field[] field;
        String[] fieldName;
        String structureName;
    
        StructureInstance(String name, String structureName,
            Field[] field, Property[] property)
        {
            super(name, Type.pvStructure,property);
            this.field = field;
            this.structureName = structureName;
            fieldName = new String[field.length];
            for(int i = 0; i <field.length; i++) {
                fieldName[i] = field[i].getName();
            }
       }
    
        public Field[] getField() {
            return field;
        }
    
        public Field getField(String name) {
            for(int i=0; i< fieldName.length; i++) {
                if(name.equals(fieldName[i])) return field[i];
            }
            return null;
        }
    
        public String[] getFieldNames() {
            return fieldName;
        }
    
        public Field[] getFields() {
            return field;
        }

        public String getStructureName() {
            return structureName;
        }
    }

}
