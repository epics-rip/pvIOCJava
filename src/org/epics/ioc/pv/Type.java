/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Process Variable Data Type.
 * @author mrk
 *
 */
public enum Type {
    /**
     * Value has type <i>boolean</i>.
     */
    pvBoolean,
    /**
     * Value has type <i>byte</i>.
     */
    pvByte,
    /**
     * Value has type <i>short</i>.
     */
    pvShort,
    /**
     * Value has type <i>int</i>.
     */
    pvInt,
    /**
     * Value has type <i>long</i>.
     */
    pvLong,
    /**
     * value has type <i>float</i>.
     */
    pvFloat,
    /**
     * Value has type <i>double</i>.
     */
    pvDouble,
    /**
     * Value has type <i>String</i>.
     */
    pvString,
    /**
     * Value provides access to structure.
     */
    pvStructure,
    /**
     * Value provides access to an array of values all of the same type.
     * arrays of any Type are allowed.
     */
    pvArray;
    /**
     * Is this a Java integer type?
     * @return Returns true if the type is a Java integer type.
     * The numeric types are byte, short, int, and  long.
     */
    public boolean isInteger() {
        if( (ordinal() >= Type.pvByte.ordinal()) && (ordinal() <= Type.pvLong.ordinal()) ) {
            return true;
        }
        return false;
    }
    /**
     * Is this a Java numeric type?
     * @return Returns true if the type is a Java numeric type.
     * The numeric types are byte, short, int, long, float, and double.
     */
    public boolean isNumeric() {
        if( (ordinal() >= Type.pvByte.ordinal()) && (ordinal() <= Type.pvDouble.ordinal()) ) {
            return true;
        }
        return false;
    }
    /**
     * Is this a Java primitive type?
     * @return Returns true if the type is a Java primitive type.
     * The numeric types and boolean are primitive types.
     */
    public boolean isPrimitive() {
        if(isNumeric()) return true;
        if(ordinal() == Type.pvBoolean.ordinal()) return true;
        return false;
    }
    /**
     * Is this either a Java primitive or a <i>string</i>?
     * @return Returns true if the type is a Java primitive or a String
     */
    public boolean isScalar() {
        if(isPrimitive()) return true;
        if(ordinal() == Type.pvString.ordinal()) return true;
        return false;
    }
}

