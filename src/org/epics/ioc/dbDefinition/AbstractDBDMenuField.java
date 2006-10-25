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
public class AbstractDBDMenuField extends AbstractEnum implements DBDMenuField {
    private DBDFieldAttribute attribute;
    private DBDMenu dbdMenu;
    
    public AbstractDBDMenuField(String name,Property[] property,
            DBDFieldAttribute attribute,DBDMenu dbdMenu)
    {
        super(name,property,false);
        this.attribute = attribute;
        this.dbdMenu = dbdMenu;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getDBType()
     */
    public DBType getDBType() {
        return DBType.dbMenu;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getFieldAttribute()
     */
    public DBDFieldAttribute getFieldAttribute() {
        return attribute;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDMenuField#getMenu()
     */
    public DBDMenu getMenu() {
        return dbdMenu;
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
        builder.append("DBType dbMenu ");
        builder.append(attribute.toString(indentLevel));
        return builder.toString();
    }
}
