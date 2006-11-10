/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import java.util.Map;
import java.util.concurrent.atomic.*;

import org.epics.ioc.pvAccess.*;

/**
 * Factory that creates an implementation of the various
 * Database Definition interfaces.
 * @author mrk
 *
 */
public final class  DBDFieldFactory {
    /**
     * Create a DBDFieldFactory from a map of attrbute values.
     * @param attributes The map of attributes.
     * @return The DBDFieldAttributes.
     */
    public static DBDFieldAttribute createFieldAttribute(Map<String,String> attributes) {
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
        return new FieldAttribute(asl,defaultValue,isDesign,isLink,isReadOnly);
    }
    /**
     * Create default field attributes.
     * @return The DBDFieldAttribute with default values.
     */
    public static DBDFieldAttribute createFieldAttribute() {
        return new FieldAttribute(1,null,true,false,false);
    }
    /**
     * Get the Type from a map of attributes.
     * @param attributes The map of attributes.
     * @return The Type.
     * If the attributes does not have a key "type" the result will be Type.pvUnknown.
     */
    public static Type getType(Map<String,String> attributes) {
        return getType(attributes.get("type"));
    }
    /**
     * Get the element Type from a map of attributes.
     * @param attributes The map of attributes.
     * @return The Type.
     * If the attributes does not have a key "elementType" the result will be Type.pvUnknown.
     */
    public static Type getElementType(Map<String,String> attributes) {
        return getType(attributes.get("elementType"));
    }
    /**
     * Get the Type from string.
     * @param value A string with the name of the type.
     * @return The Type.
     * If the string is null or is not the name of a Type, Type.pvUnknown is returned.
     */
    public static Type getType(String value) {
        if(value==null)  return Type.pvUnknown;
        if(value.equals("boolean")) return Type.pvBoolean;
        if(value.equals("byte")) return Type.pvByte;
        if(value.equals("short")) return Type.pvShort;
        if(value.equals("int")) return Type.pvInt;
        if(value.equals("long")) return Type.pvLong;
        if(value.equals("float")) return Type.pvFloat;
        if(value.equals("double")) return Type.pvDouble;
        if(value.equals("string")) return Type.pvString;
        if(value.equals("enum")) return Type.pvEnum;
        if(value.equals("menu")) return Type.pvEnum;
        if(value.equals("structure")) return Type.pvStructure;
        if(value.equals("array")) return Type.pvArray;
        if(value.equals("link")) return Type.pvUnknown;
        return Type.pvUnknown;
    }
    /**
     * Get the DBType from a map of attributes.
     * @param attributes The map of attributes.
     * @return The DBType.
     * If the attributes does not have a key "type" the result will be DBType.dbPvType.
     */
    public static DBType getDBType(Map<String,String> attributes) {
        return getDBType(attributes.get("type"));
    }
    /**
     * Get the element DBType from a map of attributes.
     * @param attributes The map of attributes.
     * @return The DBType.
     * If the attributes does not have a key "elementType" the result will be DBType.dbPvType.
     */
    public static DBType getElementDBType(Map<String,String> attributes) {
        return getDBType(attributes.get("elementType"));
    }
    /**
     * Get the DBType from string.
     * @param value A string with the name of the type.
     * @return The DBType.
     * If the string is null or is not the name of a DBType DBType.dbPvType will be returned.
     */
    public static DBType getDBType(String value) {
        if(value==null)  return DBType.dbPvType;
        if(value.equals("boolean"))  return DBType.dbPvType;
        if(value.equals("byte"))  return DBType.dbPvType;
        if(value.equals("short"))  return DBType.dbPvType;
        if(value.equals("int")) return DBType.dbPvType;
        if(value.equals("long")) return DBType.dbPvType;
        if(value.equals("float")) return DBType.dbPvType;
        if(value.equals("double")) return DBType.dbPvType;
        if(value.equals("string")) return DBType.dbPvType;
        if(value.equals("enum"))  return DBType.dbPvType;
        if(value.equals("menu")) return DBType.dbMenu;
        if(value.equals("structure")) return DBType.dbStructure;
        if(value.equals("array")) return DBType.dbArray;
        if(value.equals("link")) return DBType.dbLink;
        return DBType.dbPvType;
    }
    /**
     * Create a DBDMenu.
     * @param menuName The name of the menu.
     * @param choices The menu choices.
     * @return The menu or null if it already existed.
     */
    public static DBDMenu createMenu(String menuName, String[] choices)
    {
        return new MenuInstance(menuName,choices);
    }

    /**
     * Create a DBDStructure.
     * @param name The name of the structure.
     * @param dbdField An array of DBDField for the fields of the structure.
     * @param property An array of properties for the structure.
     * @return The interface for the newly created structure.
     */
    public static DBDStructure createStructure(String name,
        DBDField[] dbdField,Property[] property,DBDFieldAttribute attribute)
    {
        return new StructureInstance(name,dbdField,property,attribute);
    }
    
    /**
     * Create a DBDRecordType.
     * @param name The recordType name.
     * @param dbdField An array of DBDField for the fields of the structure.
     * @param property An array of properties for the structure.
     * @return interface The for the newly created structure.
     */
    public static DBDRecordType createRecordType(String name,
        DBDField[] dbdField,Property[] property,DBDFieldAttribute attribute)
    {
        return new RecordTypeInstance(name,dbdField,property,attribute);
    }

    /**
     * Create a DBDSupport.
     * @param supportName The name of the support.
     * @param configurationStructureName The name of the configuration structure.
     * @param factoryName The name of the factory for creating support instances.
     * @return the DBDSupport or null if it already existed.
     */
    public static DBDSupport createSupport(String supportName,
        String configurationStructureName,String factoryName)
    {
        return new SupportInstance(supportName,configurationStructureName,factoryName);
    }
    
    public static DBDMenuField createMenuField(String fieldName,Property[]property,
            DBDFieldAttribute attribute,DBDMenu dbdMenu)
    {
        return new AbstractDBDMenuField(fieldName,property,attribute,dbdMenu);
    }
    public static DBDArrayField createArrayField(String fieldName,Property[]property,
        DBDFieldAttribute attribute,Type elementType,DBType dbElementType)
    {
        return new AbstractDBDArrayField(fieldName,property,attribute,elementType,dbElementType);
    }
    public static DBDStructureField createStructureField(String fieldName,Property[]property,
            DBDFieldAttribute attribute,DBDStructure dbdStructure)
    {
        if(dbdStructure==null) {
            return new AbstractDBDStructureField(fieldName,property,attribute);
        }
        DBDField[] original = dbdStructure.getDBDFields();
        int length = original.length;
        DBDField[] dbdFields = new DBDField[length];
        for(int i=0; i<length; i++) {
            DBDField dbdField = original[i];
            DBDField copy = null;
            String name = dbdField.getName();
            DBDFieldAttribute attr = dbdField.getFieldAttribute();
            Property[] prop = dbdField.getPropertys();
            DBType dbType = dbdField.getDBType();
            switch(dbType) {
            case dbPvType:
            case dbLink:
                Type type = dbdField.getType();
                copy = createField(name,prop,attr,type,dbType);
                break;
            case dbMenu:
                DBDMenu dbdMenu = ((DBDMenuField)dbdField).getMenu();
                copy = createMenuField(name,prop,attr,dbdMenu);
                break;
            case dbArray:
                DBDArrayField arrayField = (DBDArrayField)dbdField;
                Type elementType = arrayField.getElementType();
                DBType elementDBType = arrayField.getDBType();
                copy = createArrayField(name,prop,attr,elementType,elementDBType);
                break;
            case dbStructure:
                DBDStructure structure = ((DBDStructureField)dbdField).getDBDStructure();
                copy = createStructureField(name,prop,attr,structure);
                break;
            }
            if(copy==null) {
                throw new IllegalStateException("logic error");
            }
            dbdFields[i] = copy;
        }
        return new AbstractDBDStructureField(fieldName,property,attribute,dbdFields,dbdStructure);
    }
    /**
     * Creates a DBDField.
     * This is used for all DBTypes.
     * @param attribute The DBDAttribute interface for the field.
     * @param property An array of properties for the field.
     * @return The interface for the newly created field.
     */
    public static DBDField createField(String fieldName,
        Property[]property,DBDFieldAttribute attribute,Type type,DBType dbType)
    {
        if(dbType==DBType.dbPvType || dbType==DBType.dbLink) {
            if(type==Type.pvEnum) {
                return new AbstractDBDEnumField(fieldName,property,attribute); 
            } else {
                return new AbstractDBDField(fieldName,type,dbType,property,attribute);
            }
        }
        throw new IllegalStateException("Illegal types");
    }
    
    private static class FieldAttribute implements DBDFieldAttribute {
        private int asl = 1;
        private String defaultValue = null;
        private boolean isDesign = true;
        private boolean isLink = false;
        private boolean isReadOnly = false;
        
        private FieldAttribute(int asl, String defaultValue,
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
         * @see org.epics.ioc.dbDefinition.DBDFieldAttribute#getAsl()
         */
        public int getAsl() {
            return asl;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDFieldAttribute#getDefault()
         */
        public String getDefault() {
            return defaultValue;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDFieldAttribute#isDesign()
         */
        public boolean isDesign() {
            return isDesign;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDFieldAttribute#isLink()
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
         * @see org.epics.ioc.dbDefinition.DBDFieldAttribute#toString(int)
         */
        public String toString(int indentLevel) {
            return String.format(
                " asl %d design %b link %b readOnly %b",
                asl,isDesign,isLink,isReadOnly);
        }
    }
    static private class MenuInstance implements DBDMenu
    {

        private String menuName;
        private String[] choices;

        private MenuInstance(String menuName, String[] choices)
        {
            this.menuName = menuName;
            this.choices = choices;
        } 
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDMenu#getChoices()
         */
        public String[] getChoices() {
            return choices;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDMenu#getName()
         */
        public String getName() {
            return menuName;
        }        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() { return getString(0);}
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDMenu#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            AbstractField.newLine(builder,indentLevel);
            builder.append(String.format("menu %s { ",menuName));
            for(String value: choices) {
                builder.append(String.format("\"%s\" ",value));
            }
            builder.append("}");
            return builder.toString();
        }
    }
    
    static private class StructureInstance implements DBDStructure
    {
        private DBDFieldAttribute attribute;
        protected Structure structure;
        protected DBDField[] dbdField;
        protected String supportName = null;
        
        public String getSupportName() {
            return supportName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Field#getParent()
         */
        public Field getParent() {
            return null;
        }
        public void setSupportName(String name) {
            supportName = name;
            
        }
        private StructureInstance(String name,
            DBDField[] dbdField,Property[] property,DBDFieldAttribute attribute)
        {
            structure = FieldFactory.createStructureField(
                name,name,dbdField,property);
            this.dbdField = dbdField;
            this.attribute = attribute;
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDField#getFieldAttribute()
         */
        public DBDFieldAttribute getFieldAttribute() {
            return attribute;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDField#getDBType()
         */
        public DBType getDBType() {
            return DBType.dbStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDStructure#getDBDFieldIndex(java.lang.String)
         */
        public int getDBDFieldIndex(String fieldName) {
            return structure.getFieldIndex(fieldName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Structure#getFieldIndex(java.lang.String)
         */
        public int getFieldIndex(String fieldName) {
            return structure.getFieldIndex(fieldName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDStructure#getDBDFields()
         */
        public DBDField[] getDBDFields() {
            return dbdField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Structure#getField(java.lang.String)
         */
        public Field getField(String fieldName) {
            return structure.getField(fieldName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Structure#getFieldNames()
         */
        public String[] getFieldNames() {
            return structure.getFieldNames();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Structure#getFields()
         */
        public Field[] getFields() {
            return structure.getFields();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Structure#getStructureName()
         */
        public String getStructureName() {
            return structure.getStructureName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Field#getName()
         */
        public String getName() {
            return structure.getName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Field#getProperty(java.lang.String)
         */
        public Property getProperty(String propertyName) {
            return structure.getProperty(propertyName);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Field#getPropertys()
         */
        public Property[] getPropertys() {
            return structure.getPropertys();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Field#getType()
         */
        public Type getType() {
            return structure.getType();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Field#isMutable()
         */
        public boolean isMutable() {
            return structure.isMutable();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Field#setMutable(boolean)
         */
        public void setMutable(boolean value) {
            structure.setMutable(value);
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
            builder.append(structure.toString(indentLevel));
            if(supportName!=null) {
                AbstractField.newLine(builder,indentLevel);
                builder.append(String.format(
                    "supportName %s",supportName));
            }
            return builder.toString();
        }
    }

    static private class RecordTypeInstance extends StructureInstance implements DBDRecordType
    {

        private AtomicReference<String> supportName  = new AtomicReference<String>();
        
        private RecordTypeInstance(String name,
            DBDField[] dbdField,Property[] property,DBDFieldAttribute attribute)
        {
            super(name,dbdField,property,attribute);
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
            String name = supportName.get();
            if(name!=null) {
                AbstractField.newLine(builder,indentLevel);
                builder.append(String.format(
                    "supportName %s",name));
            }
            return builder.toString();
        }
        
    }
    
    static private class SupportInstance implements DBDSupport
    {
        private String configurationStructureName;
        private String supportName;
        private String factoryName;

        private SupportInstance(String supportName,
            String configurationStructureName, String factoryName)
        {
            this.configurationStructureName = configurationStructureName;
            this.supportName = supportName;
            this.factoryName = factoryName;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDSupport#getConfigurationStructureName()
         */
        public String getConfigurationStructureName() {
            return configurationStructureName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDSupport#getSupportName()
         */
        public String getSupportName() {
            return supportName;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() { return getString(0);}
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDSupport#toString(int)
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            AbstractField.newLine(builder,indentLevel);
            builder.append(String.format(
                    "supportName %s configurationStructureName %s factoryName %s",
                    supportName,configurationStructureName,factoryName));
            return builder.toString();
        }

        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDSupport#getFactoryName()
         */
        public String getFactoryName() {
            return factoryName;
        }
    }
}
