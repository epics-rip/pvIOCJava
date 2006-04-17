package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.*;

public abstract class AbstractDBDField implements DBDField {
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

    AbstractDBDField(DBDAttribute attribute,Property[]property)
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
        
    protected Field field;
    protected DBDAttribute attribute;
}

