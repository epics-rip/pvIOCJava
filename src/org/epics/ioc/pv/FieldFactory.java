/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * FieldFactory creates Field instances.
 * User code creates introspection objects via the FieldCreate,
 * which is obtained via a call to <i>FieldFactory.getFieldCreate</i>.
 * This is a complete factory for the <i>PV</i> reflection.
 * Most <i>PV</i> database implementations should find this sufficient for
 * <i>PV</i> reflection.
 * @author mrk
 *
 */


public final class FieldFactory {
    private static Convert convert = ConvertFactory.getConvert();
    
    private FieldFactory(){} // dont create
    
    /**
     * Get the FieldCreate interface.
     * @return The interface for creating introspection objects.
     */
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
            if(value.equals("structure")) return Type.pvStructure;
            if(value.equals("array")) return Type.pvArray;
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
            return new BaseArray(fieldName,elementType,property,fieldAttribute);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createArrayField(java.lang.String, org.epics.ioc.pv.Type)
         */
        public Array createArray(String fieldName, Type elementType) {
            return createArray(fieldName,elementType,null,null);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createField(java.lang.String, org.epics.ioc.pv.Type, org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Field createField(String fieldName, Type type,
             Property[] property, FieldAttribute fieldAttribute)
        {
            if(!type.isScalar()) throw new IllegalArgumentException(
                    "Illegal PVType. Must be scalar but it is " + type.toString() );
            if(property==null) property = new Property[0];
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new BaseField(fieldName,type,property,fieldAttribute);
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
            return new FieldAttributeImpl();
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
            return new BaseStructure(fieldName,structureName,field,property,fieldAttribute);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createStructureField(java.lang.String, java.lang.String, org.epics.ioc.pv.Field[])
         */
        public Structure createStructure(String fieldName, String structureName, Field[] field) {
            return createStructure(fieldName,structureName,field,null,null);
        }
                
        private static class FieldAttributeImpl implements FieldAttribute {
            private TreeMap<String,String> attributeMap = new TreeMap<String,String>();
            private FieldAttributeImpl(){}

            /* (non-Javadoc)
             * @see org.epics.ioc.pv.FieldAttribute#getAttribute(java.lang.String)
             */
            public synchronized String getAttribute(String key) {
                return attributeMap.get(key);
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.pv.FieldAttribute#getAttributes()
             */
            public synchronized Map<String, String> getAttributes() {
                return attributeMap;
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.pv.FieldAttribute#setAttribute(java.lang.String, java.lang.String)
             */
            public synchronized String setAttribute(String key, String value) {
                return attributeMap.put(key, value);
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.pv.FieldAttribute#setAttributes(java.util.Map, java.lang.String[])
             */
            public synchronized void setAttributes(Map<String, String> attributes, String[] exclude) {
                Set<String> keys;
                keys = attributes.keySet();
                outer:
                for(String key: keys) {
                     for(String excludeKey: exclude) {
                         if(excludeKey.equals(key)) continue outer;
                     }
                    attributeMap.put(key,attributes.get(key));
                }
            }

            /* (non-Javadoc)
             * @see org.epics.ioc.pv.FieldAttribute#toString(int)
             */
            public synchronized String toString(int indentLevel) {
                String result = "";
                Set<String> keys = attributeMap.keySet();
                for(String key : keys) {
                    result += " " + key + "=" + attributeMap.get(key);
                }
                return result;
            }
        }
        
        private static class PropertyInstance implements Property {
            private String fieldName;
            private String name;
        
            private PropertyInstance(String name, String fieldName) {
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
                convert.newLine(builder,indentLevel);
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
