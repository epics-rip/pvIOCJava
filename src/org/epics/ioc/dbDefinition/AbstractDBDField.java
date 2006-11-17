/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.*;

/**
 * abstract class for implementing a DBDField interface or extension.
 * @author mrk
 *
 */
public class AbstractDBDField extends AbstractField implements DBDField {
    private DBType dbType;
    private DBDFieldAttribute attribute;
    
    /**
     * Constructor for AbstractDBDField.
     * @param name The field name.
     * @param type The field Type.
     * @param dbType The field DBType.
     * @param property The field properties.
     * @param attribute The attributes for the field.
     */
    public AbstractDBDField(String name, Type type,DBType dbType,
            Property[] property,DBDFieldAttribute attribute)
    {
        super(name,type,property);
        this.dbType = dbType;
        this.attribute = attribute;
    }       
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getDBType()
     */
    public DBType getDBType() {
        return dbType;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getFieldAttribute()
     */
    public DBDFieldAttribute getFieldAttribute() {
        return attribute;
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
        newLine(builder,indentLevel);
        builder.append("DBType " + dbType.toString());
        builder.append(attribute.toString(indentLevel));
        return builder.toString();
    }
   
}

