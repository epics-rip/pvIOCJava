/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Base class for implementing an Enum.
 * It is also a complete implementation.
 * @author mrk
 *
 */
public class EnumBase extends FieldBase implements Enum{
    private boolean choicesMutable;
    /**
     * Constructor for ArrayBase.
     * @param fieldName The field name.
     * @param choicesMutable Are the choices mutable?
     * @param property An array of properties for the field.
     * @param fieldAttribute The field attributes.
     */
    public EnumBase(String fieldName,boolean choicesMutable,Property[] property,FieldAttribute fieldAttribute) {
        super(fieldName,Type.pvEnum,property,fieldAttribute);
        this.choicesMutable = choicesMutable;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Enum#isChoicesMutable()
     */
    public boolean isChoicesMutable() {
        return choicesMutable;
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

    private String getString(int indentLevel) {
        StringBuilder builder = new StringBuilder();
        builder.append(super.toString(indentLevel));
        builder.append(String.format("choicesMutable %b ",
             choicesMutable));
        return builder.toString();
    }
}
