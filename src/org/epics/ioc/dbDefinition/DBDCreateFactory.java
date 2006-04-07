/**
 * 
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;

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
     * creates a DBDField.
     * @param attribute the SBDAttribute interface for the field
     * @param property an array of properties for the field
     * @return interface for the newly created field
     */
    public static DBDField createDBDField(DBDAttribute attribute, Property[]property)
    {
        return new FieldInstance(attribute,property);
    }
    

    /**
     * create a DBDStructure.
     * This can either be a structure or a recordType
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
     * Create a DBDStructure for link.
     * This is called by DBDFactory
     * @param dbd the DBD that will have the DBDStructure for link
     */
    public static void createLinkDBDStructure(DBD dbd) {
        DBDAttributeValues configValues = new StringValues("configStructName");
        DBDAttribute configAttribute = DBDAttributeFactory.create(
            dbd,configValues);
        DBDAttributeValues linkSupportValues = new StringValues(
            "linkSupportName");
        DBDAttribute linkSupportAttribute = DBDAttributeFactory.create(
            dbd,linkSupportValues);
        DBDField config = createDBDField(configAttribute,null);
        DBDField linkSupport = createDBDField(linkSupportAttribute,null);
        DBDField[] dbdField = new DBDField[]{config,linkSupport};
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

    static private class FieldInstance implements DBDField
    {

        public DBDAttribute getDBDAttribute() {
            return attribute;
        }

        public DBType getDBType() {
            return attribute.getDBType();
        }

        public String getName() {
            return field.getName();
        }

        public Property getProperty(String propertyName) {
            return field.getProperty(propertyName);
        }

        public Property[] getPropertys() {
            return field.getPropertys();
        }

        public Type getType() {
            return field.getType();
        }

        public boolean isMutable() {
            return field.isMutable();
        }

        public void setMutable(boolean value) {
            field.setMutable(value);
        }
        
        
        public String toString() { return getString(0);}

        public String toString(int indentLevel) {
            return getString(indentLevel);
        }

        private String getString(int indentLevel) {
            StringBuilder builder = new StringBuilder();
            builder.append(field.toString(indentLevel));
            builder.append(attribute.toString(indentLevel));
            return builder.toString();
        }

        FieldInstance(DBDAttribute attribute,Property[]property)
        {
            this.attribute = attribute;
            DBType dbType = attribute.getDBType();
            Type type = attribute.getType();
            String fieldName = attribute.getName();
            switch(dbType) {
            case dbPvType:
                if(type==Type.pvEnum) {
                    field = FieldFactory.createEnumField(fieldName,true,property); 
                } else {
                    field = FieldFactory.createField(fieldName,type,property);
                }
                break;
            case dbMenu:
                field = FieldFactory.createEnumField(fieldName,false,property);
                break;
            case dbStructure:
            case dbLink: {
                DBDStructure dbdStructure = attribute.getDBDStructure();
                assert(dbdStructure!=null);
                DBDField[] dbdField = dbdStructure.getDBDFields();
                assert(dbdField!=null);
                field = FieldFactory.createStructureField(fieldName,
                    dbdStructure.getName(),dbdField,property);
                break;
            }
            case dbArray:
                field = FieldFactory.createArrayField(fieldName,
                    attribute.getElementType(),property);
               break;
            }
        }
            
        private Field field;
        private DBDAttribute attribute;
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
}
