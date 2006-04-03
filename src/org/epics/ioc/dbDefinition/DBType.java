/**
 * 
 */
package org.epics.ioc.dbDefinition;

/**
 * Defines the types for Database Fields
 * @author mrk
 *
 */
public enum DBType {
    /**
     * The field is a pv Type.
     * This it is one of pvUnknown, pvBoolean, ..., pvEnum
     */
    dbPvType,
    /**
     * The field is a menu.
     * The pv Type is pvEnum 
     */
    dbMenu,
    /**
     * 
     * The field is a structure.
     * The pvType is pvStructure. The fields can be any DBType
     */
    dbStructure,
    /**
     * 
     * The field is an array.
     * The pv Type is pvArray. The elemants can be any DBType
     */
    dbArray,
    /**
     * 
     * The field is a link
     * Ths pvType is pvStructure
     */
    dbLink;
}

