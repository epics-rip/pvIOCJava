/**
 * 
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.pvAccess.Enum;

/**
 * Creates an implementation of the various DBD interfaces
 * @author mrk
 *
 */
public final class  DBDCreateFactory {

    /**
     * creates a DBDMenu
     * @param menuName name of the menu
     * @param choices for the menu
     * @return the menu or null if it already existed
     */
    public static DBDMenu createDBDMenu(String menuName, String[] choices)
    {
        return new MenuInstance(menuName,choices);
    }

    /**
     * create a DBDStructure.
     * @param name name of the structure
     * @param dbdField an array of DBDField for the fields of the structure
     * @param property an array of properties for the structure
     * @return interface for the newly created structure
     */
    public static DBDStructure createDBDStructure(String name,
        DBDField[] dbdField,Property[] property)
    {
        return new StructureInstance(name,dbdField,property);
    }
    
    /**
     * create a DBDRecordType.
     * This can either be a structure or a recordType
     * @param name the recordType name
     * @param dbdField an array of DBDField for the fields of the structure
     * @param property an array of properties for the structure
     * @return interface for the newly created structure
     */
    public static DBDRecordType createDBDRecordType(String name,
        DBDField[] dbdField,Property[] property)
    {
        return new RecordTypeInstance(name,dbdField,property);
    }

    /**
     * create a DBDLinkSupport
     * @param supportName name of the link support
     * @param configStructName name of the configuration structure
     * @return the DBDLinkSupport or null if it already existed
     */
    public static DBDLinkSupport createDBDLinkSupport(String supportName,
        String configStructName)
    {
        return new LinkSupportInstance(supportName,configStructName);
    }
    
    /**
     * creates a DBDField.
     * This is used for all DBTypes.
     * @param attribute the DBDAttribute interface for the field
     * @param property an array of properties for the field
     * @return interface for the newly created field
     */
    public static DBDField createDBDField(DBDAttribute attribute, Property[]property)
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
            return new EnumFieldInstance(attribute,property);
        case dbArray:
            return new ArrayFieldInstance(attribute,property);
        case dbStructure:
            return new StructureFieldInstance(attribute,property);
        case dbLink:
            return new StructureFieldInstance(attribute,property);
        }
        throw new IllegalStateException("Illegal DBType. Logic error");
    }
    
    /**
     * Create a DBDStructure for link.
     * This is called by DBDFactory
     * @param dbd the DBD that will have the DBDStructure for link
     */
    public static void createLinkDBDStructure(DBD dbd) {
        DBDAttributeValues linkSupportValues = new StringValues(
            "linkSupportName");
        DBDAttribute linkSupportAttribute = DBDAttributeFactory.create(
            dbd,linkSupportValues);
        DBDField linkSupport = createDBDField(linkSupportAttribute,null);
        DBDAttributeValues configValues = new StringValues(
            "configStructFieldName");
        DBDAttribute configAttribute = DBDAttributeFactory.create(
            dbd,configValues);
        DBDField config = createDBDField(configAttribute,null);
        DBDField[] dbdField = new DBDField[]{linkSupport,config};
        DBDStructure link = createDBDStructure("link",dbdField,null);
        dbd.addStructure(link);
    
    }

    private static class StringValues implements DBDAttributeValues {
      
        public int getLength() {
            return name.length;
        }
        public String getName(int index) {
            return name[index];
        }
        public String getValue(int index) {
            return value[index];
        }
        public String getValue(String attributeName) {
            for(int i=0; i< name.length; i++) {
                if(attributeName.equals(name[i])) return value[i];
            }
            return null;
        }
        StringValues(String fieldName) {
            name = new String[]{"name","type"};
            value = new String[]{fieldName,"string"};
        }
        String fieldName;
        String[] name = null;
        String[] value = null;
    }


   static private void newLine(StringBuilder builder, int indentLevel) {
        builder.append("\n");
        for (int i=0; i <indentLevel; i++) builder.append(indentString);
    }
    static private String indentString = "    ";

    static private class MenuInstance implements DBDMenu
    {
        public String[] getChoices() {
            return choices;
        }

        public String getName() {
            return menuName;
        }
        
        public String toString() { return getString(0);}

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


        MenuInstance(String menuName, String[] choices)
        {
            this.menuName = menuName;
            this.choices = choices;
        } 

        private String menuName;
        private String[] choices;

    }


    static private class StructureInstance implements DBDStructure
    {

        public DBDField[] getDBDFields() {
            return dbdField;
        }

        public Field getField(String fieldName) {
            return structure.getField(fieldName);
        }

        public String[] getFieldNames() {
            return structure.getFieldNames();
        }

        public Field[] getFields() {
            return structure.getFields();
        }

        public String getStructureName() {
            return structure.getStructureName();
        }

        public String getName() {
            return structure.getName();
        }

        public Property getProperty(String propertyName) {
            return structure.getProperty(propertyName);
        }

        public Property[] getPropertys() {
            return structure.getPropertys();
        }

        public Type getType() {
            return structure.getType();
        }

        public boolean isMutable() {
            return structure.isMutable();
        }

        public void setMutable(boolean value) {
            structure.setMutable(value);
        }

        StructureInstance(String name,
            DBDField[] dbdField,Property[] property)
        {
            structure = FieldFactory.createStructureField(
                name,name,dbdField,property);
            this.dbdField = dbdField;
        }
                
        
        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            return structure.toString(indentLevel);
        }

        private Structure structure;
        private DBDField[] dbdField;

    }

    static private class RecordTypeInstance implements DBDRecordType
    {

        public DBDAttribute getDBDAttribute() {
            return null; // record types have no attributes
        }

        public DBType getDBType() {
            return DBType.dbStructure;
        }

        public DBDField[] getDBDFields() {
            return dbdField;
        }

        public Field getField(String fieldName) {
            return structure.getField(fieldName);
        }

        public String[] getFieldNames() {
            return structure.getFieldNames();
        }

        public Field[] getFields() {
            return structure.getFields();
        }

        public String getStructureName() {
            return structure.getStructureName();
        }

        public String getName() {
            return structure.getName();
        }

        public Property getProperty(String propertyName) {
            return structure.getProperty(propertyName);
        }

        public Property[] getPropertys() {
            return structure.getPropertys();
        }

        public Type getType() {
            return structure.getType();
        }

        public boolean isMutable() {
            return structure.isMutable();
        }

        public void setMutable(boolean value) {
            structure.setMutable(value);
        }

        RecordTypeInstance(String name,
            DBDField[] dbdField,Property[] property)
        {
            structure = FieldFactory.createStructureField(
                name,name,dbdField,property);
            this.dbdField = dbdField;
        }
                
        
        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            return structure.toString(indentLevel);
        }

        private Structure structure;
        private DBDField[] dbdField;

    }
    static private class LinkSupportInstance implements DBDLinkSupport
    {

        public String getConfigStructName() {
            return configStructName;
        }

        public String getLinkSupportName() {
            return linkSupportName;
        }

        LinkSupportInstance(String supportName,
            String configStructName)
        {
            this.configStructName = configStructName;
            this.linkSupportName = supportName;
        }
        
        
        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            newLine(builder,indentLevel);
            builder.append(String.format(
                    "linkSupportName %s configStructName %s",
                    linkSupportName,configStructName));
            return builder.toString();
        }

        private String configStructName;
        private String linkSupportName;
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
        public boolean isChoicesMutable() {
            return enumField.isChoicesMutable();
        }

        EnumFieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property);
            enumField = (Enum)field;
        }
        
        private Enum enumField;
    }
    
    static private class ArrayFieldInstance extends AbstractDBDField
        implements DBDArrayField
    {
        
        public Type getElementType() {
            return array.getElementType();
        }

        ArrayFieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property);
            array = (Array)field;
        }
        
        private Array array;
        
    }
    
    static private class StructureFieldInstance extends AbstractDBDField
        implements DBDStructureField
    {

        public Field getField(String fieldName) {
            return structure.getField(fieldName);
        }

        public String[] getFieldNames() {
            return structure.getFieldNames();
        }

        public Field[] getFields() {
            return structure.getFields();
        }

        public String getStructureName() {
            return structure.getStructureName();
        }

        StructureFieldInstance(DBDAttribute attribute,Property[]property)
        {
            super(attribute,property);
            structure = (Structure)field;
        }
        
        private Structure structure;
        
    }
}
