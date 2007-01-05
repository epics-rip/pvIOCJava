/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.Map;


/**
 * FieldFactory creates Field instances.
 * This is a complete factory for the <i>PV</i> reflection.
 * Most <i>PV</i> database implementations should find this sufficient for
 * <i>PV</i> reflection.
 * @author mrk
 *
 */


public final class FieldFactory {
    
    private FieldFactory(){} // dont create
    
    public static FieldCreate getFieldCreate() {
        return FieldCreateImpl.getFieldCreate();
    }
    
    private static final class FieldCreateImpl implements FieldCreate{
        private static FieldCreateImpl singleImplementation = null;
        private static synchronized FieldCreate getFieldCreate() {
                if (singleImplementation==null) {
                    singleImplementation = new FieldCreateImpl();
                }
                return singleImplementation;
        }
        // Guarantee that ImplementConvert can only be created via getFieldCreate
        private FieldCreateImpl() {}
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#getElementType(java.util.Map)
         */
        public Type getElementType(Map<String, String> attributes) {
            return getType(attributes.get("elementType"));
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#getType(java.util.Map)
         */
        public Type getType(Map<String, String> attributes) {
            return getType(attributes.get("type"));
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#getType(java.lang.String)
         */
        public Type getType(String value) {
            if(value==null)  return null;
            if(value.equals("boolean")) return Type.pvBoolean;
            if(value.equals("byte")) return Type.pvByte;
            if(value.equals("short")) return Type.pvShort;
            if(value.equals("int")) return Type.pvInt;
            if(value.equals("long")) return Type.pvLong;
            if(value.equals("float")) return Type.pvFloat;
            if(value.equals("double")) return Type.pvDouble;
            if(value.equals("string")) return Type.pvString;
            if(value.equals("enum")) return Type.pvEnum;
            if(value.equals("menu")) return Type.pvMenu;
            if(value.equals("structure")) return Type.pvStructure;
            if(value.equals("array")) return Type.pvArray;
            if(value.equals("link")) return Type.pvLink;
            return null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createArrayField(java.lang.String, org.epics.ioc.pv.Type, org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Array createArray(String fieldName, Type elementType,
                Property[] property, FieldAttribute fieldAttribute)
        {
            if(property==null) property = new Property[0];
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new ArrayBase(fieldName,elementType,property,fieldAttribute);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createArrayField(java.lang.String, org.epics.ioc.pv.Type)
         */
        public Array createArray(String fieldName, Type elementType) {
            return createArray(fieldName,elementType,null,null);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createEnumField(java.lang.String, boolean, org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Enum createEnum(String fieldName, boolean choicesMutable,
            Property[] property, FieldAttribute fieldAttribute)
        {
            if(property==null) property = new Property[0];
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new EnumBase(fieldName,choicesMutable,property,fieldAttribute);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createEnumField(java.lang.String, boolean)
         */
        public Enum createEnum(String fieldName, boolean choicesMutable) {
            return createEnum(fieldName,choicesMutable,null,null);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createField(java.lang.String, org.epics.ioc.pv.Type, org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Field createField(String fieldName, Type type,
             Property[] property, FieldAttribute fieldAttribute)
        {
            if(!type.isScalar() && type!=Type.pvLink) throw new IllegalArgumentException(
                    "Illegal PVType. Must be scalar but it is " + type.toString() );
            if(property==null) property = new Property[0];
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new FieldBase(fieldName,type,property,fieldAttribute);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createField(java.lang.String, org.epics.ioc.pv.Type)
         */
        public Field createField(String fieldName, Type type) {
            return createField(fieldName,type,null,null);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createFieldAttribute()
         */
        public FieldAttribute createFieldAttribute() {
            return new FieldAttributeImpl(1,null,true,false,false);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createFieldAttribute(java.util.Map)
         */
        public FieldAttribute createFieldAttribute(Map<String, String> attributes) {
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

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createMenuField(java.lang.String, java.lang.String, org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Menu createMenu(String fieldName, String menuName,
        Property[] property, FieldAttribute fieldAttribute)
        {
            if(property==null) property = new Property[0];
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new MenuBase(fieldName,menuName,property,fieldAttribute);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createMenuField(java.lang.String, java.lang.String)
         */
        public Menu createMenu(String fieldName, String menuName) {
            return createMenu(fieldName,menuName,null,null);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createProperty(java.lang.String, java.lang.String)
         */
        public Property createProperty(String propertyName, String fieldName) {
            return new PropertyInstance(propertyName,fieldName);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createStructureField(java.lang.String, java.lang.String, org.epics.ioc.pv.Field[], org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Structure createStructure(String fieldName, String structureName, Field[] field,
            Property[] property, FieldAttribute fieldAttribute)
        {
            if(property==null) property = new Property[0];
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new StructureBase(fieldName,structureName,field,property,fieldAttribute);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createStructureField(java.lang.String, java.lang.String, org.epics.ioc.pv.Field[])
         */
        public Structure createStructure(String fieldName, String structureName, Field[] field) {
            return createStructure(fieldName,structureName,field,null,null);
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
             * @see org.epics.ioc.pv.FieldAttribute#isReadOnly()
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
                FieldBase.newLine(builder,indentLevel);
                builder.append(String.format("{name = %s field = %s}",
                        name,fieldName));
                return builder.toString();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.Property#getFieldName()
             */
            public String getAssociatedFieldName() { return fieldName;} 
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.Property#getPropertyName()
             */
            public String getPropertyName() { return name;}
        }
    }
}
