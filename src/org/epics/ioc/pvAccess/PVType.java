/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * Defines the types for Process Variable data
 * @author mrk
 *
 */
public enum PVType {
	pvUnknown,
    pvBoolean,
    pvByte, pvShort, pvInt, pvLong,
    pvFloat,pvDouble,
    pvString,
    pvStructure,
    pvArray;

    /**
     * @return Returns true if the type is numeric, i.e. byte, short, int, long, float, or double
     */
    boolean isNumeric() {
        if(ordinal() < 2) return false; // less than byte
        if(ordinal() > 7) return false; // greater than double
        return true;
    }
    /**
     * @return Returns true if the type is a Java primitive type, i.e. boolean, byte, short, int, long, float, or double
     */
    boolean isPrimitive() {
    	if(ordinal() < 1) return false; // less than boolean
        if(ordinal() > 7) return false; // greater than double
        return true;
    }
    /**
     * @return Returns true if the type is a Java primitive or a string
     */
    boolean isScalar() {
    	if(ordinal() < 1) return false; // less than boolean
        if(ordinal() > 8) return false; // greater than string
        return true;
    }
}

