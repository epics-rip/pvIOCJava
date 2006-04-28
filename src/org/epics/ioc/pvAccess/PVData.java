/**
 * 
 */
package org.epics.ioc.pvAccess;

/**
 * PVData is the base class for field data.
 * Each PVType has an interface that extends PVData.
 * @author mrk
 *
 */
public interface PVData {
    /**
     * get the <i>Field</i> that describes the field.
     * @return Field, which is the reflection interface.
     */
    Field getField();
    String toString();
    String toString(int indentLevel);
}
