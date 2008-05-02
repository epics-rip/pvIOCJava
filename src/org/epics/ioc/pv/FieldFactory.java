/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

import java.util.Map;


/**
 * FieldFactory creates Field instances.
 * User code creates introspection objects via FieldCreate,
 * which is obtained via a call to <i>FieldFactory.getFieldCreate</i>.
 * This is a complete factory for the <i>PV</i> reflection.
 * Most <i>PV</i> database implementations should find this sufficient for
 * <i>PV</i> reflection.
 * @author mrk
 *
 */


public final class FieldFactory {   
    private FieldFactory(){} // don't create
    private static FieldCreateImpl fieldCreate = new FieldCreateImpl(); 
    /**
     * Get the FieldCreate interface.
     * @return The interface for creating introspection objects.
     */
    public static FieldCreate getFieldCreate() {
        return fieldCreate;
    }
    
    private static final class FieldCreateImpl implements FieldCreate{
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
        public Array createArray(String fieldName, Type elementType,FieldAttribute fieldAttribute)
        {
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new BaseArray(fieldName,elementType,fieldAttribute);
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createArrayField(java.lang.String, org.epics.ioc.pv.Type)
         */
        public Array createArray(String fieldName, Type elementType) {
            return createArray(fieldName,elementType,null);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createField(java.lang.String, org.epics.ioc.pv.Type, org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Field createField(String fieldName, Type type,FieldAttribute fieldAttribute)
        {
            if(!type.isScalar()) throw new IllegalArgumentException(
                    "Illegal PVType. Must be scalar but it is " + type.toString() );
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new BaseField(fieldName,type,fieldAttribute);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createField(java.lang.String, org.epics.ioc.pv.Type)
         */
        public Field createField(String fieldName, Type type) {
            return createField(fieldName,type,null);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createFieldAttribute()
         */
        public FieldAttribute createFieldAttribute() {
            return new BaseFieldAttribute();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createStructureField(java.lang.String, java.lang.String, org.epics.ioc.pv.Field[], org.epics.ioc.pv.Property[], org.epics.ioc.pv.FieldAttribute)
         */
        public Structure createStructure(String fieldName, String structureName, Field[] field,
            FieldAttribute fieldAttribute)
        {
            if(fieldAttribute==null) fieldAttribute = createFieldAttribute();
            return new BaseStructure(fieldName,structureName,field,fieldAttribute);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.FieldCreate#createStructureField(java.lang.String, java.lang.String, org.epics.ioc.pv.Field[])
         */
        public Structure createStructure(String fieldName, String structureName, Field[] field) {
            return createStructure(fieldName,structureName,field,null);
        }
                
    }
}
