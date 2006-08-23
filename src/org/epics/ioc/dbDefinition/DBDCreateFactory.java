/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import java.util.concurrent.atomic.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.pvAccess.Enum;

/**
 * Factory that creates an implementation of the various
 * Database Definition interfaces.
 * @author mrk
 *
 */
public final class  DBDCreateFactory {

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
        DBDField[] dbdField,Property[] property)
    {
        return new StructureInstance(name,dbdField,property);
    }
    
    /**
     * Create a DBDRecordType.
     * @param name The recordType name.
     * @param dbdField An array of DBDField for the fields of the structure.
     * @param property An array of properties for the structure.
     * @return interface The for the newly created structure.
     */
    public static DBDRecordType createRecordType(String name,
        DBDField[] dbdField,Property[] property)
    {
        return new RecordTypeInstance(name,dbdField,property);
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
    
    /**
     * Creates a DBDField.
     * This is used for all DBTypes.
     * @param attribute The DBDAttribute interface for the field.
     * @param property An array of properties for the field.
     * @return The interface for the newly created field.
     */
    public static DBDField createField(DBDAttribute attribute, Property[]property)
    {
        DBType dbType = attribute.getDBType();
        Type type = attribute.getType();
        switch(dbType) {
        case dbPvType:
            if(type==Type.pvEnum) {
                return new EnumFieldInstance(attribute,property);
            } else {
                return new FieldInstance(attribute,property);
            }
        case dbMenu:
            return new MenuFieldInstance(attribute,property);
        case dbArray:
            return new ArrayFieldInstance(attribute,property);
        case dbStructure:
            return new StructureFieldInstance(attribute,property);
        case dbLink:
            return new FieldInstance(attribute,property);
        }
        throw new IllegalStateException("Illegal DBType. Logic error");
    }

   static private void newLine(StringBuilder builder, int indentLevel) {
        builder.append(String.format("%n"));
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    static private String indentString = "    ";

    static private class MenuInstance implements DBDMenu
    {

        private String menuName;
        private String[] choices;

        MenuInstance(String menuName, String[] choices)
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
            newLine(builder,indentLevel);
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
        protected Structure structure;
        protected DBDField[] dbdField;
        protected String supportName = null;
        
        public String getSupportName() {
            return supportName;
        }
        public void setSupportName(String name) {
            supportName = name;
            
        }
        StructureInstance(String name,
            DBDField[] dbdField,Property[] property)
        {
            structure = FieldFactory.createStructureField(
                name,name,dbdField,property);
            this.dbdField = dbdField;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDField#getAttribute()
         */
        public DBDAttribute getAttribute() {
            return null; // structures have no attributes
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
                newLine(builder,indentLevel);
                builder.append(String.format(
                    "supportName %s",supportName));
            }
            return builder.toString();
        }
    }

    static private class RecordTypeInstance extends StructureInstance implements DBDRecordType
    {

        private AtomicReference<String> supportName  = new AtomicReference<String>();
        
        RecordTypeInstance(String name,
            DBDField[] dbdField,Property[] property)
        {
            super(name,dbdField,property);
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
                newLine(builder,indentLevel);
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

        SupportInstance(String supportName,
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
        /**
         * @param indentLevel
         * @return
         */
        public String toString(int indentLevel) {
            return getString(indentLevel);
        }
        
        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append(String.format(
                    "supportName %s configurationStructureName %s factoryName %s",
                    supportName,configurationStructureName,factoryName));
            return builder.toString();
        }

        public String getFactoryName() {
            return factoryName;
        }
    }
    
    static private class FieldInstance extends AbstractDBDField
    {
        FieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property); 
        }
    }
    
    static private class EnumFieldInstance extends AbstractDBDField
        implements DBDEnumField
    {
        private Enum enumField;

        EnumFieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property);
            enumField = (Enum)field;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Enum#isChoicesMutable()
         */
        public boolean isChoicesMutable() {
            return enumField.isChoicesMutable();
        }
    }
    
    static private class MenuFieldInstance extends AbstractDBDField
    implements DBDMenuField
    {
        private Enum enumField;

        MenuFieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property);
            enumField = (Enum)field;
        }
    
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Enum#isChoicesMutable()
         */
        public boolean isChoicesMutable() {
            return enumField.isChoicesMutable();
        }
    }
    
    static private class ArrayFieldInstance extends AbstractDBDField
        implements DBDArrayField
    {
        private Array array;

        ArrayFieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property);
            array = (Array)field;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Array#getElementType()
         */
        public Type getElementType() {
            return array.getElementType();
        }

    }
    
    static private class StructureFieldInstance extends AbstractDBDField
    implements DBDStructureField
    {
        private Structure structure;
        private DBDStructure dbdStructure;

        StructureFieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property);
            structure = (Structure)field;
            dbdStructure = attribute.getStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDStructureField#getDBDStructure()
         */
        public DBDStructure getDBDStructure() {
            return dbdStructure;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pvAccess.Structure#getFieldIndex(java.lang.String)
         */
        public int getFieldIndex(String fieldName) {
            return structure.getFieldIndex(fieldName);
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
            if(dbdStructure != null) {
                Property[] structureProperty = dbdStructure.getPropertys();
                if(structureProperty.length>0) {
                    newLine(builder,indentLevel);
                    builder.append(String.format("field %s is structure with property {",
                            field.getName()));
                    for(Property property : structureProperty) {
                        newLine(builder,indentLevel+1);
                        builder.append(String.format("{name = %s field = %s}",
                            property.getName(),property.getFieldName()));
                    }
                    newLine(builder,indentLevel);
                    builder.append("}");
                }
            }
            builder.append(super.toString(indentLevel));
            return builder.toString();
        }

    }
    
}
