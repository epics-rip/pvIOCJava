/**
 * 
 */
package org.epics.ioc.dbDefinition;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.dbAccess.*;
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
     * creates a DBDField.
     * This must only be used for fields that are of type DBType.dbPvType.
     * @param fieldName name of the field
     * @param pvType the Type
     * @param dbType the DBType
     * @param property an array of properties for the field
     * @return interface for the newly created field
     */
    public static DBDField createDBDField(String fieldName, Type pvType, 
        DBType dbType, Property[]property)
    {
        return new FieldInstance(fieldName,pvType,property);
    }

    /**
     * creates a DBDMenuField
     * This must only be used for menu fields
     * @param fieldName name of the field
     * @param dbdMenu the DBDMenu for the field
     * @param property an array of properties for the field
     * @return interface for the newly created field
     */
    public static DBDMenuField createDBDMenuField(String fieldName,
        DBDMenu dbdMenu, Property[]property)
    {
        return new MenuFieldInstance(fieldName,dbdMenu,property);
    }

    /**
     * creates a DBDStructureField
     * This must only be used for structure fields
     * @param fieldName name of the field
     * @param dbdStructure the DBDStructure that describes the field
     * @param property an array of properties for the field
     * @return interface for the newly created field
     */
    public static DBDStructureField createDBDStructureField(String fieldName,
        DBDStructure dbdStructure, Property[]property)
    {
        return new StructureFieldInstance(fieldName,dbdStructure,property);
    }

    /**
     * creates a DBDArrayField
     * This must only be used for array fields
     * @param fieldName name of the field
     * @param pvType the Type for the field elements
     * @param dbType the DBType for the field elements
     * @param property an array of properties for the field
     * @return interface for the newly created field
     */
    public static DBDArrayField createDBDArrayField(String fieldName,
        Type pvType, DBType dbType, Property[]property)
    {
        return new ArrayFieldInstance(fieldName,pvType,dbType,property);
    }
    
    /**
     * creates a DBDField that describes a link field.
     * This must only be used for a link field.
     * The Type will be pvUnknown and the DBType will be dbLink.
     * @param fieldName name of the field
     * @param property an array of properties for the field
     * @return interface for the newly created field
     */
    public static DBDField createDBDLinkField(String fieldName,Property[]property)
    {
        return new FieldInstance(fieldName,Type.pvUnknown,DBType.dbLink,property);
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

    static private class MenuInstance implements DBDMenu
    {
         public String[] getChoices() {
            return choices;
        }

        public String getName() {
            return menuName;
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("menu %s {    ",menuName));
            for(String value: choices) {
                builder.append(String.format("\"%s\" ",value));
            }
            builder.append("}\n");
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

    static private class FieldInstance implements DBDField, DBDAttribute
    {

        public int getAsl() {
            return asl;
        }

        public String getDefault() {
            return defaultValue;
        }

        public boolean isDesign() {
            return isDesign;
        }

        public boolean isLink() {
            return isLink;
        }

        public boolean isReadonly() {
            return isReadOnly;
        }

        public void setAsl(int value) {
            asl = value;
        }

        public void setDefault(String value) {
            defaultValue = value;
        }

        public void setDesign(boolean value) {
            isDesign = value;
        }

        public void setLink(boolean value) {
            isLink = value;
        }

        public void setReadOnly(boolean value) {
            isReadOnly = value;
        }

        public DBDAttribute getDBDAttribute() {
            return this;
        }

        public DBType getDBType() {
            return dbType;
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

        public boolean isConstant() {
            return field.isConstant();
        }

        public void setConstant(boolean value) {
            field.setConstant(value);
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(field.toString());
            builder.append(String.format(" DBType %s ",dbType.toString()));
            if(defaultValue!=null) builder.append(String.format("default \"%s\"",defaultValue));
            builder.append(String.format(" asl %d design %b link %b readOnly %b",
                    asl,isDesign,isLink,isReadOnly));
            return builder.toString();
        }

        // Use this for dbPvType
        FieldInstance(String fieldName,Type pvType,Property[]property)
        {
            if(pvType==Type.pvEnum) {
                field = FieldFactory.createEnumField(fieldName,true,property);
            } else {
                field = FieldFactory.createField(fieldName,pvType,property);
            }
            this.dbType = DBType.dbPvType;
            initAttribute();
        }
        
        // Use this for dbMenu
        FieldInstance(String fieldName,Property[]property)
        {
            field = FieldFactory.createEnumField(fieldName,false,property);
            dbType = DBType.dbMenu;
            initAttribute();
        }

        // Use this for dbArray
        FieldInstance(String fieldName,Type pvType,DBType dbType,Property[]property)
        {
            assert(dbType==DBType.dbArray);
            field = FieldFactory.createArrayField(fieldName,pvType,property);
            this.dbType = DBType.dbArray;
            initAttribute();
        }
        
        // Use this for dbStructure and for dbLink
        FieldInstance(String fieldName, String structureName, DBDField[] dbdField, Property[]property)
        {
            this.field = (Field)FieldFactory.createStructureField(
                    fieldName,structureName,dbdField,property);
            this.dbType = DBType.dbStructure;
            initAttribute();
        }
        
        private void initAttribute() {
            asl = 1;
            defaultValue = null;
            isDesign = true;
            isLink = false;
            isReadOnly = false;
        }
        protected Field field;
        protected DBType dbType;
        private int asl;
        private String defaultValue;
        private boolean isDesign;
        private boolean isLink;
        private boolean isReadOnly;
    }

    static private class MenuFieldInstance extends FieldInstance
        implements DBDMenuField, Enum
    {
        public boolean isChoicesMutable() {
            return false;
        }

        public DBDMenu getDBDMenu() {
            return dbdMenu;
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append(String.format("choicesMutable false "));
            return builder.toString();
        }

        MenuFieldInstance(String fieldName, DBDMenu dbdMenu, Property[]property)
        {
            super(fieldName,property);
            this.dbdMenu = dbdMenu;
        }
        
        private DBDMenu dbdMenu;
    }
 
    static private class StructureFieldInstance extends FieldInstance
        implements DBDStructureField, Structure
    {
        public Field getField(String fieldName) {
            Structure structure = (Structure)super.field;
            return structure.getField(fieldName);
        }

        public String[] getFieldNames() {
            Structure structure = (Structure)super.field;
            return structure.getFieldNames();
        }

        public Field[] getFields() {
            Structure structure = (Structure)super.field;
            return structure.getFields();
        }

        public String getStructureName() {
            Structure structure = (Structure)super.field;
            return structure.getStructureName();
        }

        public DBDStructure getDBDStructure() {
            return dbdStructure;
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append(dbdStructure.toString());
            return builder.toString();
        }

        StructureFieldInstance(String fieldName,
            DBDStructure dbdStructure, Property[]property)
        {
            super(fieldName,dbdStructure.getStructureName(),
                    dbdStructure.getDBDFields(),property);
            this.dbdStructure = dbdStructure;
        }
        
        private DBDStructure dbdStructure;
    }

    static private class ArrayFieldInstance extends FieldInstance
        implements DBDArrayField, Array
    {
        public Type getElementType() {
            Array array = (Array)super.field;
            return array.getElementType();
        }

        public DBType getElementDBType() {
            return elementType;
        }
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append(String.format("elementType %s ",elementType.toString()));
            return builder.toString();
        }

        ArrayFieldInstance(String fieldName,Type pvType,
            DBType dbType, Property[]property)
        {
            super(fieldName,pvType,DBType.dbStructure,property);
            elementType = dbType;
        }
        
        private DBType elementType;
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

        public boolean isConstant() {
            return structure.isConstant();
        }

        public void setConstant(boolean value) {
            structure.setConstant(value);
        }

        StructureInstance(String name,
            DBDField[] dbdField,Property[] property)
        {
            structure = FieldFactory.createStructureField(name,name,dbdField,property);
            this.dbdField = dbdField;
        }
                
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("\nas Structure\n%s",structure.toString()));
            builder.append(String.format("\nas DBDStructure\n{\n"));
            for(int i=0, n=dbdField.length; i < n; i++) {
                DBDField field = dbdField[i];
                builder.append(String.format("    {name %s  type  %s}\n",field.getName(),field.getDBType().toString()));;
            }
            builder.append(String.format("}\n"));
            return builder.toString();
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
        
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format(
                    "linkSupportName %s configStructName %s\n",
                    linkSupportName,configStructName));
            return builder.toString();
        }

        private String configStructName;
        private String linkSupportName;
    }
}
