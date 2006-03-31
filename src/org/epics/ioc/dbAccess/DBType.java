/**
 * 
 */
package org.epics.ioc.dbAccess;

/**
 * Defines the types for Database Fields
 * @author mrk
 *
 */
public enum DBType {
    dbPvType, // Type pvBoolean, ..., pvEnum
    dbMenu,
    dbStructure,
    dbArray,
    dbLink;
}

