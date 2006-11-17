/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbDefinition;

import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public class AbstractDBDArrayField extends AbstractArray implements DBDArrayField {
    private DBDFieldAttribute attribute;
    private DBType elementDBType;
    
    /**
     * Constructor for AbstractDBDArrayField.
     * @param name The field name.
     * @param property The field properties.
     * @param attribute The attributes for the field.
     * @param elementType The Type for the array elements.
     * @param elementDBType The DBType for the array elements.
     */
    public AbstractDBDArrayField(String name,Property[] property,
        DBDFieldAttribute attribute,Type elementType,DBType elementDBType)
    {
        super(name,property,elementType);
        this.attribute = attribute;
        this.elementDBType = elementDBType;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getDBType()
     */
    public DBType getDBType() {
        return DBType.dbArray;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getFieldAttribute()
     */
    public DBDFieldAttribute getFieldAttribute() {
        return attribute;
    }
    /* (non-Javadoc)
     * @see org.epics.ioprivate DBType dbType;
    private DBDFieldAttribute attribute;c.dbDefinition.DBDArrayField#getElementDBType()
     */
    public DBType getElementDBType() {
        return elementDBType;
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
        builder.append("elementDBType " + elementDBType.toString());
        return builder.toString();
    }
}
