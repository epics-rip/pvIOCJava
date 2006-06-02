/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * Defines the Process Variable data types.
 * @author mrk
 *
 */
public enum Type {
    /**
     * type is unknown.
     */
    pvUnknown,
    /**
     * value has type <i>boolean</i>.
     */
    pvBoolean,
    /**
     * value has type <i>byte</i>.
     */
    pvByte,
    /**
     * value has type <i>short</i>.
     */
    pvShort,
    /**
     * value has type <i>int</i>.
     */
    pvInt,
    /**
     * value has type <i>long</i>.
     */
    pvLong,
    /**
     * value has type <i>float</i>.
     */
    pvFloat,
    /**
     * value has type <i>double</i>.
     */
    pvDouble,
    /**
     * value has type <i>String</i>.
     */
    pvString,
    /**
     * value has a <i>String[]</i> of choices and an index
     * that selects a choice.
     */
    pvEnum,
    /**
     * value provides access to structure.
     */
    pvStructure,
    /**
     * value provides access to an array of values all of the same type.
     * arrays of any Type are allowed.
     */
    pvArray;

    /**
     * is this a Java numeric type?
     * @return Returns true if the type is a Java numeric type.
     * The numeric types are byte, int, long, float, and double.
     */
    public boolean isNumeric() {
        if(ordinal() < Type.pvByte.ordinal()) return false;
        if(ordinal() > Type.pvDouble.ordinal()) return false;
        return true;
    }
    /**
     * is this a Java primitive type?
     * @return Returns true if the type is a Java primitive type.
     * This is the numeric types and boolean.
     */
    public boolean isPrimitive() {
        if(ordinal() < Type.pvBoolean.ordinal()) return false;
        if(ordinal() > Type.pvDouble.ordinal()) return false;
        return true;
    }
    /**
     * is this either a Java primitive or a <i>string</i>?
     * @return Returns true if the type is a Java primitive or a String
     */
    public boolean isScalar() {
        if(ordinal() < Type.pvBoolean.ordinal()) return false;
        if(ordinal() > Type.pvString.ordinal()) return false;
        return true;
    }
}
