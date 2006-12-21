/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.db;

import org.epics.ioc.pv.*;

/**
 * Abstract base class for DBMenu.
 * @author mrk
 *
 */
public abstract class AbstractDBMenu extends AbstractDBData implements PVMenu
{
    private int index;
    private String[]choice;
    private static Convert convert = ConvertFactory.getConvert();
    
    protected AbstractDBMenu(DBData parent,Menu menu,String[] choice) {
        super(parent,menu);
        index = 0;
        this.choice = choice;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#getChoices()
     */
    public String[] getChoices() {
        return choice;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#getIndex()
     */
    public int getIndex() {
        return index;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choice) {
        throw new UnsupportedOperationException(
            "Menu choices can not be modified");
    }    
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.PVEnum#setIndex(int)
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
     * @see org.epics.ioc.db.AbstractDBData#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        newLine(builder,indentLevel);
        Menu menu = (Menu)super.getField();
        builder.append("menu(" + menu.getMenuName() + ")" + " {");
        newLine(builder,indentLevel+1);
        builder.append(convert.getString(this,indentLevel+2));
        newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
