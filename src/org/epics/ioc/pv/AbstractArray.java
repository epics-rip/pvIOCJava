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
public class AbstractArray extends AbstractField implements Array {
    
    private Type elementType;
    
    /**
     * Constructor for AbstractArray.
     * @param name The field name.
     * @param property An array of Property.
     * @param fieldAttribute The field attributes.
     * @param elementType The element Type.
     */
    public AbstractArray(String name, Property[] property,FieldAttribute fieldAttribute,Type elementType) {
        super(name, Type.pvArray,property,fieldAttribute);
        this.elementType = elementType;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pvAccess.Array#getElementType()
     */
    public Type getElementType() {
        return elementType;
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
        builder.append("elementType " + elementType.toString());
        return builder.toString();
    }

}
