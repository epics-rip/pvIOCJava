/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * Defines the Process Variable data types.
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
     * Value has a <i>String[]</i> of choices and an index
     * that selects a choice.
     */
    pvEnum,
    /**
     * Value provides access to structure.
     */
    pvStructure,
    /**
     * Value provides access to an array of values all of the same type.
     * arrays of any Type are allowed.
     */
    pvArray,
    /**
     * Value is a enum with immutable choices.
     * A menu also hace a menuName.
     */
    pvMenu,
    /**
     * No value.
     * It provides a field that has support but no data.
     */
    pvLink;

    /**
     * Is this a Java numeric type?
     * @return Returns true if the type is a Java numeric type.
     * The numeric types are byte, int, long, float, and double.
     */
    public boolean isNumeric() {
        if(ordinal() < Type.pvByte.ordinal()) return false;
        if(ordinal() > Type.pvDouble.ordinal()) return false;
        return true;
    }
    /**
     * Is this a Java primitive type?
     * @return Returns true if the type is a Java primitive type.
     * This is the numeric types and boolean.
     */
    public boolean isPrimitive() {
        if(ordinal() < Type.pvBoolean.ordinal()) return false;
        if(ordinal() > Type.pvDouble.ordinal()) return false;
        return true;
    }
    /**
     * Is this either a Java primitive or a <i>string</i>?
     * @return Returns true if the type is a Java primitive or a String
     */
    public boolean isScalar() {
        if(ordinal() < Type.pvBoolean.ordinal()) return false;
        if(ordinal() > Type.pvString.ordinal()) return false;
        return true;
    }
}

