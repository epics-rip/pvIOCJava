/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * Implemention of Enum.
 * @author mrk
 *
 */
public class AbstractEnum extends AbstractField implements Enum{
    private boolean choicesMutable;
    /**
     * Constructor for AbstractArray.
     * @param name The field name.
     * @param property An array of properties for the field.
     * @param choicesMutable Are the choices mutable?
     */
    public AbstractEnum(String name,Property[] property,boolean choicesMutable) {
        super(name,Type.pvEnum,property);
        this.choicesMutable = choicesMutable;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Enum#isChoicesMutable()
     */
    public boolean isChoicesMutable() {
        return choicesMutable;
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
        builder.append(String.format("choicesMutable %b ",
             choicesMutable));
        return builder.toString();
    }
}
