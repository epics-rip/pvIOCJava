/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Base class for implementing a Menu.
 * It is also a complete implementation.
 * @author mrk
 *
 */
public class BaseMenu extends BaseField implements Menu{
    private static Convert convert = ConvertFactory.getConvert();
    private String menuName;
    private String[] menuChoices;
    
    /**
     * Constructor for a menu field.
     * @param fieldName The field name.
     * @param menuName The menu name.
     * @param property The field properties.
     * @param fieldAttribute The field attributes.
     */
    public BaseMenu(String fieldName,String menuName,String[] menuChoices,
        Property[] property,FieldAttribute fieldAttribute)
    {
        super(fieldName,Type.pvMenu,property,fieldAttribute);
        this.menuName = menuName;
        this.menuChoices = menuChoices;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Enum#isChoicesMutable()
     */
    public boolean isChoicesMutable() {
        return false;
    }  
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() { return getString(0);}
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Field#toString(int)
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
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Menu#getMenuChoices()
     */
    public String[] getMenuChoices() {
        return menuChoices;
    }
    
    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString(indentLevel));
        convert.newLine(builder,indentLevel);
        builder.append("menu(" + menuName + ")" + " {");
        convert.newLine(builder,indentLevel+1);
        for(String choice: menuChoices) {
            builder.append(choice + " ");
        }
        convert.newLine(builder,indentLevel);
        builder.append("}");
        return builder.toString();
    }
}
