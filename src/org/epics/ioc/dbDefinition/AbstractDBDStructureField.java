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
public class AbstractDBDStructureField extends AbstractStructure implements DBDStructureField {
    private DBDFieldAttribute attribute;
    private DBDField[] dbdField;
    private DBDStructure dbdStructure;
    
    public AbstractDBDStructureField(String name,Property[] property,DBDFieldAttribute attribute,
        DBDField[] dbdField,DBDStructure dbdStructure)
    {
        super(name,property,dbdStructure.getStructureName(),(Field[])dbdField);
        this.attribute = attribute;
        this.dbdField = dbdField;
        this.dbdStructure = dbdStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getDBType()
     */
    public DBType getDBType() {
        return DBType.dbStructure;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDField#getFieldAttribute()
     */
    public DBDFieldAttribute getFieldAttribute() {
        return attribute;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDStructureField#getDBDFields()
     */
    public DBDField[] getDBDFields() {
        return dbdField;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.dbDefinition.DBDStructureField#getDBDStructure()
     */
    public DBDStructure getDBDStructure() {
        return dbdStructure;
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
        builder.append("DBType dbStructure ");
        builder.append(attribute.toString(indentLevel));
        return builder.toString();
    }
}
