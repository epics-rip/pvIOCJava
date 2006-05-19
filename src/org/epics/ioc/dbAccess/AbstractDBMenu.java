/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.dbDefinition.*;
import org.epics.ioc.pvAccess.*;

/**
 * Abstract base class for DBMenu.
 * @author mrk
 *
 */
public abstract class AbstractDBMenu extends AbstractDBData implements DBMenu
{

    /* (non-Javadoc)
     * @see org.epics.ioc.dbAccess.DBMenu#getMenuName()
     */
    public String getMenuName() {
        return menuName;
    }

    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#getChoices()
     */
    public String[] getChoices() {
        return choice;
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#getIndex()
     */
    public int getIndex() {
        return index;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choice) {
        throw new UnsupportedOperationException(
            "Menu choices can not be modified");
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVEnum#setIndex(int)
     */
    public void setIndex(int index) {
        if(super.getField().isMutable()) { this.index = index; return; }
        throw new IllegalStateException("PVData.isMutable is false");
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.PVData#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        newLine(builder,indentLevel);
        builder.append("menu(" + menuName + ")" + " {");
        newLine(builder,indentLevel+1);
        builder.append(convert.getString(this,indentLevel+2));
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
    
    /**
     * constructor that derived classes must call.
     * @param parent the parent interface.
     * @param dbdMenuField the reflection interface for the DBMenu data. 
     */
    protected AbstractDBMenu(DBStructure parent,DBDMenuField dbdMenuField) {
        super(parent,dbdMenuField);
        index = 0;
        DBDMenu dbdMenu = super.getDBDField().getAttribute().getMenu();
        this.choice = dbdMenu.getChoices();
        this.menuName = dbdMenu.getName();
    }
    
    private int index;
    private String[]choice;
    private String menuName;
    private static Convert convert = ConvertFactory.getConvert();
}
