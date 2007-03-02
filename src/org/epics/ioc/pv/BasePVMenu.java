/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Abstract base class for PVMenu.
 * @author mrk
 *
 */
public class BasePVMenu extends BasePVEnum implements PVMenu
{ 
    private static Convert convert = ConvertFactory.getConvert();
    /**
     * Constructor for BasePVMenu
     * @param parent The parent.
     * @param menu The introspection interface.
     * @param choice The array of choices.
     */
    public BasePVMenu(PVField parent,Menu menu) {
        super(parent,menu);
        super.setChoices(menu.getMenuChoices());
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.BasePVEnum#setChoices(java.lang.String[])
     */
    public boolean setChoices(String[] choice) {
        throw new UnsupportedOperationException(
            "Menu choices can not be modified");
    }    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.BasePVEnum#toString(int)
     */
    public String toString(int indentLevel) {
        return getString(indentLevel);
    }

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        convert.newLine(builder,indentLevel);
        Menu menu = (Menu)super.getField();
        builder.append("menu(" + menu.getMenuName() + ")" + " {");
        convert.newLine(builder,indentLevel+1);
        builder.append(super.toString(indentLevel+1));
        convert.newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
