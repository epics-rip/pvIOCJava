/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Defines the types for Process Variable data
 * @author mrk
 *
 */
public enum Type {
    /**
     * type is unknown
     */
    pvUnknown,
    /**
     * value has type <i>boolean</i>
     */
    pvBoolean,
    /**
     * value has type <i>byte</i>
     */
    pvByte,
    /**
     * value has type <i>short</i>
     */
    pvShort,
    /**
     * value has type <i>int</i>
     */
    pvInt,
    /**
     * value has type <i>long</i>
     */
    pvLong,
    /**
     * value has type <i>float</i>
     */
    pvFloat,
    /**
     * value has type <i>double</i>
     */
    pvDouble,
    /**
     * value has type <i>String</i>
     */
    pvString,
    /**
     * value has a <i>String[]</i> of choices and an index that selects a choice.
     */
    pvEnum,
    /**
     * value provides access to structure
     */
    pvStructure,
    /**
     * value provides access to an array of values all of the same type.
     * arrays of any Type are allowed.
     */
    pvArray;

    /**
     * is this a Java numeric type
     * @return Returns true if the type is a Java numeric type.
     * The numeric types are byte, int, long, float, and double.
     */
    public boolean isNumeric() {
        if(ordinal() < 2) return false; // less than byte
        if(ordinal() > 7) return false; // greater than double
        return true;
    }
    /**
     * is this a Java primitive type
     * @return Returns true if the type is a Java primitive type.
     * This is the numeric types and boolean.
     */
    public boolean isPrimitive() {
        if(ordinal() < 1) return false; // less than boolean
        if(ordinal() > 7) return false; // greater than double
        return true;
    }
    /**
     * is this wither a Java primitive or a <i>string</i>
     * @return Returns true if the type is a Java primitive or a String
     */
    public boolean isScalar() {
        if(ordinal() < 1) return false; // less than boolean
        if(ordinal() > 8) return false; // greater than string
        return true;
    }
}

