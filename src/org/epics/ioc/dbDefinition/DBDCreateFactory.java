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
     * creates a DBDMenu.
     * @param menuName name of the menu.
     * @param choices for the menu.
     * @return the menu or null if it already existed.
     */
    public static DBDMenu createMenu(String menuName, String[] choices)
    {
        return new MenuInstance(menuName,choices);
    }

    /**
     * create a DBDStructure.
     * @param name name of the structure.
     * @param dbdField an array of DBDField for the fields of the structure.
     * @param property an array of properties for the structure.
     * @return interface for the newly created structure.
     */
    public static DBDStructure createStructure(String name,
        DBDField[] dbdField,Property[] property)
    {
        return new StructureInstance(name,dbdField,property);
    }
    
    /**
     * create a DBDRecordType.
     * @param name the recordType name.
     * @param dbdField an array of DBDField for the fields of the structure.
     * @param property an array of properties for the structure.
     * @return interface for the newly created structure.
     */
    public static DBDRecordType createRecordType(String name,
        DBDField[] dbdField,Property[] property)
    {
        return new RecordTypeInstance(name,dbdField,property);
    }

    /**
     * create a DBDLinkSupport.
     * @param supportName name of the link support.
     * @param configStructureName name of the configuration structure.
     * @return the DBDLinkSupport or null if it already existed.
     */
    public static DBDLinkSupport createLinkSupport(String supportName,
        String configStructureName)
    {
        return new LinkSupportInstance(supportName,configStructureName);
    }
    
    /**
     * creates a DBDField.
     * This is used for all DBTypes.
     * @param attribute the DBDAttribute interface for the field.
     * @param property an array of properties for the field.
     * @return interface for the newly created field.
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
            return new LinkFieldInstance(attribute,property);
        }
        throw new IllegalStateException("Illegal DBType. Logic error");
    }
    
    /**
     * Create a DBDStructure for link.
     * This is called by DBDFactory.
     * @param dbd the DBD that will have the DBDStructure for link.
     */
    public static void createLinkDBDStructure(DBD dbd) {
        DBDAttributeValues linkSupportValues = new StringValues(
            "linkSupportName");
        DBDAttribute linkSupportAttribute = DBDAttributeFactory.create(
            dbd,linkSupportValues);
        DBDField linkSupport = createField(linkSupportAttribute,null);
        DBDAttributeValues configValues = new StringValues(
            "configStructureName");
        DBDAttribute configAttribute = DBDAttributeFactory.create(
            dbd,configValues);
        DBDField config = createField(configAttribute,null);
        DBDField[] dbdField = new DBDField[]{linkSupport,config};
        DBDStructure link = createStructure("link",dbdField,null);
        dbd.addStructure(link);
    
    }

    private static class StringValues implements DBDAttributeValues {
        String fieldName;
        String[] name = null;
        String[] value = null;
        
        StringValues(String fieldName) {
            name = new String[]{"name","type"};
            value = new String[]{fieldName,"string"};
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDAttributeValues#getLength()
         */
        public int getLength() {
            return name.length;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDAttributeValues#getName(int)
         */
        public String getName(int index) {
            return name[index];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDAttributeValues#getValue(int)
         */
        public String getValue(int index) {
            return value[index];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDAttributeValues#getValue(java.lang.String)
         */
        public String getValue(String attributeName) {
            for(int i=0; i< name.length; i++) {
                if(attributeName.equals(name[i])) return value[i];
            }
            return null;
        }   
    }


   static private void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
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
        private AtomicReference<String> structureSupportName = new AtomicReference<String>();
        
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
         * @see org.epics.ioc.dbDefinition.DBDStructure#getStructureSupportName()
         */
        public String getStructureSupportName() {
            return structureSupportName.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDStructure#setStructureSupportName(java.lang.String)
         */
        public boolean setStructureSupportName(String supportName) {
            return structureSupportName.compareAndSet(null,supportName);
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
            String name = structureSupportName.get();
            if(name!=null) {
                newLine(builder,indentLevel);
                builder.append(String.format(
                    "structureSupportName %s",name));
            }
            return builder.toString();
        }
    }

    static private class RecordTypeInstance extends StructureInstance implements DBDRecordType
    {

        private AtomicReference<String> recordSupportName  = new AtomicReference<String>();
        
        RecordTypeInstance(String name,
            DBDField[] dbdField,Property[] property)
        {
            super(name,dbdField,property);
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDRecordType#getRecordSupportName()
         */
        public String getRecordSupportName() {
            return recordSupportName.get();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDRecordType#setRecordSupportName(java.lang.String)
         */
        public boolean setRecordSupportName(String supportName) {
            return recordSupportName.compareAndSet(null,supportName);
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
            String name = recordSupportName.get();
            if(name!=null) {
                newLine(builder,indentLevel);
                builder.append(String.format(
                    "recordSupportName %s",name));
            }
            return builder.toString();
        }
        
    }
    
    static private class LinkSupportInstance implements DBDLinkSupport
    {
        private String configStructureName;
        private String linkSupportName;

        LinkSupportInstance(String supportName,
            String configStructureName)
        {
            this.configStructureName = configStructureName;
            this.linkSupportName = supportName;
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDLinkSupport#getConfigStructureName()
         */
        public String getConfigStructureName() {
            return configStructureName;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbDefinition.DBDLinkSupport#getLinkSupportName()
         */
        public String getLinkSupportName() {
            return linkSupportName;
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
                    "linkSupportName %s configStructureName %s",
                    linkSupportName,configStructureName));
            return builder.toString();
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
            Property[] structureProperty = dbdStructure.getPropertys();
            StringBuilder builder = new StringBuilder();
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
            builder.append(super.toString(indentLevel));
            String supportName = dbdStructure.getStructureSupportName();
            if(supportName!=null) {
                newLine(builder,indentLevel);
                builder.append("structureSupportName " + supportName);
            }
            return builder.toString();
        }

    }
    
    static private class LinkFieldInstance extends AbstractDBDField
        implements DBDLinkField
    {
        private Structure structure;
        private DBDStructure dbdStructure;

        LinkFieldInstance(DBDAttribute attribute,Property[]property)
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
    }
}
