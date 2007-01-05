/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Base class for implementing an Array.
 * It is also a complete implementation.
 * @author mrk
 *
 */
public class ArrayBase extends FieldBase implements Array {
    
    private Type elementType;
    
    /**
     * Constructor for ArrayBase.
     * @param fieldName The field name.
     * @param elementType The element Type.
     * @param property An array of Property.
     * @param fieldAttribute The field attributes.
     */
    public ArrayBase(String fieldName,Type elementType, Property[] property,FieldAttribute fieldAttribute) {
        super(fieldName, Type.pvArray,property,fieldAttribute);
        this.elementType = elementType;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.pv.Array#getElementType()
     */
    public Type getElementType() {
        return elementType;
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
        builder.append("elementType " + elementType.toString());
        return builder.toString();
    }

}
