/**
 * 
 */
package org.epics.ioc.pvAccess;

import java.util.*;

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
        return new ArrayInstance(name,elementType,property);
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
    boolean choicesMutable,Property[] property) {
        return new EnumInstance(name,choicesMutable,property);
    } 

    /**
     * Create a <i>Field</i>.
     * This must only be called for scalar types,
     * i.e. <i>pvBoolean</i>, ... , <i>pvString</i>
     * For <i>pvEnum</i>, <i>pvArray</i>, and <i>pvStructure</i>
     * the appropriate create method must be called.
     * @param name field name
     * @param type field type 
     * @param property the field properties.
     * The properties must be created before calling this method.
     * <i>null</i> means that the field has no properties.
     * @return a <i>Field</i> interface for the newly created object.
     * @throws <i>IllegalArgumentException</i> if an illegal type is specified.
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
        return new StructureInstance(name,structureName,field,property);
    } 

    private static void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    private static String indentString = "    ";

    private static class ArrayInstance extends FieldInstance implements Array {
        Type elementType;
    
        ArrayInstance(String name, Type elementType, Property[] property) {
            super(name, Type.pvArray,property);
            this.elementType = elementType;
        }
    
        public Type getElementType() {
            return elementType;
        }


        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append(super.toString(indentLevel));
            newLine(builder,indentLevel);
            builder.append(String.format("elementType %s ",
                elementType.toString()));
            return builder.toString();
        }

    }
    
    private static class EnumInstance extends FieldInstance implements Enum{
    
        private boolean choicesMutable;
        EnumInstance(String name, boolean choicesMutable, Property[] property) {
            super(name, Type.pvEnum, property);
            this.choicesMutable = choicesMutable;
        }
        public boolean isChoicesMutable() {
            return choicesMutable;
        }



        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append(super.toString(indentLevel));
            newLine(builder,indentLevel);
            builder.append(String.format("choicesMutable %b ",
                 choicesMutable));
            return builder.toString();
        }
    }
    
    private static class FieldInstance implements Field {
        protected boolean isMutable;
        protected String name;
        protected Property[] property;
        protected Type type;
    
        FieldInstance(String name, Type type,Property[] property) {
            this.name = name;
            this.type = type;
            if(property==null) property = new Property[0];
            this.property = property;
            isMutable = true;
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
    
        public boolean isMutable() {
            return(isMutable);
        }
    
        public void setMutable(boolean value) {
            isMutable = value;
            
        }

        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append(String.format("field %s type %s isMutable %b ",
                    name,type.toString(),isMutable));
            if(property.length>0) {
                newLine(builder,indentLevel);
                builder.append("property{");
                for(Property prop : property) {
                    builder.append(prop.toString(indentLevel + 1));
                }
                newLine(builder,indentLevel);
                builder.append("}");
            }
            return builder.toString();
        }
    }
    
    private static class PropertyInstance implements Property {
        private String fieldName;
        private String name;
    
        PropertyInstance(String name, String fieldName) {
            this.name = name;
            this.fieldName = fieldName;
        }

        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append(String.format("{name = %s field = %s}",
                    name,fieldName));
            return builder.toString();
        }
    
        public String getFieldName() { return fieldName;}
        public String getName() { return name;}
    }
    
    private static class StructureInstance extends FieldInstance
        implements Structure
    {
        Field[] field;
        String[] fieldName;
        String structureName;
        List<String> sortedFieldNameList;
        int[] fieldIndex;
        
        StructureInstance(String name, String structureName,
            Field[] field, Property[] property)
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
            fieldIndex = new int[field.length];
            for(int i=0; i<field.length; i++) {
                String value = sortedFieldNameList.get(i);
                for(int j=0; j<field.length; j++) {
                    if(value.equals(fieldName[j])) {
                        fieldIndex[i] = j;
                    }
                }
            }
       }
    
        public Field[] getField() {
            return field;
        }
    
        public Field getField(String name) {
            int i = Collections.binarySearch(sortedFieldNameList,name);
            if(i>=0) {
                return field[fieldIndex[i]];
            }
            return null;
        }

        public int getFieldIndex(String name) {
            int i = Collections.binarySearch(sortedFieldNameList,name);
            if(i>=0) return fieldIndex[i];
            return -1;
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

        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            if(indentLevel!=0) {
                builder.append(super.toString(indentLevel));
            } else {
                newLine(builder,indentLevel);
                builder.append(String.format("%s isMutable %b",
                    name,isMutable));
                if(property.length>0) {
                    newLine(builder,indentLevel);
                    builder.append("property{");
                    for(Property prop : property) {
                        builder.append(prop.toString(indentLevel + 1));
                    }
                    newLine(builder,indentLevel);
                    builder.append("}");
                }
            }
            newLine(builder,indentLevel);
            builder.append(String.format("structure %s {",
                structureName));
            for(int i=0, n= field.length; i < n; i++) {
                builder.append(field[i].toString(indentLevel + 1));
                if(i<n-1) newLine(builder,indentLevel + 1);
            }
            newLine(builder,indentLevel);
            builder.append("}");
            return builder.toString();
        }

    }
}
