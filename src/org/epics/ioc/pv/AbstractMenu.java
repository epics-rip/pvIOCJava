/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * @author mrk
 *
 */
public class AbstractMenu extends AbstractField implements Menu{
    private String menuName;
    
    /**
     * Constructor for a menu field.
     * @param name The field name.
     * @param property The field properties.
     * @param fieldAttribute The field attributes.
     * @param menuName The menu name.
     */
    public AbstractMenu(String name,Property[] property,FieldAttribute fieldAttribute,String menuName) {
        super(name,Type.pvMenu,property,fieldAttribute);
        this.menuName = menuName;
    }
    
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Enum#isChoicesMutable()
     */
    public boolean isChoicesMutable() {
        return false;
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

    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Menu#getMenuName()
     */
    public String getMenuName() {
        return menuName;
    }

    private String getString(int indentLevel) {
        return super.toString(indentLevel) + " choicesMutable false";
    }
}
