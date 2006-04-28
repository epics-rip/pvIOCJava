package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.*;

/**
 * abstract class for implementing a DBDField interface or extension.
 * @author mrk
 *
 */
public abstract class AbstractDBDField implements DBDField {
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getDBDAttribute()
     */
    public DBDAttribute getDBDAttribute() {
        return attribute;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getDBType()
     */
    public DBType getDBType() {
        return attribute.getDBType();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Field#getName()
     */
    public String getName() {
        return field.getName();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Field#getProperty(java.lang.String)
     */
    public Property getProperty(String propertyName) {
        return field.getProperty(propertyName);
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Field#getPropertys()
     */
    public Property[] getPropertys() {
        return field.getPropertys();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Field#getType()
     */
    public Type getType() {
        return field.getType();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Field#isMutable()
     */
    public boolean isMutable() {
        return field.isMutable();
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Field#setMutable(boolean)
     */
    public void setMutable(boolean value) {
        field.setMutable(value);
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
        builder.append(field.toString(indentLevel));
        builder.append(attribute.toString(indentLevel));
        return builder.toString();
    }

    /**
     * AbstractDBDField constructor
     * @param attribute attribute for field. This must be created first.
     * @param property property array. It can be null.
     */
    public AbstractDBDField(DBDAttribute attribute,Property[]property)
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

